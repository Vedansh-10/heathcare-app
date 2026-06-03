package com.healthcare.websocket;

import com.healthcare.dto.request.ChatRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    /**
     * Handles real-time chat messages sent via WebSocket.
     * Client sends to /app/chat, receives response at /user/queue/chat-response
     */
    @MessageMapping("/chat")
    public void handleChatMessage(
            @Payload ChatRequest.SendMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {

        if (principal == null) {
            log.warn("Unauthenticated WebSocket message attempt");
            return;
        }

        String userEmail = principal.getName();
        log.debug("WebSocket message from [{}]", userEmail);

        try {
            ApiResponse.ChatResponse response = chatService.sendMessage(request, userEmail);
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/chat-response", response);
        } catch (Exception e) {
            log.error("WebSocket chat processing failed for [{}]: {}", userEmail, e.getMessage(), e);
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/errors",
                    ApiResponse.ErrorResponse.of("Failed to process your message. Please try again."));
        }
    }
}
