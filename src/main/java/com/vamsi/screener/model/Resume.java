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
import jakarta.persistence.Lob;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A parsed resume together with the structured data extracted from it. */
@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "file_name", nullable = false, length = 260)
    private String fileName;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "file_size_bytes")
    private long fileSizeBytes;

    /** SHA-256 of the extracted text; used to detect re-uploads of the same resume. */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "candidate_name", length = 200)
    private String candidateName;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "phone", length = 60)
    private String phone;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "years_of_experience")
    private Double yearsOfExperience;

    @Column(name = "highest_education", length = 200)
    private String highestEducation;

    @Column(name = "summary", length = 2000)
    private String summary;

    @Lob
    @Column(name = "raw_text")
    private String rawText;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_source", length = 20)
    private AnalysisSource extractionSource;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "resume_skills", joinColumns = @JoinColumn(name = "resume_id"))
    @Column(name = "skill", length = 120)
    private Set<String> skills = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "resume_experience", joinColumns = @JoinColumn(name = "resume_id"))
    @OrderColumn(name = "entry_index")
    private List<ExperienceEntry> experiences = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "resume_education", joinColumns = @JoinColumn(name = "resume_id"))
    @OrderColumn(name = "entry_index")
    private List<EducationEntry> educations = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Double yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getHighestEducation() {
        return highestEducation;
    }

    public void setHighestEducation(String highestEducation) {
        this.highestEducation = highestEducation;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public AnalysisSource getExtractionSource() {
        return extractionSource;
    }

    public void setExtractionSource(AnalysisSource extractionSource) {
        this.extractionSource = extractionSource;
    }

    public Set<String> getSkills() {
        return skills;
    }

    public void setSkills(Set<String> skills) {
        this.skills = skills == null ? new LinkedHashSet<>() : new LinkedHashSet<>(skills);
    }

    public List<ExperienceEntry> getExperiences() {
        return experiences;
    }

    public void setExperiences(List<ExperienceEntry> experiences) {
        this.experiences = experiences == null ? new ArrayList<>() : new ArrayList<>(experiences);
    }

    public List<EducationEntry> getEducations() {
        return educations;
    }

    public void setEducations(List<EducationEntry> educations) {
        this.educations = educations == null ? new ArrayList<>() : new ArrayList<>(educations);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /** @return the candidate name when known, otherwise the uploaded file name. */
    public String displayName() {
        return candidateName == null || candidateName.isBlank() ? fileName : candidateName;
    }
}
