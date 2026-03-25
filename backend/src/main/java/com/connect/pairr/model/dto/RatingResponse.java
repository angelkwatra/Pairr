package com.connect.pairr.model.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record RatingResponse(
        UUID id,
        UUID fromUserId,
        String fromUserDisplayName,
        UUID toUserId,
        UUID skillId,
        String skillName,
        Integer rating,
        String feedback,
        Instant createdAt
) {}
