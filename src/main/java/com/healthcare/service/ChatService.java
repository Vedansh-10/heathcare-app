package com.healthcare.service;

import com.healthcare.ai.AiService;
import com.healthcare.dto.request.ChatRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.entity.Chat;
import com.healthcare.entity.User;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.ChatRepository;
import com.healthcare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final EmergencyDetectionService emergencyDetectionService;

    private static final int CONTEXT_WINDOW_SIZE = 10; // last N messages for AI context

    @Transactional
    public ApiResponse.ChatResponse sendMessage(ChatRequest.SendMessageRequest request, String userEmail) {
        User user = findUserByEmail(userEmail);

        // Sanitize input
        String sanitizedMessage = sanitize(request.message());

        // Emergency detection
        EmergencyDetectionService.EmergencyResult emergency = emergencyDetectionService.detect(sanitizedMessage);

        // Build conversation history for context
        String sessionId = request.sessionId();
        String conversationHistory = buildConversationHistory(user.getId(), sessionId);

        // Language: use request language, fall back to user preference
        String language = request.language() != null ? request.language() : user.getLanguage();

        // Get AI response with context
        String aiResponse = aiService.chat(sanitizedMessage, conversationHistory, language);

        // Prepend emergency warning if needed
        String finalResponse = emergency.isEmergency()
                ? emergency.emergencyMessage() + "\n\n" + aiResponse
                : aiResponse;

        Chat chat = Chat.builder()
                .user(user)
                .message(sanitizedMessage)
                .response(finalResponse)
                .isEmergency(emergency.isEmergency())
                .sessionId(sessionId)
                .build();

        chat = chatRepository.save(chat);
        log.debug("Chat saved for user [{}], emergency={}", userEmail, emergency.isEmergency());

        return mapToChatResponse(chat, emergency.isEmergency() ? emergency.emergencyMessage() : null);
    }

    @Transactional(readOnly = true)
    public ApiResponse.PagedResponse<ApiResponse.ChatResponse> getChatHistory(String userEmail, Pageable pageable) {
        User user = findUserByEmail(userEmail);
        Page<Chat> chatPage = chatRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        List<ApiResponse.ChatResponse> responses = chatPage.getContent().stream()
                .map(c -> mapToChatResponse(c, null))
                .toList();
        return new ApiResponse.PagedResponse<>(
                responses, chatPage.getNumber(), chatPage.getSize(),
                chatPage.getTotalElements(), chatPage.getTotalPages(), chatPage.isLast());
    }

    @Transactional
    public void deleteChat(UUID chatId, String userEmail) {
        User user = findUserByEmail(userEmail);
        Chat chat = chatRepository.findByIdAndUserId(chatId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat", "id", chatId));
        chatRepository.delete(chat);
        log.info("Chat [{}] deleted by user [{}]", chatId, userEmail);
    }

    @Transactional
    public void deleteAllChats(String userEmail) {
        User user = findUserByEmail(userEmail);
        chatRepository.deleteAllByUserId(user.getId());
        log.info("All chats cleared for user [{}]", userEmail);
    }

    private String buildConversationHistory(UUID userId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            // No session — use recent messages as context
            List<Chat> recent = chatRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
            if (recent.isEmpty()) return null;
            return recent.reversed().stream()
                    .map(c -> "User: " + c.getMessage() + "\nAssistant: " + c.getResponse())
                    .collect(Collectors.joining("\n\n"));
        }
        // Session-specific history
        List<Chat> sessionChats = chatRepository.findByUserIdAndSessionId(userId, sessionId);
        if (sessionChats.isEmpty()) return null;
        return sessionChats.stream()
                .limit(CONTEXT_WINDOW_SIZE)
                .map(c -> "User: " + c.getMessage() + "\nAssistant: " + c.getResponse())
                .collect(Collectors.joining("\n\n"));
    }

    private String sanitize(String input) {
        if (input == null) return "";
        // Remove potential prompt injection patterns
        return input.replaceAll("(?i)(ignore (previous|all) instructions?|system prompt|you are now)", "[filtered]")
                    .trim();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private ApiResponse.ChatResponse mapToChatResponse(Chat chat, String emergencyMessage) {
        return new ApiResponse.ChatResponse(
                chat.getId(), chat.getMessage(), chat.getResponse(),
                chat.getIsEmergency(), emergencyMessage, chat.getSessionId(), chat.getCreatedAt());
    }
}
