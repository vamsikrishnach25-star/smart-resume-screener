package com.vamsi.screener.dto;

/** An education row as returned by the API. */
public record EducationDto(String institution, String degree, String fieldOfStudy, String year) {
}
