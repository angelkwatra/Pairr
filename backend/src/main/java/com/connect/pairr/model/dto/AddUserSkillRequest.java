package com.connect.pairr.model.dto;

import com.connect.pairr.model.enums.ProficiencyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddUserSkillRequest(
        @NotNull @Schema(description = "ID of the skill to add") UUID skillId,
        @NotNull @Schema(description = "BEGINNER, AMATEUR, INTERMEDIATE, or EXPERT") ProficiencyLevel proficiency
) {}
