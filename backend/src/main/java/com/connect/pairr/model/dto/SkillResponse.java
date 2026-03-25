package com.connect.pairr.model.dto;

import java.util.UUID;

/**
 * API response DTO for a skill. Exposes only safe fields and avoids leaking entity structure.
 */
public record SkillResponse(
        UUID id,
        String name,
        UUID categoryId,
        String categoryName
) {}
