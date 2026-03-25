package com.connect.pairr.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddRatingRequest(
        @NotNull @Schema(description = "ID of the user being rated") UUID toUserId,
        @NotNull @Schema(description = "ID of the skill being rated on") UUID skillId,
        @NotNull @Min(1) @Max(5) @Schema(description = "Rating score", minimum = "1", maximum = "5", example = "4") Integer rating,
        @Size(max = 500) @Schema(description = "Optional text feedback (max 500 characters)") String feedback
) {}
