package com.connect.pairr.model.dto;

import java.util.UUID;

/**
 * API response DTO for a category. Exposes only safe fields and avoids leaking entity structure.
 */
public record CategoryResponse(
        UUID id,
        String name
) {}
