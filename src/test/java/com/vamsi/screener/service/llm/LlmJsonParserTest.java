package com.vamsi.screener.service.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vamsi.screener.exception.LlmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LlmJsonParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("reads JSON that the model wrapped in markdown fences")
    void extractsFencedJson() {
        String response = "```json\n{\"matchScore\": 8.5, \"justification\": \"Strong fit\"}\n```";

        MatchAssessmentPayload payload =
                LlmJsonParser.parse(objectMapper, response, MatchAssessmentPayload.class);

        assertThat(payload.matchScore()).isEqualTo(8.5D);
        assertThat(payload.justification()).isEqualTo("Strong fit");
    }

    @Test
    @DisplayName("reads JSON surrounded by commentary and keeps nested objects intact")
    void extractsJsonWithSurroundingProse() {
        String response = "Here is the result:\n{\"matchScore\": 6, \"nested\": {\"a\": {\"b\": 1}}}\nHope that helps!";

        String json = LlmJsonParser.extractJsonObject(response);

        assertThat(json).isEqualTo("{\"matchScore\": 6, \"nested\": {\"a\": {\"b\": 1}}}");
    }

    @Test
    @DisplayName("ignores braces that appear inside string values")
    void ignoresBracesInsideStrings() {
        String response = "{\"justification\": \"uses } and { in text\", \"matchScore\": 7}";

        MatchAssessmentPayload payload =
                LlmJsonParser.parse(objectMapper, response, MatchAssessmentPayload.class);

        assertThat(payload.justification()).isEqualTo("uses } and { in text");
        assertThat(payload.matchScore()).isEqualTo(7.0D);
    }

    @Test
    @DisplayName("unknown fields in the model output do not break parsing")
    void toleratesUnknownFields() {
        String response = "{\"matchScore\": 5, \"confidence\": \"high\", \"extra\": [1, 2, 3]}";

        MatchAssessmentPayload payload =
                LlmJsonParser.parse(objectMapper, response, MatchAssessmentPayload.class);

        assertThat(payload.matchScore()).isEqualTo(5.0D);
    }

    @Test
    @DisplayName("a response without JSON fails loudly so the caller can fall back")
    void rejectsResponsesWithoutJson() {
        assertThatThrownBy(() -> LlmJsonParser.extractJsonObject("I cannot help with that."))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("did not contain a JSON object");

        assertThatThrownBy(() -> LlmJsonParser.extractJsonObject("{\"matchScore\": 5"))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("unterminated");

        assertThatThrownBy(() -> LlmJsonParser.extractJsonObject("   "))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("empty response");
    }
}
