package com.vamsi.screener.dto;

/** A work-history row as returned by the API. */
public record ExperienceDto(String company, String title, String period, String description) {
}
