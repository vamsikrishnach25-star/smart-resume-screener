package com.vamsi.screener.dto;

import com.vamsi.screener.model.AnalysisSource;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Compact resume view used by list endpoints and the dashboard. */
public record ResumeSummaryResponse(
        UUID id,
        String fileName,
        String candidateName,
        String email,
        String phone,
        Double yearsOfExperience,
        String highestEducation,
        Set<String> skills,
        AnalysisSource extractionSource,
        Instant createdAt) {
}
