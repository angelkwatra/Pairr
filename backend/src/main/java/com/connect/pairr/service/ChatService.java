package com.connect.pairr.service;

import com.connect.pairr.exception.ConversationNotFoundException;
import com.connect.pairr.exception.SelfMessageException;
import com.connect.pairr.exception.UserNotFoundException;
import com.connect.pairr.mapper.ConversationMapper;
import com.connect.pairr.mapper.MessageMapper;
import com.connect.pairr.model.dto.ConversationResponse;
import com.connect.pairr.model.dto.MessageResponse;
import com.connect.pairr.model.dto.SendMessageRequest;
import com.connect.pairr.model.entity.Conversation;
import com.connect.pairr.model.entity.Message;
import com.connect.pairr.model.entity.User;
import com.connect.pairr.repository.ConversationRepository;
import com.connect.pairr.repository.MessageRepository;
import com.connect.pairr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public MessageResponse sendMessage(UUID senderId, SendMessageRequest request) {

        if (senderId.equals(request.recipientId())) {
            throw new SelfMessageException();
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException(senderId));

        User recipient = userRepository.findById(request.recipientId())
                .orElseThrow(() -> new UserNotFoundException(request.recipientId()));

        Conversation conversation = findOrCreateConversation(sender, recipient);

        Message message = MessageMapper.toEntity(request.content(), conversation, sender);
        message = messageRepository.save(message);

        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        return MessageMapper.toResponse(message);
    }

    @Transactional(readOnly = true)
    public Page<ConversationResponse> getConversations(UUID userId, Pageable pageable) {
        // Ensure sorting by lastMessageAt DESC if not provided
        Sort sort = Sort.by(Sort.Direction.DESC, "lastMessageAt");
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        
        Page<Conversation> conversations = conversationRepository.findAllByParticipant(userId, sortedPageable);

        return conversations.map(conv -> {
            Message lastMessage = messageRepository
                    .findFirstByConversationIdOrderByCreatedAtDesc(conv.getId())
                    .orElse(null);
            
            long unreadCount = messageRepository
                    .countByConversationIdAndSenderIdNotAndIsReadFalse(conv.getId(), userId);
            
            return ConversationMapper.toResponse(conv, userId, lastMessage, unreadCount);
        });
    }

    @Transactional
    public void markMessagesAsRead(UUID userId, UUID conversationId) {
        messageRepository.markAllAsRead(conversationId, userId);
    }

    @Transactional
    public Page<MessageResponse> getMessages(UUID userId, UUID conversationId, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        boolean isParticipant = conversation.getParticipant1().getId().equals(userId)
                || conversation.getParticipant2().getId().equals(userId);

        if (!isParticipant) {
            throw new ConversationNotFoundException(conversationId);
        }

        // Mark as read when viewing history
        messageRepository.markAllAsRead(conversationId, userId);

        // Fetch newest first for pagination
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<Message> messagePage = messageRepository.findAllByConversationId(conversationId, sortedPageable);
        
        // Reverse them so they are chronological for the frontend
        List<Message> messages = new ArrayList<>(messagePage.getContent());
        Collections.reverse(messages);

        List<MessageResponse> content = messages.stream()
                .map(MessageMapper::toResponse)
                .toList();

        return new PageImpl<>(content, pageable, messagePage.getTotalElements());
    }

    private Conversation findOrCreateConversation(User user1, User user2) {
        // Sort UUIDs to ensure consistent ordering in the unique constraint
        UUID id1 = user1.getId();
        UUID id2 = user2.getId();
        User participant1 = id1.compareTo(id2) < 0 ? user1 : user2;
        User participant2 = id1.compareTo(id2) < 0 ? user2 : user1;

        return conversationRepository
                .findByParticipant1IdAndParticipant2Id(participant1.getId(), participant2.getId())
                .orElseGet(() -> {
                    Conversation conversation = Conversation.builder()
                            .participant1(participant1)
                            .participant2(participant2)
                            .build();
                    return conversationRepository.save(conversation);
                });
    }
}
