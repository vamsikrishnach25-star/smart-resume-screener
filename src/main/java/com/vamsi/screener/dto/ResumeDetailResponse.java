package com.vamsi.screener.dto;

import com.vamsi.screener.model.AnalysisSource;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Full structured view of one parsed resume. */
public record ResumeDetailResponse(
        UUID id,
        String fileName,
        String contentType,
        long fileSizeBytes,
        String candidateName,
        String email,
        String phone,
        String location,
        String summary,
        Double yearsOfExperience,
        String highestEducation,
        Set<String> skills,
        List<ExperienceDto> experiences,
        List<EducationDto> educations,
        AnalysisSource extractionSource,
        String rawTextPreview,
        Instant createdAt) {
}
