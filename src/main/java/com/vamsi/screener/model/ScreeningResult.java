package com.vamsi.screener.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** The stored outcome of screening one resume against one job description. */
@Entity
@Table(
        name = "screening_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_screening_resume_job",
                columnNames = {"resume_id", "job_id"}))
public class ScreeningResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private JobDescription jobDescription;

    /** Overall fit, 1 (poor) to 10 (excellent). */
    @Column(name = "match_score", nullable = false)
    private double matchScore;

    @Column(name = "skill_score")
    private double skillScore;

    @Column(name = "experience_score")
    private double experienceScore;

    @Column(name = "education_score")
    private double educationScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation", length = 30, nullable = false)
    private Recommendation recommendation;

    @Column(name = "justification", length = 4000)
    private String justification;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "screening_matched_skills", joinColumns = @JoinColumn(name = "result_id"))
    @Column(name = "skill", length = 120)
    private Set<String> matchedSkills = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "screening_missing_skills", joinColumns = @JoinColumn(name = "result_id"))
    @Column(name = "skill", length = 120)
    private Set<String> missingSkills = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_source", length = 20, nullable = false)
    private AnalysisSource analysisSource;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "latency_ms")
    private long latencyMs;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @PrePersist
    void onCreate() {
        if (evaluatedAt == null) {
            evaluatedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Resume getResume() {
        return resume;
    }

    public void setResume(Resume resume) {
        this.resume = resume;
    }

    public JobDescription getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(JobDescription jobDescription) {
        this.jobDescription = jobDescription;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
    }

    public double getSkillScore() {
        return skillScore;
    }

    public void setSkillScore(double skillScore) {
        this.skillScore = skillScore;
    }

    public double getExperienceScore() {
        return experienceScore;
    }

    public void setExperienceScore(double experienceScore) {
        this.experienceScore = experienceScore;
    }

    public double getEducationScore() {
        return educationScore;
    }

    public void setEducationScore(double educationScore) {
        this.educationScore = educationScore;
    }

    public Recommendation getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(Recommendation recommendation) {
        this.recommendation = recommendation;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public Set<String> getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(Set<String> matchedSkills) {
        this.matchedSkills = matchedSkills == null ? new LinkedHashSet<>() : new LinkedHashSet<>(matchedSkills);
    }

    public Set<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(Set<String> missingSkills) {
        this.missingSkills = missingSkills == null ? new LinkedHashSet<>() : new LinkedHashSet<>(missingSkills);
    }

    public AnalysisSource getAnalysisSource() {
        return analysisSource;
    }

    public void setAnalysisSource(AnalysisSource analysisSource) {
        this.analysisSource = analysisSource;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
