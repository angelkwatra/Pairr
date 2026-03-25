package com.connect.pairr.mapper;

import com.connect.pairr.model.dto.PairingSessionResponse;
import com.connect.pairr.model.entity.PairingSession;

public class PairingSessionMapper {

    public static PairingSessionResponse toResponse(PairingSession session, boolean ratedByCurrentUser) {
        return PairingSessionResponse.builder()
                .id(session.getId())
                .requesterId(session.getRequester().getId())
                .requesterDisplayName(session.getRequester().getDisplayName())
                .requesteeId(session.getRequestee().getId())
                .requesteeDisplayName(session.getRequestee().getDisplayName())
                .skillId(session.getSkill().getId())
                .skillName(session.getSkill().getName())
                .status(session.getStatus())
                .requestedAt(session.getRequestedAt())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .cancelledAt(session.getCancelledAt())
                .ratedByCurrentUser(ratedByCurrentUser)
                .build();
    }
}
