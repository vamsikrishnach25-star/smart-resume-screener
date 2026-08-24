package com.vamsi.screener.service.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vamsi.screener.config.AppProperties;
import com.vamsi.screener.exception.LlmException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Calls Google's Generative Language API (Gemini).
 *
 * <p>Requests ask for {@code application/json} output and use a low temperature so the same resume
 * and job description produce stable scores. Transient failures (network errors, 429, 5xx) are
 * retried with exponential backoff; anything else fails fast so the caller can fall back to the
 * deterministic scorer.
 */
@Component
public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final long BASE_BACKOFF_MILLIS = 800L;

    private final AppProperties.Llm config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiLlmClient(AppProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getLlm();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public boolean isAvailable() {
        return config.isConfigured();
    }

    @Override
    public String provider() {
        return config.getProvider();
    }

    @Override
    public String model() {
        return config.getModel();
    }

    @Override
    public String generateJson(String systemInstruction, String userPrompt) {
        if (!isAvailable()) {
            throw new LlmException("No LLM API key is configured; set GEMINI_API_KEY to enable model scoring.");
        }

        HttpRequest request = HttpRequest.newBuilder(endpoint())
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", config.getApiKey())
                .timeout(config.getTimeout())
                .POST(HttpRequest.BodyPublishers.ofString(
                        buildRequestBody(systemInstruction, userPrompt), StandardCharsets.UTF_8))
                .build();

        int attempts = Math.max(1, config.getMaxRetries() + 1);
        LlmException lastFailure = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return readCandidateText(response.body());
                }
                LlmException failure = new LlmException(
                        "Gemini returned HTTP " + status + ": " + abbreviate(response.body()));
                if (!isRetryable(status) || attempt == attempts) {
                    throw failure;
                }
                lastFailure = failure;
            } catch (IOException e) {
                lastFailure = new LlmException("Gemini request failed: " + e.getMessage(), e);
                if (attempt == attempts) {
                    throw lastFailure;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LlmException("Interrupted while calling Gemini.", e);
            }

            log.warn("Gemini call attempt {}/{} failed ({}); retrying", attempt, attempts,
                    lastFailure.getMessage());
            sleepBeforeRetry(attempt);
        }

        throw lastFailure == null ? new LlmException("Gemini call failed.") : lastFailure;
    }

    private URI endpoint() {
        String base = config.getBaseUrl().endsWith("/")
                ? config.getBaseUrl().substring(0, config.getBaseUrl().length() - 1)
                : config.getBaseUrl();
        return URI.create(base + "/models/" + config.getModel() + ":generateContent");
    }

    private String buildRequestBody(String systemInstruction, String userPrompt) {
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode system = root.putObject("systemInstruction");
        system.putArray("parts").addObject().put("text", systemInstruction);

        ObjectNode userContent = root.putArray("contents").addObject();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject().put("text", userPrompt);

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("temperature", config.getTemperature());
        generationConfig.put("maxOutputTokens", config.getMaxOutputTokens());
        generationConfig.put("responseMimeType", "application/json");

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new LlmException("Could not build the Gemini request body.", e);
        }
    }

    private String readCandidateText(String responseBody) {
        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (JsonProcessingException e) {
            throw new LlmException("Gemini returned a response that is not valid JSON.", e);
        }

        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            String blockReason = root.path("promptFeedback").path("blockReason").asText("");
            throw new LlmException("Gemini returned no candidates"
                    + (blockReason.isEmpty() ? "." : " (blocked: " + blockReason + ")."));
        }

        JsonNode candidate = candidates.get(0);
        StringBuilder text = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            text.append(part.path("text").asText(""));
        }
        String result = text.toString().trim();
        if (result.isEmpty()) {
            String finishReason = candidate.path("finishReason").asText("");
            String hint = "MAX_TOKENS".equals(finishReason)
                    ? " The answer was cut short; raise app.llm.max-output-tokens."
                    : "";
            throw new LlmException("Gemini returned an empty completion"
                    + (finishReason.isEmpty() ? "." : " (finishReason: " + finishReason + ").") + hint);
        }
        return result;
    }

    private static boolean isRetryable(int status) {
        return status == 429 || status == 408 || status >= 500;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(BASE_BACKOFF_MILLIS * (1L << (attempt - 1)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Interrupted while waiting to retry a Gemini call.", e);
        }
    }

    private static String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        String single = body.replaceAll("\\s+", " ").trim();
        return single.length() <= 300 ? single : single.substring(0, 300) + "...";
    }
}
