package com.vamsi.screener.dto;

import com.vamsi.screener.model.EducationEntry;
import com.vamsi.screener.model.ExperienceEntry;
import com.vamsi.screener.model.JobDescription;
import com.vamsi.screener.model.Resume;
import com.vamsi.screener.model.ScreeningResult;
import com.vamsi.screener.util.TextUtils;
import java.util.List;

/** Converts entities into the records returned by the API. */
public final class DtoMapper {

    private static final int RAW_TEXT_PREVIEW_LENGTH = 1200;

    private DtoMapper() {
        // utility class
    }

    public static ResumeSummaryResponse toSummary(Resume resume) {
        return new ResumeSummaryResponse(
                resume.getId(),
                resume.getFileName(),
                resume.getCandidateName(),
                resume.getEmail(),
                resume.getPhone(),
                resume.getYearsOfExperience(),
                resume.getHighestEducation(),
                resume.getSkills(),
                resume.getExtractionSource(),
                resume.getCreatedAt());
    }

    public static ResumeDetailResponse toDetail(Resume resume) {
        List<ExperienceDto> experiences = resume.getExperiences().stream()
                .map(DtoMapper::toDto)
                .toList();
        List<EducationDto> educations = resume.getEducations().stream()
                .map(DtoMapper::toDto)
                .toList();
        return new ResumeDetailResponse(
                resume.getId(),
                resume.getFileName(),
                resume.getContentType(),
                resume.getFileSizeBytes(),
                resume.getCandidateName(),
                resume.getEmail(),
                resume.getPhone(),
                resume.getLocation(),
                resume.getSummary(),
                resume.getYearsOfExperience(),
                resume.getHighestEducation(),
                resume.getSkills(),
                experiences,
                educations,
                resume.getExtractionSource(),
                TextUtils.truncate(resume.getRawText(), RAW_TEXT_PREVIEW_LENGTH),
                resume.getCreatedAt());
    }

    public static ExperienceDto toDto(ExperienceEntry entry) {
        return new ExperienceDto(entry.getCompany(), entry.getTitle(), entry.getPeriod(), entry.getDescription());
    }

    public static EducationDto toDto(EducationEntry entry) {
        return new EducationDto(entry.getInstitution(), entry.getDegree(), entry.getFieldOfStudy(), entry.getYear());
    }

    public static JobDescriptionResponse toResponse(JobDescription job) {
        return new JobDescriptionResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getMinYearsExperience(),
                job.getDescription(),
                job.getRequiredSkills(),
                job.getCreatedAt());
    }

    public static CandidateScoreResponse toCandidateScore(ScreeningResult result, int rank) {
        Resume resume = result.getResume();
        return new CandidateScoreResponse(
                rank,
                resume.getId(),
                resume.displayName(),
                resume.getFileName(),
                resume.getEmail(),
                resume.getYearsOfExperience(),
                result.getMatchScore(),
                result.getSkillScore(),
                result.getExperienceScore(),
                result.getEducationScore(),
                result.getRecommendation(),
                result.getRecommendation() != null && result.getRecommendation().isShortlisted(),
                result.getMatchedSkills(),
                result.getMissingSkills(),
                result.getJustification(),
                result.getAnalysisSource(),
                result.getModel(),
                result.getLatencyMs(),
                result.getEvaluatedAt());
    }
}
