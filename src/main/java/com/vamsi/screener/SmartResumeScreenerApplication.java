package com.vamsi.screener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the Smart Resume Screener service.
 *
 * <p>The application exposes a REST API that ingests resumes (PDF/TXT), extracts structured
 * candidate data, and scores each candidate against a job description using an LLM, with a
 * deterministic fallback so the service keeps working when no API key is configured.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SmartResumeScreenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartResumeScreenerApplication.class, args);
    }
}
