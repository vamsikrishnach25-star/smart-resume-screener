package com.vamsi.screener.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration for the application, bound from the {@code app.*} keys in
 * {@code application.yml} (and overridable through environment variables).
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Llm llm = new Llm();
    private final Upload upload = new Upload();
    private final Scoring scoring = new Scoring();

    public Llm getLlm() {
        return llm;
    }

    public Upload getUpload() {
        return upload;
    }

    public Scoring getScoring() {
        return scoring;
    }

    /** Large language model settings. */
    public static class Llm {

        /** Master switch. When false the service always uses the deterministic scorer. */
        private boolean enabled = true;

        /** Provider id. Only {@code gemini} ships out of the box. */
        private String provider = "gemini";

        /** API key. Injected from the GEMINI_API_KEY environment variable by default. */
        private String apiKey = "";

        /** Model name used for extraction and matching. */
        private String model = "gemini-3.6-flash";

        /** Base URL of the Google Generative Language API. */
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

        /** Per-request timeout. */
        private Duration timeout = Duration.ofSeconds(45);

        /** Number of extra attempts after a transient failure. */
        private int maxRetries = 2;

        /** Sampling temperature. Kept low so scores are reproducible. */
        private double temperature = 0.2D;

        /** Upper bound on generated tokens. */
        private int maxOutputTokens = 8192;

        /** Maximum number of resume characters sent to the model. */
        private int maxInputCharacters = 18000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxOutputTokens() {
            return maxOutputTokens;
        }

        public void setMaxOutputTokens(int maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }

        public int getMaxInputCharacters() {
            return maxInputCharacters;
        }

        public void setMaxInputCharacters(int maxInputCharacters) {
            this.maxInputCharacters = maxInputCharacters;
        }

        /** @return true when the model can actually be called. */
        public boolean isConfigured() {
            return enabled && apiKey != null && !apiKey.isBlank();
        }
    }

    /** Upload guard rails. */
    public static class Upload {

        private long maxFileSizeBytes = 5L * 1024L * 1024L;
        private int maxFilesPerRequest = 25;
        private List<String> allowedExtensions = List.of("pdf", "txt", "md");

        public long getMaxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public void setMaxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
        }

        public int getMaxFilesPerRequest() {
            return maxFilesPerRequest;
        }

        public void setMaxFilesPerRequest(int maxFilesPerRequest) {
            this.maxFilesPerRequest = maxFilesPerRequest;
        }

        public List<String> getAllowedExtensions() {
            return allowedExtensions;
        }

        public void setAllowedExtensions(List<String> allowedExtensions) {
            this.allowedExtensions = allowedExtensions;
        }
    }

    /** Weights used by the deterministic fallback scorer. */
    public static class Scoring {

        private double skillWeight = 0.60D;
        private double experienceWeight = 0.25D;
        private double educationWeight = 0.15D;

        /** Minimum score (1-10) for a candidate to be recommended for shortlisting. */
        private double shortlistThreshold = 7.0D;

        public double getSkillWeight() {
            return skillWeight;
        }

        public void setSkillWeight(double skillWeight) {
            this.skillWeight = skillWeight;
        }

        public double getExperienceWeight() {
            return experienceWeight;
        }

        public void setExperienceWeight(double experienceWeight) {
            this.experienceWeight = experienceWeight;
        }

        public double getEducationWeight() {
            return educationWeight;
        }

        public void setEducationWeight(double educationWeight) {
            this.educationWeight = educationWeight;
        }

        public double getShortlistThreshold() {
            return shortlistThreshold;
        }

        public void setShortlistThreshold(double shortlistThreshold) {
            this.shortlistThreshold = shortlistThreshold;
        }
    }
}
