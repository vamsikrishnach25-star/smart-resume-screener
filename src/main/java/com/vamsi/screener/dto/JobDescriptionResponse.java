package com.vamsi.screener.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Job description as returned by the API. */
public record JobDescriptionResponse(
        UUID id,
        String title,
        String company,
        String location,
        Double minYearsExperience,
        String description,
        Set<String> requiredSkills,
        Instant createdAt) {
}
