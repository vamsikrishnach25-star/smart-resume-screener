package com.vamsi.screener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vamsi.screener.dto.CandidateScoreResponse;
import com.vamsi.screener.dto.ScreeningRequest;
import com.vamsi.screener.dto.ScreeningResponse;
import com.vamsi.screener.exception.LlmException;
import com.vamsi.screener.model.AnalysisSource;
import com.vamsi.screener.model.JobDescription;
import com.vamsi.screener.model.Recommendation;
import com.vamsi.screener.model.Resume;
import com.vamsi.screener.model.ScreeningResult;
import com.vamsi.screener.repository.ResumeRepository;
import com.vamsi.screener.repository.ScreeningResultRepository;
import com.vamsi.screener.service.llm.LlmClient;
import com.vamsi.screener.service.scoring.HeuristicMatchScorer;
import com.vamsi.screener.service.scoring.LlmMatchScorer;
import com.vamsi.screener.service.scoring.MatchAssessment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScreeningServiceTest {

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private ScreeningResultRepository screeningResultRepository;
    @Mock
    private ResumeService resumeService;
    @Mock
    private JobDescriptionService jobDescriptionService;
    @Mock
    private LlmClient llmClient;
    @Mock
    private LlmMatchScorer llmMatchScorer;
    @Mock
    private HeuristicMatchScorer heuristicMatchScorer;

    private ScreeningService screeningService;

    @BeforeEach
    void setUp() {
        screeningService = new ScreeningService(resumeRepository, screeningResultRepository, resumeService,
                jobDescriptionService, llmClient, llmMatchScorer, heuristicMatchScorer);
    }

    @Test
    @DisplayName("falls back to the rule engine when the model call fails")
    void fallsBackToHeuristicScorer() {
        JobDescription job = job();
        Resume resume = resume("Priya Sharma");

        when(jobDescriptionService.requireJob(job.getId())).thenReturn(job);
        when(resumeRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(resume));
        when(screeningResultRepository.findByResumeIdAndJobDescriptionId(resume.getId(), job.getId()))
                .thenReturn(Optional.empty());
        when(llmClient.isAvailable()).thenReturn(true);
        when(llmClient.provider()).thenReturn("gemini");
        when(llmClient.model()).thenReturn("gemini-3.6-flash");
        when(llmMatchScorer.score(resume, job)).thenThrow(new LlmException("model unavailable"));
        when(heuristicMatchScorer.score(resume, job)).thenReturn(assessment(7.5D));
        when(screeningResultRepository.save(any(ScreeningResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScreeningResponse response = screeningService.screen(
                new ScreeningRequest(job.getId(), null, null, null, null));

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).rank()).isEqualTo(1);
        assertThat(response.results().get(0).candidateName()).isEqualTo("Priya Sharma");
        assertThat(response.results().get(0).analysisSource()).isEqualTo(AnalysisSource.HEURISTIC);
        assertThat(response.engine()).isEqualTo("gemini:gemini-3.6-flash");
        verify(heuristicMatchScorer).score(resume, job);
    }

    @Test
    @DisplayName("reuses a stored verdict instead of scoring the same pair twice")
    void reusesStoredResults() {
        JobDescription job = job();
        Resume resume = resume("Ankit Verma");
        ScreeningResult stored = storedResult(resume, job, 8.8D);

        when(jobDescriptionService.requireJob(job.getId())).thenReturn(job);
        when(resumeRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(resume));
        when(screeningResultRepository.findByResumeIdAndJobDescriptionId(resume.getId(), job.getId()))
                .thenReturn(Optional.of(stored));
        when(llmClient.isAvailable()).thenReturn(false);

        ScreeningResponse response = screeningService.screen(
                new ScreeningRequest(job.getId(), null, null, null, false));

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).matchScore()).isEqualTo(8.8D);
        assertThat(response.engine()).isEqualTo("deterministic");
        verify(llmMatchScorer, never()).score(any(), any());
        verify(heuristicMatchScorer, never()).score(any(), any());
        verify(screeningResultRepository, never()).save(any(ScreeningResult.class));
    }

    @Test
    @DisplayName("ranks candidates by score and applies the minScore and topN filters")
    void ranksAndFiltersCandidates() {
        JobDescription job = job();
        Resume strong = resume("Strong Candidate");
        Resume weak = resume("Weak Candidate");
        Resume middle = resume("Middle Candidate");

        when(jobDescriptionService.requireJob(job.getId())).thenReturn(job);
        when(resumeService.requireResume(strong.getId())).thenReturn(strong);
        when(resumeService.requireResume(weak.getId())).thenReturn(weak);
        when(resumeService.requireResume(middle.getId())).thenReturn(middle);
        when(screeningResultRepository.findByResumeIdAndJobDescriptionId(strong.getId(), job.getId()))
                .thenReturn(Optional.of(storedResult(strong, job, 9.2D)));
        when(screeningResultRepository.findByResumeIdAndJobDescriptionId(weak.getId(), job.getId()))
                .thenReturn(Optional.of(storedResult(weak, job, 3.9D)));
        when(screeningResultRepository.findByResumeIdAndJobDescriptionId(middle.getId(), job.getId()))
                .thenReturn(Optional.of(storedResult(middle, job, 7.1D)));
        when(llmClient.isAvailable()).thenReturn(false);

        ScreeningResponse response = screeningService.screen(new ScreeningRequest(
                job.getId(),
                List.of(strong.getId(), weak.getId(), middle.getId()),
                5.0D,
                5,
                false));

        assertThat(response.candidatesEvaluated()).isEqualTo(3);
        assertThat(response.candidatesReturned()).isEqualTo(2);
        assertThat(response.results()).extracting(CandidateScoreResponse::candidateName)
                .containsExactly("Strong Candidate", "Middle Candidate");
        assertThat(response.results().get(0).rank()).isEqualTo(1);
        assertThat(response.results().get(1).rank()).isEqualTo(2);
    }

    // ------------------------------------------------------------------ fixtures

    private JobDescription job() {
        JobDescription job = new JobDescription();
        job.setId(UUID.randomUUID());
        job.setTitle("Backend Engineer");
        job.setDescription("Java and Spring Boot role.");
        return job;
    }

    private Resume resume(String name) {
        Resume resume = new Resume();
        resume.setId(UUID.randomUUID());
        resume.setFileName(name.toLowerCase().replace(' ', '-') + ".pdf");
        resume.setCandidateName(name);
        resume.setSkills(Set.of("Java"));
        return resume;
    }

    private MatchAssessment assessment(double score) {
        return MatchAssessment.of(score, score, score, score, Set.of("Java"), Set.of("Kafka"),
                "Solid backend background.", AnalysisSource.HEURISTIC);
    }

    private ScreeningResult storedResult(Resume resume, JobDescription job, double score) {
        ScreeningResult result = new ScreeningResult();
        result.setId(UUID.randomUUID());
        result.setResume(resume);
        result.setJobDescription(job);
        result.setMatchScore(score);
        result.setSkillScore(score);
        result.setExperienceScore(score);
        result.setEducationScore(score);
        result.setRecommendation(Recommendation.fromScore(score));
        result.setJustification("Stored verdict.");
        result.setMatchedSkills(Set.of("Java"));
        result.setMissingSkills(Set.of());
        result.setAnalysisSource(AnalysisSource.HEURISTIC);
        result.setModel("rule-engine");
        result.setEvaluatedAt(Instant.now());
        return result;
    }
}
