package com.vamsi.screener.dto;

import com.vamsi.screener.model.AnalysisSource;
import com.vamsi.screener.model.Recommendation;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** One ranked candidate inside a screening run. */
public record CandidateScoreResponse(
        int rank,
        UUID resumeId,
        String candidateName,
        String fileName,
        String email,
        Double yearsOfExperience,
        double matchScore,
        double skillScore,
        double experienceScore,
        double educationScore,
        Recommendation recommendation,
        boolean shortlisted,
        Set<String> matchedSkills,
        Set<String> missingSkills,
        String justification,
        AnalysisSource analysisSource,
        String model,
        long latencyMs,
        Instant evaluatedAt) {
}
