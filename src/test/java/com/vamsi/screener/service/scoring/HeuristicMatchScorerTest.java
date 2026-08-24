package com.vamsi.screener.service.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.vamsi.screener.config.AppProperties;
import com.vamsi.screener.model.AnalysisSource;
import com.vamsi.screener.model.JobDescription;
import com.vamsi.screener.model.Recommendation;
import com.vamsi.screener.model.Resume;
import com.vamsi.screener.service.extraction.SkillDictionary;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HeuristicMatchScorerTest {

    private HeuristicMatchScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new HeuristicMatchScorer(new JobSkillResolver(new SkillDictionary()), new AppProperties());
    }

    @Test
    @DisplayName("a candidate covering every requirement scores as a strong match")
    void strongCandidateScoresHigh() {
        JobDescription job = job(3.0D, List.of("Java", "Spring Boot", "PostgreSQL"));
        Resume resume = resume(6.0D, "Bachelor's degree", Set.of("Java", "Spring Boot", "PostgreSQL", "Docker"));

        MatchAssessment assessment = scorer.score(resume, job);

        assertThat(assessment.matchScore()).isGreaterThanOrEqualTo(8.5D);
        assertThat(assessment.recommendation()).isEqualTo(Recommendation.STRONG_MATCH);
        assertThat(assessment.matchedSkills()).containsExactlyInAnyOrder("Java", "Spring Boot", "PostgreSQL");
        assertThat(assessment.missingSkills()).isEmpty();
        assertThat(assessment.source()).isEqualTo(AnalysisSource.HEURISTIC);
    }

    @Test
    @DisplayName("a candidate with none of the required skills is not a match")
    void weakCandidateScoresLow() {
        JobDescription job = job(3.0D, List.of("Java", "Spring Boot", "PostgreSQL"));
        Resume resume = resume(null, null, Set.of("PHP"));

        MatchAssessment assessment = scorer.score(resume, job);

        assertThat(assessment.matchScore()).isLessThan(5.0D);
        assertThat(assessment.recommendation()).isEqualTo(Recommendation.NOT_A_MATCH);
        assertThat(assessment.missingSkills()).containsExactlyInAnyOrder("Java", "Spring Boot", "PostgreSQL");
        assertThat(assessment.matchedSkills()).isEmpty();
    }

    @Test
    @DisplayName("experience below the requirement drags the score down")
    void juniorCandidateScoresLowerThanSenior() {
        JobDescription job = job(6.0D, List.of("Java", "Spring Boot"));
        Resume senior = resume(8.0D, "Bachelor's degree", Set.of("Java", "Spring Boot"));
        Resume junior = resume(1.0D, "Bachelor's degree", Set.of("Java", "Spring Boot"));

        assertThat(scorer.score(junior, job).matchScore())
                .isLessThan(scorer.score(senior, job).matchScore());
    }

    @Test
    @DisplayName("every score stays inside the 1-10 band and carries a justification")
    void scoresAreBoundedAndExplained() {
        JobDescription job = job(null, List.of());
        Resume resume = resume(2.0D, null, Set.of("Java"));

        MatchAssessment assessment = scorer.score(resume, job);

        assertThat(assessment.matchScore()).isBetween(1.0D, 10.0D);
        assertThat(assessment.skillScore()).isBetween(1.0D, 10.0D);
        assertThat(assessment.experienceScore()).isBetween(1.0D, 10.0D);
        assertThat(assessment.educationScore()).isBetween(1.0D, 10.0D);
        assertThat(assessment.justification()).isNotBlank();
    }

    private JobDescription job(Double minYears, List<String> requiredSkills) {
        JobDescription job = new JobDescription();
        job.setTitle("Backend Engineer");
        job.setMinYearsExperience(minYears);
        job.setDescription("We are hiring a backend engineer. A bachelor's degree or equivalent "
                + "practical experience is expected.");
        job.setRequiredSkills(new java.util.LinkedHashSet<>(requiredSkills));
        return job;
    }

    private Resume resume(Double years, String education, Set<String> skills) {
        Resume resume = new Resume();
        resume.setFileName("candidate.pdf");
        resume.setYearsOfExperience(years);
        resume.setHighestEducation(education);
        resume.setSkills(skills);
        return resume;
    }
}
