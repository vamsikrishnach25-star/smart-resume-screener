package com.vamsi.screener.service.llm;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Loads prompt templates from {@code src/main/resources/prompts} and fills in {@code {{variables}}}.
 *
 * <p>Keeping prompts in files rather than string literals means they are reviewable, diffable and
 * easy to tune without recompiling the service.
 */
@Service
public class PromptTemplateService {

    public static final String RESUME_EXTRACTION_SYSTEM = "resume-extraction.system.txt";
    public static final String RESUME_EXTRACTION_USER = "resume-extraction.user.txt";
    public static final String JOB_MATCH_SYSTEM = "job-match.system.txt";
    public static final String JOB_MATCH_USER = "job-match.user.txt";

    private static final String TEMPLATE_DIRECTORY = "prompts/";

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /** Loads a template from the classpath, caching it after the first read. */
    public String load(String templateName) {
        return cache.computeIfAbsent(templateName, name -> {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_DIRECTORY + name);
            try {
                return resource.getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Prompt template not found: " + name, e);
            }
        });
    }

    /**
     * Renders a template by replacing every {@code {{key}}} placeholder with its value.
     *
     * @param templateName file name inside {@code resources/prompts}
     * @param variables    placeholder values; null values are rendered as an empty string
     * @return the rendered prompt
     */
    public String render(String templateName, Map<String, String> variables) {
        String rendered = load(templateName);
        if (variables != null) {
            for (Map.Entry<String, String> variable : variables.entrySet()) {
                String value = variable.getValue() == null ? "" : variable.getValue();
                rendered = rendered.replace("{{" + variable.getKey() + "}}", value);
            }
        }
        return rendered;
    }
}
