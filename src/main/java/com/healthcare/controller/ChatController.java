package com.healthcare.controller;

import com.healthcare.dto.request.ChatRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI-powered healthcare conversational chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/message")
    @Operation(summary = "Send a message to the AI healthcare assistant")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.ChatResponse>> sendMessage(
            @Valid @RequestBody ChatRequest.SendMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Message sent",
                chatService.sendMessage(request, userDetails.getUsername())));
    }

    @GetMapping("/history")
    @Operation(summary = "Get paginated chat history")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.PagedResponse<ApiResponse.ChatResponse>>> getChatHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Chat history retrieved",
                chatService.getChatHistory(userDetails.getUsername(), pageable)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a specific chat message")
    public ResponseEntity<ApiResponse.SuccessResponse<Void>> deleteChat(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        chatService.deleteChat(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Chat deleted", null));
    }

    @DeleteMapping("/history/all")
    @Operation(summary = "Clear all chat history for current user")
    public ResponseEntity<ApiResponse.SuccessResponse<Void>> clearChatHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        chatService.deleteAllChats(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Chat history cleared", null));
    }
}
