package com.vamsi.screener.service.extraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vamsi.screener.config.AppProperties;
import com.vamsi.screener.model.AnalysisSource;
import com.vamsi.screener.model.EducationEntry;
import com.vamsi.screener.model.ExperienceEntry;
import com.vamsi.screener.service.llm.LlmClient;
import com.vamsi.screener.service.llm.LlmJsonParser;
import com.vamsi.screener.service.llm.PromptTemplateService;
import com.vamsi.screener.service.llm.ResumeExtractionPayload;
import com.vamsi.screener.util.TextUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Extracts structured resume data with the configured language model.
 *
 * <p>The model output is treated as untrusted input: every field is normalised, clamped and
 * canonicalised before it reaches the database.
 */
@Component
public class LlmResumeExtractor {

    private static final int MAX_SKILLS = 60;
    private static final int MAX_ENTRIES = 15;

    private final LlmClient llmClient;
    private final PromptTemplateService promptTemplates;
    private final ObjectMapper objectMapper;
    private final SkillDictionary skillDictionary;
    private final AppProperties.Llm config;

    public LlmResumeExtractor(LlmClient llmClient,
                              PromptTemplateService promptTemplates,
                              ObjectMapper objectMapper,
                              SkillDictionary skillDictionary,
                              AppProperties properties) {
        this.llmClient = llmClient;
        this.promptTemplates = promptTemplates;
        this.objectMapper = objectMapper;
        this.skillDictionary = skillDictionary;
        this.config = properties.getLlm();
    }

    /**
     * @param resumeText normalised resume text
     * @return structured data tagged with {@link AnalysisSource#LLM}
     * @throws com.vamsi.screener.exception.LlmException when the model is unavailable or its output
     *                                                   cannot be parsed
     */
    public ExtractedResumeData extract(String resumeText) {
        String systemPrompt = promptTemplates.load(PromptTemplateService.RESUME_EXTRACTION_SYSTEM);
        String userPrompt = promptTemplates.render(PromptTemplateService.RESUME_EXTRACTION_USER,
                Map.of("resumeText", TextUtils.truncate(resumeText, config.getMaxInputCharacters())));

        String rawResponse = llmClient.generateJson(systemPrompt, userPrompt);
        ResumeExtractionPayload payload =
                LlmJsonParser.parse(objectMapper, rawResponse, ResumeExtractionPayload.class);

        return new ExtractedResumeData(
                clean(payload.candidateName(), 200),
                clean(payload.email(), 200),
                clean(payload.phone(), 60),
                clean(payload.location(), 200),
                clean(payload.summary(), 2000),
                sanitizeYears(payload.yearsOfExperience()),
                clean(payload.highestEducation(), 200),
                canonicalSkills(payload.skills()),
                experiences(payload.experience()),
                educations(payload.education()),
                AnalysisSource.LLM);
    }

    private Set<String> canonicalSkills(List<String> rawSkills) {
        Set<String> skills = new LinkedHashSet<>();
        if (rawSkills == null) {
            return skills;
        }
        for (String raw : rawSkills) {
            String canonical = skillDictionary.canonicalize(raw);
            if (canonical != null && !canonical.isBlank()) {
                skills.add(canonical);
            }
            if (skills.size() >= MAX_SKILLS) {
                break;
            }
        }
        return skills;
    }

    private List<ExperienceEntry> experiences(List<ResumeExtractionPayload.ExperienceItem> items) {
        List<ExperienceEntry> entries = new ArrayList<>();
        if (items == null) {
            return entries;
        }
        for (ResumeExtractionPayload.ExperienceItem item : items) {
            if (item == null) {
                continue;
            }
            entries.add(new ExperienceEntry(
                    clean(item.company(), 300),
                    clean(item.title(), 300),
                    clean(item.period(), 120),
                    clean(item.description(), 2000)));
            if (entries.size() >= MAX_ENTRIES) {
                break;
            }
        }
        return entries;
    }

    private List<EducationEntry> educations(List<ResumeExtractionPayload.EducationItem> items) {
        List<EducationEntry> entries = new ArrayList<>();
        if (items == null) {
            return entries;
        }
        for (ResumeExtractionPayload.EducationItem item : items) {
            if (item == null) {
                continue;
            }
            entries.add(new EducationEntry(
                    clean(item.institution(), 300),
                    clean(item.degree(), 200),
                    clean(item.fieldOfStudy(), 200),
                    clean(item.year(), 60)));
            if (entries.size() >= MAX_ENTRIES) {
                break;
            }
        }
        return entries;
    }

    private Double sanitizeYears(Double years) {
        if (years == null || years.isNaN() || years < 0.0D || years > 60.0D) {
            return null;
        }
        return Math.round(years * 10.0D) / 10.0D;
    }

    private String clean(String value, int maxLength) {
        String trimmed = TextUtils.trimToNull(value);
        if (trimmed == null || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}
