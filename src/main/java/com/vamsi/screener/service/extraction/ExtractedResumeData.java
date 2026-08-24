package com.vamsi.screener.service.extraction;

import com.vamsi.screener.model.AnalysisSource;
import com.vamsi.screener.model.EducationEntry;
import com.vamsi.screener.model.ExperienceEntry;
import java.util.List;
import java.util.Set;

/**
 * Structured candidate data produced by an extractor, before it is persisted.
 *
 * @param source which engine produced this data, so the API can report it
 */
public record ExtractedResumeData(
        String candidateName,
        String email,
        String phone,
        String location,
        String summary,
        Double yearsOfExperience,
        String highestEducation,
        Set<String> skills,
        List<ExperienceEntry> experiences,
        List<EducationEntry> educations,
        AnalysisSource source) {
}
