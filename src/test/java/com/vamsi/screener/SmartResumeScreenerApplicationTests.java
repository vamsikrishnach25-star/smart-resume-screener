package com.vamsi.screener;

import static org.assertj.core.api.Assertions.assertThat;

import com.vamsi.screener.service.ScreeningService;
import com.vamsi.screener.service.extraction.SkillDictionary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SmartResumeScreenerApplicationTests {

    @Autowired
    private ScreeningService screeningService;

    @Autowired
    private SkillDictionary skillDictionary;

    @Test
    @DisplayName("the application context starts and wires the screening pipeline")
    void contextLoads() {
        assertThat(screeningService).isNotNull();
        assertThat(skillDictionary.size()).isGreaterThan(100);
    }

    @Test
    @DisplayName("without an API key the deterministic engine is active")
    void reportsDeterministicEngineWithoutApiKey() {
        assertThat(screeningService.activeEngine()).isEqualTo("deterministic");
    }
}
