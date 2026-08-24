package com.vamsi.screener.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

/**
 * Screening instruction.
 *
 * @param jobDescriptionId job to screen against
 * @param resumeIds        resumes to evaluate; when null or empty every stored resume is used
 * @param minScore         drop candidates scoring below this value (1-10)
 * @param topN             keep only the best N candidates after ranking
 * @param refresh          recompute even when a stored result already exists
 */
public record ScreeningRequest(
        @NotNull(message = "jobDescriptionId is required")
        UUID jobDescriptionId,

        List<UUID> resumeIds,

        @DecimalMin(value = "0.0", message = "minScore must be >= 0")
        @DecimalMax(value = "10.0", message = "minScore must be <= 10")
        Double minScore,

        @Positive(message = "topN must be positive")
        Integer topN,

        Boolean refresh) {

    /** @return true when cached results should be ignored. */
    public boolean refreshRequested() {
        return Boolean.TRUE.equals(refresh);
    }
}
