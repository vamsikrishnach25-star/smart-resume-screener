package com.vamsi.screener.dto;

import java.util.List;

/**
 * Result of a bulk resume upload. Files are processed independently so one bad file never fails
 * the whole batch.
 */
public record UploadResponse(
        int received,
        int parsed,
        List<ResumeSummaryResponse> resumes,
        List<RejectedFile> rejected) {

    /** A file that could not be parsed, with the reason why. */
    public record RejectedFile(String fileName, String reason) {
    }
}
