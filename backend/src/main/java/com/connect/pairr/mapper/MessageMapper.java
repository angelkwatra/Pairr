package com.connect.pairr.mapper;

import com.connect.pairr.model.dto.MessageResponse;
import com.connect.pairr.model.entity.Conversation;
import com.connect.pairr.model.entity.Message;
import com.connect.pairr.model.entity.User;

public class MessageMapper {

    public static MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderDisplayName(message.getSender().getDisplayName())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public static Message toEntity(String content, Conversation conversation, User sender) {
        return Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .build();
    }
}
