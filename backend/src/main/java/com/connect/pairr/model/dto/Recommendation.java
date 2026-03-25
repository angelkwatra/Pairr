package com.connect.pairr.model.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record Recommendation(
        UUID userId,
        String displayName,
        double score
) {}
