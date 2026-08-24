package com.vamsi.screener.service.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vamsi.screener.exception.LlmException;

/**
 * Reads JSON out of a model response.
 *
 * <p>Models sometimes wrap JSON in markdown fences or add a sentence of commentary. This parser
 * scans for the first balanced {@code { ... }} block (quote- and escape-aware) instead of trusting
 * the response to be pure JSON.
 */
public final class LlmJsonParser {

    private LlmJsonParser() {
        // utility class
    }

    /** Extracts the first balanced JSON object from a raw model response. */
    public static String extractJsonObject(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new LlmException("The model returned an empty response.");
        }
        String text = raw.trim();
        int start = text.indexOf('{');
        if (start < 0) {
            throw new LlmException("The model response did not contain a JSON object.");
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char current = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        throw new LlmException("The model response contained an unterminated JSON object.");
    }

    /** Extracts and deserialises the first JSON object in a model response. */
    public static <T> T parse(ObjectMapper objectMapper, String raw, Class<T> type) {
        String json = extractJsonObject(raw);
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new LlmException("Could not parse the model's JSON response: " + e.getOriginalMessage(), e);
        }
    }
}
