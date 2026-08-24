package com.vamsi.screener.dto;

import java.util.List;
import java.util.UUID;

/** Ranked output of a screening run. */
public record ScreeningResponse(
        UUID jobDescriptionId,
        String jobTitle,
        int candidatesEvaluated,
        int candidatesReturned,
        int shortlistedCount,
        String engine,
        List<CandidateScoreResponse> results) {
}
