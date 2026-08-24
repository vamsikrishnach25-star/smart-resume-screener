package com.vamsi.screener.controller;

import com.vamsi.screener.dto.ScreeningRequest;
import com.vamsi.screener.dto.ScreeningResponse;
import com.vamsi.screener.service.ScreeningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Score and rank candidates against a role. */
@RestController
@RequestMapping("/api/v1/screenings")
@Tag(name = "Screening", description = "Score candidates against a role and read the shortlist")
public class ScreeningController {

    private final ScreeningService screeningService;

    public ScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    @Operation(summary = "Screen resumes against a job description and return them ranked with justifications")
    @PostMapping
    public ScreeningResponse screen(@Valid @RequestBody ScreeningRequest request) {
        return screeningService.screen(request);
    }

    @Operation(summary = "Read the stored ranking for a job description without re-scoring")
    @GetMapping("/jobs/{jobId}")
    public ScreeningResponse resultsForJob(@PathVariable UUID jobId) {
        return screeningService.resultsForJob(jobId);
    }
}
