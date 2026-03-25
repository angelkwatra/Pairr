package com.connect.pairr.controller;

import com.connect.pairr.model.dto.ConversationResponse;
import com.connect.pairr.model.dto.MessageResponse;
import com.connect.pairr.model.dto.SendMessageRequest;
import com.connect.pairr.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "1:1 messaging — send messages, list conversations, view history")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/messages")
    @Operation(summary = "Send a message", description = "Sends a message to another user. Creates a conversation if one doesn't exist.")
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid SendMessageRequest request
    ) {
        return ResponseEntity.ok(chatService.sendMessage(userId, request));
    }

    @GetMapping("/conversations")
    @Operation(summary = "List your conversations", description = "Returns paginated conversations with last message preview")
    public ResponseEntity<Page<ConversationResponse>> getConversations(
            @AuthenticationPrincipal UUID userId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(chatService.getConversations(userId, pageable));
    }

    @PostMapping("/conversations/{conversationId}/read")
    @Operation(summary = "Mark conversation as read", description = "Marks all messages from the other participant as read.")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID conversationId
    ) {
        chatService.markMessagesAsRead(userId, conversationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Get message history", description = "Returns paginated messages in a conversation, ordered by time. Automatically marks messages as read.")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID conversationId,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable
    ) {
        return ResponseEntity.ok(chatService.getMessages(userId, conversationId, pageable));
    }
}
