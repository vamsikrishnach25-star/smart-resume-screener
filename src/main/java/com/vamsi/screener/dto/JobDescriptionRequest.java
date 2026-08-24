package com.vamsi.screener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Payload for creating a job description. */
public record JobDescriptionRequest(
        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @Size(max = 200, message = "company must be at most 200 characters")
        String company,

        @Size(max = 200, message = "location must be at most 200 characters")
        String location,

        @PositiveOrZero(message = "minYearsExperience cannot be negative")
        Double minYearsExperience,

        @NotBlank(message = "description is required")
        @Size(min = 30, max = 40000, message = "description must be between 30 and 40000 characters")
        String description,

        List<String> requiredSkills) {
}
