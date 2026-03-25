package com.connect.pairr.model.dto;

import com.connect.pairr.model.enums.Role;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * API response DTO for a user. Exposes only safe fields; never includes password.
 */
public record UserResponse(
        UUID id,
        String email,
        String username,
        String displayName,
        Role role,
        BigDecimal overallRating,
        Long completedSessionsCount,
        Instant createdAt
) {}
