package com.vamsi.screener.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end test of the documented workflow: upload resumes, create a role, screen candidates.
 *
 * <p>The {@code test} profile disables the LLM, so this exercises the deterministic path and runs
 * offline in CI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScreeningFlowIntegrationTest {

    private static final String STRONG_RESUME = """
            PRIYA SHARMA
            Bengaluru, India | priya.sharma@example.com | +91 98765 43210

            SUMMARY
            Backend engineer with 6 years of experience building distributed Java services.

            TECHNICAL SKILLS
            Java, Spring Boot, PostgreSQL, Docker, Kubernetes, Apache Kafka, REST APIs

            WORK EXPERIENCE
            Senior Software Engineer, Fintech Labs (2021 - Present)
            Designed a payments platform on Spring Boot and PostgreSQL.
            Software Engineer, Cloud Systems (2018 - 2021)

            EDUCATION
            B.Tech in Computer Science, VIT University, 2018
            """;

    private static final String WEAK_RESUME = """
            RAHUL MEHTA
            Pune, India | rahul.mehta@example.com | +91 91234 56780

            SUMMARY
            Frontend developer with 2 years of experience building marketing websites.

            TECHNICAL SKILLS
            HTML, CSS, jQuery, Figma

            WORK EXPERIENCE
            Frontend Developer, Design Studio (2022 - Present)

            EDUCATION
            B.Sc in Design, Pune University, 2022
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("upload resumes, create a role, and get a ranked shortlist with justifications")
    void screensCandidatesEndToEnd() throws Exception {
        MockMultipartFile strong = new MockMultipartFile(
                "files", "priya.txt", MediaType.TEXT_PLAIN_VALUE, STRONG_RESUME.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile weak = new MockMultipartFile(
                "files", "rahul.txt", MediaType.TEXT_PLAIN_VALUE, WEAK_RESUME.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/resumes").file(strong).file(weak))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.received").value(2))
                .andExpect(jsonPath("$.parsed").value(2))
                .andExpect(jsonPath("$.rejected").isEmpty());

        String jobPayload = """
                {
                  "title": "Senior Backend Engineer",
                  "company": "Unthinkable",
                  "minYearsExperience": 4,
                  "description": "We need a backend engineer to build microservices with Java and Spring Boot, \
                backed by PostgreSQL and deployed with Docker on Kubernetes. A bachelor's degree in computer \
                science or equivalent experience is expected.",
                  "requiredSkills": ["Java", "Spring Boot", "PostgreSQL", "Docker"]
                }
                """;

        String jobResponse = mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiredSkills.length()").value(4))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String jobId = objectMapper.readTree(jobResponse).get("id").asText();

        mockMvc.perform(post("/api/v1/screenings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobDescriptionId\":\"" + jobId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidatesEvaluated").value(2))
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].rank").value(1))
                .andExpect(jsonPath("$.results[0].candidateName").value("Priya Sharma"))
                .andExpect(jsonPath("$.results[0].justification").isNotEmpty())
                .andExpect(jsonPath("$.results[0].matchedSkills.length()").value(4))
                .andExpect(jsonPath("$.results[1].candidateName").value("Rahul Mehta"));

        mockMvc.perform(get("/api/v1/screenings/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(2));
    }

    @Test
    @DisplayName("an invalid job description is rejected with field-level errors")
    void rejectsInvalidJobDescription() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"description\":\"too short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.description").exists());
    }

    @Test
    @DisplayName("an unknown resume id returns a 404 problem response")
    void returnsNotFoundForUnknownResume() throws Exception {
        mockMvc.perform(get("/api/v1/resumes/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    @DisplayName("the status endpoint reports the active engine")
    void reportsSystemStatus() throws Exception {
        mockMvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.llmConfigured").value(false))
                .andExpect(jsonPath("$.activeEngine").value("deterministic"));
    }
}
