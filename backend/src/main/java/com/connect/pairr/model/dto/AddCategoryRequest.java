package com.connect.pairr.model.dto;

import jakarta.validation.constraints.NotBlank;

public record AddCategoryRequest(
        @NotBlank String name
) {}
