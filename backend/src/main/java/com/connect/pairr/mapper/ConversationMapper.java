package com.connect.pairr.mapper;

import com.connect.pairr.model.dto.ConversationResponse;
import com.connect.pairr.model.entity.Conversation;
import com.connect.pairr.model.entity.Message;
import com.connect.pairr.model.entity.User;

import java.util.UUID;

public class ConversationMapper {

    private static final int LAST_MESSAGE_PREVIEW_LENGTH = 100;

    public static ConversationResponse toResponse(Conversation conversation, UUID currentUserId, Message lastMessage, long unreadCount) {
        User otherUser = conversation.getParticipant1().getId().equals(currentUserId)
                ? conversation.getParticipant2()
                : conversation.getParticipant1();

        String preview = null;
        if (lastMessage != null) {
            String content = lastMessage.getContent();
            preview = content.length() > LAST_MESSAGE_PREVIEW_LENGTH
                    ? content.substring(0, LAST_MESSAGE_PREVIEW_LENGTH) + "..."
                    : content;
        }

        return ConversationResponse.builder()
                .id(conversation.getId())
                .otherUserId(otherUser.getId())
                .otherUserDisplayName(otherUser.getDisplayName())
                .lastMessage(preview)
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(unreadCount)
                .build();
    }
}
