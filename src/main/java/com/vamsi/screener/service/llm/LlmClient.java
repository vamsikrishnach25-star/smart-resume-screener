package com.vamsi.screener.service.llm;

import com.vamsi.screener.exception.LlmException;

/**
 * Minimal abstraction over a chat-completion provider.
 *
 * <p>Keeping this an interface means the scoring pipeline is testable without network access, and a
 * different provider can be dropped in without touching the services.
 */
public interface LlmClient {

    /** @return true when the client has everything it needs to make a call. */
    boolean isAvailable();

    /** @return provider id, e.g. {@code gemini}. */
    String provider();

    /** @return the model name used for requests. */
    String model();

    /**
     * Sends a prompt and returns the raw model output, which is expected to be JSON.
     *
     * @param systemInstruction role and output-contract instructions
     * @param userPrompt        the task-specific content
     * @return raw model response text
     * @throws LlmException when the provider is unavailable or the response is unusable
     */
    String generateJson(String systemInstruction, String userPrompt);
}
