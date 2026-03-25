package com.connect.pairr.controller;

import com.connect.pairr.model.dto.MessageResponse;
import com.connect.pairr.model.dto.SendMessageRequest;
import com.connect.pairr.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebSocketChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());

        MessageResponse response = chatService.sendMessage(senderId, request);

        // Send to recipient's personal queue
        messagingTemplate.convertAndSendToUser(
                request.recipientId().toString(),
                "/queue/messages",
                response
        );

        // Also send back to sender so their UI updates
        messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/queue/messages",
                response
        );
    }
}
