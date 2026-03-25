package com.connect.pairr.model.dto;

import com.connect.pairr.model.enums.PairingStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record PairingSessionResponse(
        UUID id,
        UUID requesterId,
        String requesterDisplayName,
        UUID requesteeId,
        String requesteeDisplayName,
        UUID skillId,
        String skillName,
        PairingStatus status,
        Instant requestedAt,
        Instant startedAt,
        Instant completedAt,
        Instant cancelledAt,
        boolean ratedByCurrentUser
) {}
