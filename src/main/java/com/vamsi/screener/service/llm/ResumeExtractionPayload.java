package com.vamsi.screener.service.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * JSON contract the model must follow when extracting structured data from a resume.
 *
 * <p>Every field is nullable on purpose: a missing value is normal (not every resume lists a
 * location) and must never break parsing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResumeExtractionPayload(
        String candidateName,
        String email,
        String phone,
        String location,
        String summary,
        Double yearsOfExperience,
        String highestEducation,
        List<String> skills,
        List<ExperienceItem> experience,
        List<EducationItem> education) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExperienceItem(String company, String title, String period, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EducationItem(String institution, String degree, String fieldOfStudy, String year) {
    }
}
