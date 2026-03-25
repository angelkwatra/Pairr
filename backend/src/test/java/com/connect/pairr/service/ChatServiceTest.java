package com.connect.pairr.service;

import com.connect.pairr.exception.ConversationNotFoundException;
import com.connect.pairr.exception.SelfMessageException;
import com.connect.pairr.exception.UserNotFoundException;
import com.connect.pairr.model.dto.ConversationResponse;
import com.connect.pairr.model.dto.MessageResponse;
import com.connect.pairr.model.dto.SendMessageRequest;
import com.connect.pairr.model.entity.Conversation;
import com.connect.pairr.model.entity.Message;
import com.connect.pairr.model.entity.User;
import com.connect.pairr.model.enums.Role;
import com.connect.pairr.repository.ConversationRepository;
import com.connect.pairr.repository.MessageRepository;
import com.connect.pairr.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ChatService chatService;

    private User sender;
    private User recipient;
    private UUID senderId;
    private UUID recipientId;

    @BeforeEach
    void setUp() {
        // Ensure sender UUID < recipient UUID for predictable ordering
        senderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        recipientId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        sender = User.builder().id(senderId).email("sender@test.com")
                .displayName("Sender").password("p").role(Role.USER).createdAt(Instant.now()).build();
        recipient = User.builder().id(recipientId).email("recipient@test.com")
                .displayName("Recipient").password("p").role(Role.USER).createdAt(Instant.now()).build();
    }

    @Test
    void sendMessage_createsNewConversation() {
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(conversationRepository.findByParticipant1IdAndParticipant2Id(senderId, recipientId))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(Instant.now());
            return c;
        });
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(Instant.now());
            return m;
        });

        SendMessageRequest request = new SendMessageRequest(recipientId, "Hello!");
        MessageResponse response = chatService.sendMessage(senderId, request);

        assertNotNull(response);
        assertEquals("Hello!", response.content());
        assertEquals(senderId, response.senderId());
    }

    @Test
    void sendMessage_reusesExistingConversation() {
        Conversation existing = Conversation.builder()
                .id(UUID.randomUUID()).participant1(sender).participant2(recipient)
                .createdAt(Instant.now()).build();

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(conversationRepository.findByParticipant1IdAndParticipant2Id(senderId, recipientId))
                .thenReturn(Optional.of(existing));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(Instant.now());
            return m;
        });
        when(conversationRepository.save(any(Conversation.class))).thenReturn(existing);

        chatService.sendMessage(senderId, new SendMessageRequest(recipientId, "Hi again"));

        // conversationRepository.save called only for lastMessageAt update, not for creation
        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertEquals(existing.getId(), captor.getValue().getId());
    }

    @Test
    void sendMessage_selfMessage_throws() {
        assertThrows(SelfMessageException.class,
                () -> chatService.sendMessage(senderId, new SendMessageRequest(senderId, "Hi")));
    }

    @Test
    void sendMessage_senderNotFound_throws() {
        when(userRepository.findById(senderId)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> chatService.sendMessage(senderId, new SendMessageRequest(recipientId, "Hi")));
    }

    @Test
    void sendMessage_recipientNotFound_throws() {
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(recipientId)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> chatService.sendMessage(senderId, new SendMessageRequest(recipientId, "Hi")));
    }

    @Test
    void sendMessage_uuidOrdering_smallerBecomesParticipant1() {
        // Use UUIDs where compareTo ordering is clear
        UUID idA = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID idB = UUID.fromString("00000000-0000-0000-0000-000000000009");
        // Verify our assumption: idA < idB
        assertTrue(idA.compareTo(idB) < 0);

        User userA = User.builder().id(idA).email("a@test.com")
                .displayName("A").password("p").role(Role.USER).createdAt(Instant.now()).build();
        User userB = User.builder().id(idB).email("b@test.com")
                .displayName("B").password("p").role(Role.USER).createdAt(Instant.now()).build();

        // Send from B (larger) to A (smaller) — participant1 should be A
        when(userRepository.findById(idB)).thenReturn(Optional.of(userB));
        when(userRepository.findById(idA)).thenReturn(Optional.of(userA));
        when(conversationRepository.findByParticipant1IdAndParticipant2Id(idA, idB))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(Instant.now());
            return c;
        });
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(Instant.now());
            return m;
        });

        chatService.sendMessage(idB, new SendMessageRequest(idA, "Hey"));

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository, atLeastOnce()).save(captor.capture());
        Conversation saved = captor.getAllValues().get(0);
        assertEquals(idA, saved.getParticipant1().getId());
        assertEquals(idB, saved.getParticipant2().getId());
    }

    @Test
    void getConversations_returnsConversationsWithPreviewAndUnreadCount() {
        Conversation conv = Conversation.builder()
                .id(UUID.randomUUID()).participant1(sender).participant2(recipient)
                .lastMessageAt(Instant.now()).createdAt(Instant.now()).build();

        Message lastMsg = Message.builder()
                .id(UUID.randomUUID()).conversation(conv).sender(recipient)
                .content("Last message").createdAt(Instant.now()).build();

        when(conversationRepository.findAllByParticipant(eq(senderId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(conv)));
        when(messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conv.getId()))
                .thenReturn(Optional.of(lastMsg));
        when(messageRepository.countByConversationIdAndSenderIdNotAndIsReadFalse(conv.getId(), senderId))
                .thenReturn(5L);

        Page<ConversationResponse> result = chatService.getConversations(senderId, PageRequest.of(0, 10));
        assertEquals(1, result.getContent().size());
        assertEquals(recipientId, result.getContent().get(0).otherUserId());
        assertEquals("Last message", result.getContent().get(0).lastMessage());
        assertEquals(5L, result.getContent().get(0).unreadCount());
    }

    @Test
    void getMessages_userIsParticipant_marksAsReadAndReturnsMessages() {
        UUID convId = UUID.randomUUID();
        Conversation conv = Conversation.builder()
                .id(convId).participant1(sender).participant2(recipient)
                .createdAt(Instant.now()).build();

        Message msg = Message.builder()
                .id(UUID.randomUUID()).conversation(conv).sender(recipient)
                .content("Hello").createdAt(Instant.now()).build();

        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));
        when(messageRepository.findAllByConversationId(eq(convId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(msg)));

        Page<MessageResponse> result = chatService.getMessages(senderId, convId, PageRequest.of(0, 50));
        
        verify(messageRepository).markAllAsRead(convId, senderId);
        assertEquals(1, result.getContent().size());
        assertEquals("Hello", result.getContent().get(0).content());
    }

    @Test
    void markMessagesAsRead_callsRepository() {
        UUID convId = UUID.randomUUID();
        chatService.markMessagesAsRead(senderId, convId);
        verify(messageRepository).markAllAsRead(convId, senderId);
    }

    @Test
    void getMessages_conversationNotFound_throws() {
        UUID convId = UUID.randomUUID();
        when(conversationRepository.findById(convId)).thenReturn(Optional.empty());
        assertThrows(ConversationNotFoundException.class,
                () -> chatService.getMessages(senderId, convId, PageRequest.of(0, 50)));
    }

    @Test
    void getMessages_userNotParticipant_throws() {
        UUID convId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        Conversation conv = Conversation.builder()
                .id(convId).participant1(sender).participant2(recipient)
                .createdAt(Instant.now()).build();

        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));
        assertThrows(ConversationNotFoundException.class,
                () -> chatService.getMessages(outsiderId, convId, PageRequest.of(0, 50)));
    }
}
