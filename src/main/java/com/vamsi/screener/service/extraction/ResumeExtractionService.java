package com.vamsi.screener.service.extraction;

import com.vamsi.screener.exception.LlmException;
import com.vamsi.screener.model.AnalysisSource;
import com.vamsi.screener.service.llm.LlmClient;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Chooses how a resume gets parsed.
 *
 * <p>The language model is preferred because it understands layout and phrasing that rules cannot.
 * When it is not configured, or a call fails, the deterministic extractor takes over so an upload
 * never fails just because an API is down. Whichever engine ran is recorded on the result.
 */
@Service
public class ResumeExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ResumeExtractionService.class);

    private final LlmClient llmClient;
    private final LlmResumeExtractor llmExtractor;
    private final HeuristicResumeExtractor heuristicExtractor;
    private final SkillDictionary skillDictionary;

    public ResumeExtractionService(LlmClient llmClient,
                                   LlmResumeExtractor llmExtractor,
                                   HeuristicResumeExtractor heuristicExtractor,
                                   SkillDictionary skillDictionary) {
        this.llmClient = llmClient;
        this.llmExtractor = llmExtractor;
        this.heuristicExtractor = heuristicExtractor;
        this.skillDictionary = skillDictionary;
    }

    /**
     * Extracts structured data from resume text, preferring the LLM and falling back to rules.
     *
     * @param resumeText normalised resume text
     * @return extracted data; never null
     */
    public ExtractedResumeData extract(String resumeText) {
        if (llmClient.isAvailable()) {
            try {
                return enrich(llmExtractor.extract(resumeText), resumeText);
            } catch (LlmException e) {
                log.warn("LLM extraction failed, using the deterministic extractor instead: {}",
                        e.getMessage());
            }
        }
        return heuristicExtractor.extract(resumeText);
    }

    /** Backfills skills from the dictionary when the model returned none. */
    private ExtractedResumeData enrich(ExtractedResumeData data, String resumeText) {
        if (data.skills() != null && !data.skills().isEmpty()) {
            return data;
        }
        Set<String> dictionarySkills = new LinkedHashSet<>(skillDictionary.extractFrom(resumeText));
        return new ExtractedResumeData(
                data.candidateName(),
                data.email(),
                data.phone(),
                data.location(),
                data.summary(),
                data.yearsOfExperience(),
                data.highestEducation(),
                dictionarySkills,
                data.experiences(),
                data.educations(),
                AnalysisSource.LLM);
    }
}
