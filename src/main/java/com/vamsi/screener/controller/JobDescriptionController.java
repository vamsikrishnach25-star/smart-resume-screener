package com.vamsi.screener.controller;

import com.vamsi.screener.dto.JobDescriptionRequest;
import com.vamsi.screener.dto.JobDescriptionResponse;
import com.vamsi.screener.service.JobDescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Create and manage the roles candidates are screened against. */
@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Job descriptions", description = "Create and manage roles")
public class JobDescriptionController {

    private final JobDescriptionService jobDescriptionService;

    public JobDescriptionController(JobDescriptionService jobDescriptionService) {
        this.jobDescriptionService = jobDescriptionService;
    }

    @Operation(summary = "Create a job description; required skills are inferred when not supplied")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobDescriptionResponse create(@Valid @RequestBody JobDescriptionRequest request) {
        return jobDescriptionService.create(request);
    }

    @Operation(summary = "List every stored job description, newest first")
    @GetMapping
    public List<JobDescriptionResponse> list() {
        return jobDescriptionService.findAll();
    }

    @Operation(summary = "Fetch one job description")
    @GetMapping("/{id}")
    public JobDescriptionResponse get(@PathVariable UUID id) {
        return jobDescriptionService.findById(id);
    }

    @Operation(summary = "Delete a job description and its screening results")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        jobDescriptionService.delete(id);
    }
}
