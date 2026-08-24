package com.vamsi.screener.service.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SkillDictionaryTest {

    private SkillDictionary dictionary;

    @BeforeEach
    void setUp() {
        dictionary = new SkillDictionary();
    }

    @Test
    @DisplayName("loads a non-trivial dictionary from the classpath")
    void loadsDictionary() {
        assertThat(dictionary.size()).isGreaterThan(100);
    }

    @Test
    @DisplayName("finds skills in free text using canonical spelling")
    void findsSkillsInText() {
        String text = "Built REST APIs with spring boot and postgres, deployed on Kubernetes.";

        Set<String> skills = dictionary.extractFrom(text);

        assertThat(skills).contains("Spring Boot", "PostgreSQL", "Kubernetes", "REST API");
    }

    @Test
    @DisplayName("respects token boundaries so Java does not match inside JavaScript")
    void respectsTokenBoundaries() {
        Set<String> skills = dictionary.extractFrom("Strong JavaScript and TypeScript developer.");

        assertThat(skills).contains("JavaScript", "TypeScript");
        assertThat(skills).doesNotContain("Java");
    }

    @Test
    @DisplayName("a skill matches at the end of a sentence and beside a hyphen")
    void matchesAtTokenEdges() {
        assertThat(dictionary.extractFrom("Everything runs on Kubernetes.")).contains("Kubernetes");
        assertThat(dictionary.extractFrom("Built a Redis-backed cache")).contains("Redis");
        assertThat(dictionary.extractFrom("Owned the Go-live checklist")).doesNotContain("Go");
    }

    @Test
    @DisplayName("short aliases are case sensitive to avoid false positives")
    void shortAliasesAreCaseSensitive() {
        assertThat(dictionary.extractFrom("I will go to the office and see the R and D team"))
                .doesNotContain("Go");
        assertThat(dictionary.extractFrom("Languages: Go, C, Java")).contains("Go", "C", "Java");
    }

    @Test
    @DisplayName("canonicalize maps aliases onto the canonical name and keeps unknown skills")
    void canonicalizesAliases() {
        assertThat(dictionary.canonicalize("react.js")).isEqualTo("React");
        assertThat(dictionary.canonicalize("  NODEJS ")).isEqualTo("Node.js");
        assertThat(dictionary.canonicalize("Proprietary In-House Tool")).isEqualTo("Proprietary In-House Tool");
        assertThat(dictionary.canonicalize("   ")).isNull();
    }
}
