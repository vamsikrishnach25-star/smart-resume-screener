package com.vamsi.screener.service.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import com.vamsi.screener.model.AnalysisSource;
import com.vamsi.screener.util.TextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HeuristicResumeExtractorTest {

    private static final String RESUME = """
            PRIYA SHARMA
            Bengaluru, India | priya.sharma@example.com | +91 98765 43210

            SUMMARY
            Backend engineer with 5 years of experience building Java microservices at scale.

            TECHNICAL SKILLS
            Java, Spring Boot, PostgreSQL, Docker, Kubernetes, Kafka, REST APIs

            WORK EXPERIENCE
            Senior Software Engineer, Fintech Labs (2021 - Present)
            Led a team of four engineers on a payments platform.
            Software Engineer, Cloud Systems (2019 - 2021)

            EDUCATION
            B.Tech in Computer Science, VIT University, 2019
            """;

    private HeuristicResumeExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new HeuristicResumeExtractor(new SkillDictionary());
    }

    @Test
    @DisplayName("extracts contact details from the resume header")
    void extractsContactDetails() {
        ExtractedResumeData data = extractor.extract(TextUtils.normalize(RESUME));

        assertThat(data.candidateName()).isEqualTo("Priya Sharma");
        assertThat(data.email()).isEqualTo("priya.sharma@example.com");
        assertThat(data.phone()).contains("98765");
        assertThat(data.source()).isEqualTo(AnalysisSource.HEURISTIC);
    }

    @Test
    @DisplayName("extracts skills from both the skills section and the body text")
    void extractsSkills() {
        ExtractedResumeData data = extractor.extract(TextUtils.normalize(RESUME));

        assertThat(data.skills())
                .contains("Java", "Spring Boot", "PostgreSQL", "Docker", "Kubernetes", "Apache Kafka");
    }

    @Test
    @DisplayName("reads the stated years of experience")
    void extractsYearsOfExperience() {
        ExtractedResumeData data = extractor.extract(TextUtils.normalize(RESUME));

        assertThat(data.yearsOfExperience()).isEqualTo(5.0D);
    }

    @Test
    @DisplayName("extracts education and derives the highest degree")
    void extractsEducation() {
        ExtractedResumeData data = extractor.extract(TextUtils.normalize(RESUME));

        assertThat(data.highestEducation()).isEqualTo("Bachelor's degree");
        assertThat(data.educations()).isNotEmpty();
        assertThat(data.educations().get(0).getYear()).isEqualTo("2019");
    }

    @Test
    @DisplayName("extracts dated experience rows and their periods")
    void extractsExperience() {
        ExtractedResumeData data = extractor.extract(TextUtils.normalize(RESUME));

        assertThat(data.experiences()).isNotEmpty();
        assertThat(data.experiences().get(0).getTitle()).contains("Senior Software Engineer");
        assertThat(data.experiences().get(0).getPeriod()).contains("2021");
    }

    @Test
    @DisplayName("empty input never throws and returns empty data")
    void handlesEmptyInput() {
        ExtractedResumeData data = extractor.extract("");

        assertThat(data.candidateName()).isNull();
        assertThat(data.email()).isNull();
        assertThat(data.skills()).isEmpty();
        assertThat(data.experiences()).isEmpty();
    }
}
