package com.vamsi.screener.controller;

import com.vamsi.screener.config.AppProperties;
import com.vamsi.screener.dto.SystemStatusResponse;
import com.vamsi.screener.service.JobDescriptionService;
import com.vamsi.screener.service.ResumeService;
import com.vamsi.screener.service.ScreeningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reports which scoring engine is active and how much data is stored. */
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "Runtime status")
public class SystemController {

    private final AppProperties.Llm llmConfig;
    private final ResumeService resumeService;
    private final JobDescriptionService jobDescriptionService;
    private final ScreeningService screeningService;

    public SystemController(AppProperties properties,
                            ResumeService resumeService,
                            JobDescriptionService jobDescriptionService,
                            ScreeningService screeningService) {
        this.llmConfig = properties.getLlm();
        this.resumeService = resumeService;
        this.jobDescriptionService = jobDescriptionService;
        this.screeningService = screeningService;
    }

    @Operation(summary = "Report the active scoring engine, model and stored record counts")
    @GetMapping("/status")
    public SystemStatusResponse status() {
        return new SystemStatusResponse(
                "Smart Resume Screener",
                "1.0.0",
                llmConfig.isEnabled(),
                llmConfig.isConfigured(),
                llmConfig.getProvider(),
                llmConfig.getModel(),
                screeningService.activeEngine(),
                resumeService.count(),
                jobDescriptionService.count());
    }
}
