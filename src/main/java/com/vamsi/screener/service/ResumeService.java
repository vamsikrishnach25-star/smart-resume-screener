package com.vamsi.screener.service;

import com.vamsi.screener.config.AppProperties;
import com.vamsi.screener.dto.DtoMapper;
import com.vamsi.screener.dto.ResumeDetailResponse;
import com.vamsi.screener.dto.ResumeSummaryResponse;
import com.vamsi.screener.dto.UploadResponse;
import com.vamsi.screener.exception.DocumentParsingException;
import com.vamsi.screener.exception.ResourceNotFoundException;
import com.vamsi.screener.model.Resume;
import com.vamsi.screener.repository.ResumeRepository;
import com.vamsi.screener.repository.ScreeningResultRepository;
import com.vamsi.screener.service.extraction.DocumentTextExtractor;
import com.vamsi.screener.service.extraction.ExtractedResumeData;
import com.vamsi.screener.service.extraction.ResumeExtractionService;
import com.vamsi.screener.util.TextUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Ingests and serves resumes.
 *
 * <p>Uploads are processed file by file so a single unreadable PDF cannot fail an entire batch, and
 * a resume whose text has already been parsed is reused instead of being sent to the model again.
 */
@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final ScreeningResultRepository screeningResultRepository;
    private final DocumentTextExtractor textExtractor;
    private final ResumeExtractionService extractionService;
    private final AppProperties.Upload uploadConfig;

    public ResumeService(ResumeRepository resumeRepository,
                         ScreeningResultRepository screeningResultRepository,
                         DocumentTextExtractor textExtractor,
                         ResumeExtractionService extractionService,
                         AppProperties properties) {
        this.resumeRepository = resumeRepository;
        this.screeningResultRepository = screeningResultRepository;
        this.textExtractor = textExtractor;
        this.extractionService = extractionService;
        this.uploadConfig = properties.getUpload();
    }

    /**
     * Parses and stores a batch of uploaded resumes.
     *
     * @param files uploaded files
     * @return the resumes that parsed, plus a reason for each file that did not
     */
    public UploadResponse upload(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file must be provided under the 'files' field.");
        }
        if (files.size() > uploadConfig.getMaxFilesPerRequest()) {
            throw new IllegalArgumentException("At most " + uploadConfig.getMaxFilesPerRequest()
                    + " files can be uploaded in one request.");
        }

        List<ResumeSummaryResponse> parsed = new ArrayList<>();
        List<UploadResponse.RejectedFile> rejected = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename() == null ? "resume" : file.getOriginalFilename();
            try {
                parsed.add(DtoMapper.toSummary(store(file, fileName)));
            } catch (DocumentParsingException | IllegalArgumentException e) {
                log.warn("Rejected '{}': {}", fileName, e.getMessage());
                rejected.add(new UploadResponse.RejectedFile(fileName, e.getMessage()));
            } catch (IOException e) {
                log.warn("Could not read '{}': {}", fileName, e.getMessage());
                rejected.add(new UploadResponse.RejectedFile(fileName, "The file could not be read."));
            }
        }

        return new UploadResponse(files.size(), parsed.size(), parsed, rejected);
    }

    private Resume store(MultipartFile file, String fileName) throws IOException {
        String text = textExtractor.extract(fileName, file.getBytes());
        String contentHash = TextUtils.sha256Hex(text);

        Optional<Resume> existing = resumeRepository.findByContentHash(contentHash);
        if (existing.isPresent()) {
            log.info("Resume '{}' matches an already parsed resume; reusing {}", fileName, existing.get().getId());
            return existing.get();
        }

        ExtractedResumeData data = extractionService.extract(text);

        Resume resume = new Resume();
        resume.setFileName(fileName);
        resume.setContentType(file.getContentType());
        resume.setFileSizeBytes(file.getSize());
        resume.setContentHash(contentHash);
        resume.setRawText(text);
        resume.setCandidateName(data.candidateName());
        resume.setEmail(data.email());
        resume.setPhone(data.phone());
        resume.setLocation(data.location());
        resume.setSummary(data.summary());
        resume.setYearsOfExperience(data.yearsOfExperience());
        resume.setHighestEducation(data.highestEducation());
        resume.setSkills(data.skills());
        resume.setExperiences(data.experiences());
        resume.setEducations(data.educations());
        resume.setExtractionSource(data.source());

        Resume saved = resumeRepository.save(resume);
        log.info("Parsed '{}' via {} into {} skills (id {})",
                fileName, data.source(), saved.getSkills().size(), saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ResumeSummaryResponse> findAll() {
        return resumeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(DtoMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeDetailResponse findById(UUID id) {
        return DtoMapper.toDetail(requireResume(id));
    }

    @Transactional(readOnly = true)
    public Resume requireResume(UUID id) {
        return resumeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Resume", id));
    }

    @Transactional
    public void delete(UUID id) {
        Resume resume = requireResume(id);
        screeningResultRepository.deleteByResumeId(resume.getId());
        resumeRepository.delete(resume);
        log.info("Deleted resume {}", id);
    }

    @Transactional(readOnly = true)
    public long count() {
        return resumeRepository.count();
    }
}
