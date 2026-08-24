package com.vamsi.screener.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TextUtilsTest {

    @Test
    @DisplayName("normalize collapses whitespace and repeated blank lines")
    void normalizeCleansUpPdfArtifacts() {
        String messy = "John   Doe\r\n\r\n\r\n   Senior    Engineer  \r\n\t\tJava\n\n\n\nSpring";

        String normalized = TextUtils.normalize(messy);

        assertThat(normalized).isEqualTo("John Doe\n\nSenior Engineer\nJava\n\nSpring");
    }

    @Test
    @DisplayName("normalize returns an empty string for null input")
    void normalizeHandlesNull() {
        assertThat(TextUtils.normalize(null)).isEmpty();
    }

    @Test
    @DisplayName("truncate only shortens text longer than the limit")
    void truncateShortensLongText() {
        assertThat(TextUtils.truncate("short", 20)).isEqualTo("short");
        assertThat(TextUtils.truncate("0123456789", 5)).startsWith("01234").contains("[truncated]");
    }

    @Test
    @DisplayName("sha256Hex is stable and differs for different content")
    void sha256IsStable() {
        String first = TextUtils.sha256Hex("resume text");
        String second = TextUtils.sha256Hex("resume text");
        String other = TextUtils.sha256Hex("different text");

        assertThat(first).hasSize(64).isEqualTo(second).isNotEqualTo(other);
    }

    @Test
    @DisplayName("clampScore keeps scores inside the 1-10 band and rounds to one decimal")
    void clampScoreBoundsValues() {
        assertThat(TextUtils.clampScore(12.4D)).isEqualTo(10.0D);
        assertThat(TextUtils.clampScore(-3.0D)).isEqualTo(1.0D);
        assertThat(TextUtils.clampScore(7.4499D)).isEqualTo(7.4D);
    }

    @Test
    @DisplayName("trimToNull turns blank values into null")
    void trimToNullHandlesBlanks() {
        assertThat(TextUtils.trimToNull("  ")).isNull();
        assertThat(TextUtils.trimToNull(null)).isNull();
        assertThat(TextUtils.trimToNull("  Java  ")).isEqualTo("Java");
    }
}
