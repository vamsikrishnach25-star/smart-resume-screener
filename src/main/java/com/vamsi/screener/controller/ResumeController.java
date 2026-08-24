package com.vamsi.screener.controller;

import com.vamsi.screener.dto.ResumeDetailResponse;
import com.vamsi.screener.dto.ResumeSummaryResponse;
import com.vamsi.screener.dto.UploadResponse;
import com.vamsi.screener.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Upload and inspect parsed resumes. */
@RestController
@RequestMapping("/api/v1/resumes")
@Tag(name = "Resumes", description = "Upload resumes and read the structured data extracted from them")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @Operation(summary = "Upload one or more resumes (PDF or TXT) and extract structured data")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@RequestParam("files") List<MultipartFile> files) {
        return resumeService.upload(files);
    }

    @Operation(summary = "List every stored resume, newest first")
    @GetMapping
    public List<ResumeSummaryResponse> list() {
        return resumeService.findAll();
    }

    @Operation(summary = "Fetch one resume with its full extracted profile")
    @GetMapping("/{id}")
    public ResumeDetailResponse get(@PathVariable UUID id) {
        return resumeService.findById(id);
    }

    @Operation(summary = "Delete a resume and any screening results that reference it")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        resumeService.delete(id);
    }
}
