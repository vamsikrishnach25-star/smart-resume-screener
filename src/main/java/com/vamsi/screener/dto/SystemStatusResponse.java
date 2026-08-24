package com.vamsi.screener.dto;

/** Runtime status, used by the dashboard to show which scoring engine is active. */
public record SystemStatusResponse(
        String application,
        String version,
        boolean llmEnabled,
        boolean llmConfigured,
        String provider,
        String model,
        String activeEngine,
        long storedResumes,
        long storedJobs) {
}
