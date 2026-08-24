package com.vamsi.screener.service.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** JSON contract the model must follow when rating a candidate against a job description. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchAssessmentPayload(
        Double matchScore,
        Double skillScore,
        Double experienceScore,
        Double educationScore,
        List<String> matchedSkills,
        List<String> missingSkills,
        String justification) {
}
