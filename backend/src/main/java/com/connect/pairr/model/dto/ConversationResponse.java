package com.connect.pairr.model.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ConversationResponse(
        UUID id,
        UUID otherUserId,
        String otherUserDisplayName,
        String lastMessage,
        Instant lastMessageAt,
        long unreadCount
) {}
