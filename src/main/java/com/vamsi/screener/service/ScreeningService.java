package com.vamsi.screener.service;

import com.vamsi.screener.dto.CandidateScoreResponse;
import com.vamsi.screener.dto.DtoMapper;
import com.vamsi.screener.dto.ScreeningRequest;
import com.vamsi.screener.dto.ScreeningResponse;
import com.vamsi.screener.exception.LlmException;
import com.vamsi.screener.model.JobDescription;
import com.vamsi.screener.model.Resume;
import com.vamsi.screener.model.ScreeningResult;
import com.vamsi.screener.repository.ResumeRepository;
import com.vamsi.screener.repository.ScreeningResultRepository;
import com.vamsi.screener.service.llm.LlmClient;
import com.vamsi.screener.service.scoring.HeuristicMatchScorer;
import com.vamsi.screener.service.scoring.LlmMatchScorer;
import com.vamsi.screener.service.scoring.MatchAssessment;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs a screening: score every requested candidate against a role, store the verdicts, and return
 * them ranked.
 *
 * <p>Scoring prefers the language model and falls back to the deterministic scorer per candidate, so
 * one failed model call never sinks the whole run. Existing verdicts are reused unless the caller
 * asks for a refresh, which keeps repeat runs fast and cheap.
 */
@Service
public class ScreeningService {

    private static final Logger log = LoggerFactory.getLogger(ScreeningService.class);

    private final ResumeRepository resumeRepository;
    private final ScreeningResultRepository screeningResultRepository;
    private final ResumeService resumeService;
    private final JobDescriptionService jobDescriptionService;
    private final LlmClient llmClient;
    private final LlmMatchScorer llmMatchScorer;
    private final HeuristicMatchScorer heuristicMatchScorer;

    public ScreeningService(ResumeRepository resumeRepository,
                            ScreeningResultRepository screeningResultRepository,
                            ResumeService resumeService,
                            JobDescriptionService jobDescriptionService,
                            LlmClient llmClient,
                            LlmMatchScorer llmMatchScorer,
                            HeuristicMatchScorer heuristicMatchScorer) {
        this.resumeRepository = resumeRepository;
        this.screeningResultRepository = screeningResultRepository;
        this.resumeService = resumeService;
        this.jobDescriptionService = jobDescriptionService;
        this.llmClient = llmClient;
        this.llmMatchScorer = llmMatchScorer;
        this.heuristicMatchScorer = heuristicMatchScorer;
    }

    /**
     * Screens candidates against a role and returns them ranked best-first.
     *
     * @param request job, optional candidate subset, and ranking filters
     * @return ranked candidates with justifications
     */
    public ScreeningResponse screen(ScreeningRequest request) {
        JobDescription job = jobDescriptionService.requireJob(request.jobDescriptionId());
        List<Resume> candidates = resolveCandidates(request.resumeIds());
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    "There are no stored resumes to screen. Upload resumes before running a screening.");
        }

        List<ScreeningResult> results = new ArrayList<>();
        for (Resume candidate : candidates) {
            results.add(evaluate(candidate, job, request.refreshRequested()));
        }

        log.info("Screened {} candidates against '{}'", results.size(), job.getTitle());
        return buildResponse(job, results, request.minScore(), request.topN(), results.size());
    }

    /** Returns previously stored results for a role, ranked best-first. */
    @Transactional(readOnly = true)
    public ScreeningResponse resultsForJob(UUID jobId) {
        JobDescription job = jobDescriptionService.requireJob(jobId);
        List<ScreeningResult> stored = screeningResultRepository.findByJobDescriptionIdOrderByMatchScoreDesc(jobId);
        return buildResponse(job, stored, null, null, stored.size());
    }

    /** @return a short label describing which engine will score the next run. */
    public String activeEngine() {
        return llmClient.isAvailable()
                ? llmClient.provider() + ":" + llmClient.model()
                : "deterministic";
    }

    private ScreeningResult evaluate(Resume resume, JobDescription job, boolean refresh) {
        Optional<ScreeningResult> cached =
                screeningResultRepository.findByResumeIdAndJobDescriptionId(resume.getId(), job.getId());
        if (cached.isPresent() && !refresh) {
            return cached.get();
        }

        long startedAt = System.nanoTime();
        MatchAssessment assessment = assess(resume, job);
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000L;

        ScreeningResult result = cached.orElseGet(ScreeningResult::new);
        result.setResume(resume);
        result.setJobDescription(job);
        result.setMatchScore(assessment.matchScore());
        result.setSkillScore(assessment.skillScore());
        result.setExperienceScore(assessment.experienceScore());
        result.setEducationScore(assessment.educationScore());
        result.setRecommendation(assessment.recommendation());
        result.setJustification(assessment.justification());
        result.setMatchedSkills(assessment.matchedSkills());
        result.setMissingSkills(assessment.missingSkills());
        result.setAnalysisSource(assessment.source());
        result.setModel(llmClient.isAvailable() ? llmClient.model() : "rule-engine");
        result.setLatencyMs(latencyMs);
        result.setEvaluatedAt(Instant.now());

        return screeningResultRepository.save(result);
    }

    private MatchAssessment assess(Resume resume, JobDescription job) {
        if (llmClient.isAvailable()) {
            try {
                return llmMatchScorer.score(resume, job);
            } catch (LlmException e) {
                log.warn("LLM scoring failed for resume {}, falling back to the rule engine: {}",
                        resume.getId(), e.getMessage());
            }
        }
        return heuristicMatchScorer.score(resume, job);
    }

    private List<Resume> resolveCandidates(List<UUID> resumeIds) {
        if (resumeIds == null || resumeIds.isEmpty()) {
            return resumeRepository.findAllByOrderByCreatedAtDesc();
        }
        List<Resume> resumes = new ArrayList<>();
        for (UUID id : resumeIds) {
            resumes.add(resumeService.requireResume(id));
        }
        return resumes;
    }

    private ScreeningResponse buildResponse(JobDescription job,
                                            List<ScreeningResult> results,
                                            Double minScore,
                                            Integer topN,
                                            int evaluatedCount) {
        List<ScreeningResult> ranked = new ArrayList<>(results);
        ranked.sort(Comparator.comparingDouble(ScreeningResult::getMatchScore).reversed());

        List<CandidateScoreResponse> payload = new ArrayList<>();
        int rank = 0;
        int shortlisted = 0;
        for (ScreeningResult result : ranked) {
            if (minScore != null && result.getMatchScore() < minScore) {
                continue;
            }
            if (topN != null && payload.size() >= topN) {
                break;
            }
            CandidateScoreResponse candidate = DtoMapper.toCandidateScore(result, ++rank);
            if (candidate.shortlisted()) {
                shortlisted++;
            }
            payload.add(candidate);
        }

        return new ScreeningResponse(
                job.getId(),
                job.getTitle(),
                evaluatedCount,
                payload.size(),
                shortlisted,
                activeEngine(),
                payload);
    }
}
