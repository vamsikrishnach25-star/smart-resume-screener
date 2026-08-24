package com.vamsi.screener.service;

import com.vamsi.screener.dto.DtoMapper;
import com.vamsi.screener.dto.JobDescriptionRequest;
import com.vamsi.screener.dto.JobDescriptionResponse;
import com.vamsi.screener.exception.ResourceNotFoundException;
import com.vamsi.screener.model.JobDescription;
import com.vamsi.screener.repository.JobDescriptionRepository;
import com.vamsi.screener.repository.ScreeningResultRepository;
import com.vamsi.screener.service.extraction.SkillDictionary;
import com.vamsi.screener.util.TextUtils;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates and serves the roles that candidates are screened against. */
@Service
public class JobDescriptionService {

    private static final Logger log = LoggerFactory.getLogger(JobDescriptionService.class);

    private final JobDescriptionRepository jobRepository;
    private final ScreeningResultRepository screeningResultRepository;
    private final SkillDictionary skillDictionary;

    public JobDescriptionService(JobDescriptionRepository jobRepository,
                                 ScreeningResultRepository screeningResultRepository,
                                 SkillDictionary skillDictionary) {
        this.jobRepository = jobRepository;
        this.screeningResultRepository = screeningResultRepository;
        this.skillDictionary = skillDictionary;
    }

    /**
     * Stores a job description. When the caller does not list required skills, they are inferred
     * from the description text so screening always has something concrete to match against.
     */
    @Transactional
    public JobDescriptionResponse create(JobDescriptionRequest request) {
        JobDescription job = new JobDescription();
        job.setTitle(request.title().trim());
        job.setCompany(TextUtils.trimToNull(request.company()));
        job.setLocation(TextUtils.trimToNull(request.location()));
        job.setMinYearsExperience(request.minYearsExperience());
        job.setDescription(TextUtils.normalize(request.description()));
        job.setRequiredSkills(resolveSkills(request));

        JobDescription saved = jobRepository.save(job);
        log.info("Stored job '{}' ({}) with {} required skills",
                saved.getTitle(), saved.getId(), saved.getRequiredSkills().size());
        return DtoMapper.toResponse(saved);
    }

    private Set<String> resolveSkills(JobDescriptionRequest request) {
        Set<String> skills = new LinkedHashSet<>();
        if (request.requiredSkills() != null) {
            for (String raw : request.requiredSkills()) {
                String canonical = skillDictionary.canonicalize(raw);
                if (canonical != null && !canonical.isBlank()) {
                    skills.add(canonical);
                }
            }
        }
        if (skills.isEmpty()) {
            skills.addAll(skillDictionary.extractFrom(request.description()));
        }
        return skills;
    }

    @Transactional(readOnly = true)
    public List<JobDescriptionResponse> findAll() {
        return jobRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(DtoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobDescriptionResponse findById(UUID id) {
        return DtoMapper.toResponse(requireJob(id));
    }

    @Transactional(readOnly = true)
    public JobDescription requireJob(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Job description", id));
    }

    @Transactional
    public void delete(UUID id) {
        JobDescription job = requireJob(id);
        screeningResultRepository.deleteByJobDescriptionId(job.getId());
        jobRepository.delete(job);
        log.info("Deleted job description {}", id);
    }

    @Transactional(readOnly = true)
    public long count() {
        return jobRepository.count();
    }
}
