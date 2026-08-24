package com.vamsi.screener.service.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vamsi.screener.config.AppProperties;
import com.vamsi.screener.exception.LlmException;
import com.vamsi.screener.model.AnalysisSource;
import com.vamsi.screener.model.JobDescription;
import com.vamsi.screener.model.Resume;
import com.vamsi.screener.service.extraction.SkillDictionary;
import com.vamsi.screener.service.llm.LlmClient;
import com.vamsi.screener.service.llm.LlmJsonParser;
import com.vamsi.screener.service.llm.MatchAssessmentPayload;
import com.vamsi.screener.service.llm.PromptTemplateService;
import com.vamsi.screener.util.TextUtils;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Scores a candidate against a role with the configured language model.
 *
 * <p>The model is asked for a strict JSON verdict following the rubric in
 * {@code resources/prompts/job-match.system.txt}. Anything it returns is validated: scores are
 * clamped, skills are canonicalised, and an unusable response raises {@link LlmException} so the
 * caller can fall back to the deterministic scorer.
 */
@Component
public class LlmMatchScorer {

    private static final int MAX_SKILL_ENTRIES = 40;

    private final LlmClient llmClient;
    private final PromptTemplateService promptTemplates;
    private final ObjectMapper objectMapper;
    private final JobSkillResolver jobSkillResolver;
    private final SkillDictionary skillDictionary;
    private final AppProperties.Llm config;

    public LlmMatchScorer(LlmClient llmClient,
                          PromptTemplateService promptTemplates,
                          ObjectMapper objectMapper,
                          JobSkillResolver jobSkillResolver,
                          SkillDictionary skillDictionary,
                          AppProperties properties) {
        this.llmClient = llmClient;
        this.promptTemplates = promptTemplates;
        this.objectMapper = objectMapper;
        this.jobSkillResolver = jobSkillResolver;
        this.skillDictionary = skillDictionary;
        this.config = properties.getLlm();
    }

    /**
     * @param resume parsed candidate
     * @param job    role to screen against
     * @return an assessment tagged with {@link AnalysisSource#LLM}
     * @throws LlmException when the model is unavailable or returns something unusable
     */
    public MatchAssessment score(Resume resume, JobDescription job) {
        Set<String> requiredSkills = jobSkillResolver.requiredSkills(job);

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("jobTitle", nullSafe(job.getTitle()));
        variables.put("jobCompany", nullSafe(job.getCompany()));
        variables.put("jobLocation", nullSafe(job.getLocation()));
        variables.put("minYearsExperience", job.getMinYearsExperience() == null
                ? "not specified"
                : String.format(Locale.ROOT, "%.1f", job.getMinYearsExperience()));
        variables.put("requiredSkills", requiredSkills.isEmpty()
                ? "not specified; infer them from the description"
                : String.join(", ", requiredSkills));
        variables.put("jobDescription", TextUtils.truncate(job.getDescription(), config.getMaxInputCharacters()));
        variables.put("candidateName", nullSafe(resume.getCandidateName()));
        variables.put("candidateYears", resume.getYearsOfExperience() == null
                ? "unknown"
                : String.format(Locale.ROOT, "%.1f", resume.getYearsOfExperience()));
        variables.put("candidateEducation", nullSafe(resume.getHighestEducation()));
        variables.put("candidateSkills", resume.getSkills().isEmpty()
                ? "none extracted"
                : String.join(", ", resume.getSkills()));
        variables.put("resumeText", TextUtils.truncate(resume.getRawText(), config.getMaxInputCharacters()));

        String systemPrompt = promptTemplates.load(PromptTemplateService.JOB_MATCH_SYSTEM);
        String userPrompt = promptTemplates.render(PromptTemplateService.JOB_MATCH_USER, variables);

        String rawResponse = llmClient.generateJson(systemPrompt, userPrompt);
        MatchAssessmentPayload payload =
                LlmJsonParser.parse(objectMapper, rawResponse, MatchAssessmentPayload.class);

        if (payload.matchScore() == null || payload.matchScore().isNaN()) {
            throw new LlmException("The model response did not include a usable matchScore.");
        }

        double overall = payload.matchScore();
        return MatchAssessment.of(
                overall,
                orDefault(payload.skillScore(), overall),
                orDefault(payload.experienceScore(), overall),
                orDefault(payload.educationScore(), overall),
                canonicalize(payload.matchedSkills()),
                canonicalize(payload.missingSkills()),
                justificationOrFallback(payload.justification()),
                AnalysisSource.LLM);
    }

    private double orDefault(Double value, double fallback) {
        return value == null || value.isNaN() ? fallback : value;
    }

    private String justificationOrFallback(String justification) {
        String trimmed = TextUtils.trimToNull(justification);
        if (trimmed == null) {
            return "The model returned a score without a written justification.";
        }
        return TextUtils.truncate(trimmed, 3500);
    }

    private Set<String> canonicalize(List<String> rawSkills) {
        Set<String> skills = new LinkedHashSet<>();
        if (rawSkills == null) {
            return skills;
        }
        for (String raw : rawSkills) {
            String canonical = skillDictionary.canonicalize(raw);
            if (canonical != null && !canonical.isBlank()) {
                skills.add(canonical);
            }
            if (skills.size() >= MAX_SKILL_ENTRIES) {
                break;
            }
        }
        return skills;
    }

    private String nullSafe(String value) {
        String trimmed = TextUtils.trimToNull(value);
        return trimmed == null ? "not specified" : trimmed;
    }
}
