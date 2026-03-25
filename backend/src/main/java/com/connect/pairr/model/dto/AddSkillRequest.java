package com.connect.pairr.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddSkillRequest(
        @NotBlank String name,
        @NotNull UUID categoryId
) {}
