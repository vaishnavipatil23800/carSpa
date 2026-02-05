/**
 * ChatController.java
 *
 * POST /api/chat/user   — user support chatbot (any logged-in user)
 * POST /api/chat/admin  — admin BI chatbot (admin role only — enforced here)
 *
 * Both endpoints read user identity from X-User-* headers injected by the gateway.
 * No JWT parsing needed here.
 *
 * The admin endpoint checks X-User-Role — if it's not ROLE_ADMIN, returns 403.
 * This is a secondary check; the gateway should also enforce this via routing.
 */
package com.carspa.chatservice.controller;

import com.carspa.chatservice.dto.ChatDto;
import com.carspa.chatservice.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI-powered chat — user support + admin analytics")
public class ChatController {

    private final ChatService chatService;

    /**
     * User support chat.
     * Any authenticated user can call this.
     */
    @PostMapping("/user")
    @Operation(summary = "User support chatbot — ask about bookings, services, pricing")
    public ResponseEntity<ChatDto.ChatResponse> userChat(
        @Valid @RequestBody             ChatDto.ChatRequest request,
        @RequestHeader("X-User-Email")  String userEmail,
        @RequestHeader(value = "X-User-Name", required = false) String userName
    ) {
        ChatDto.ChatResponse response = chatService.chat(request, userEmail, userName);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin analytics chat.
     * Checks that the caller has ROLE_ADMIN — returns 403 otherwise.
     * Has access to live booking + revenue data for business intelligence queries.
     */
    @PostMapping("/admin")
    @Operation(summary = "Admin BI chatbot — query live booking stats and revenue")
    public ResponseEntity<ChatDto.ChatResponse> adminChat(
        @Valid @RequestBody             ChatDto.ChatRequest request,
        @RequestHeader("X-User-Email")  String userEmail,
        @RequestHeader("X-User-Role")   String userRole
    ) {
        // secondary role check (gateway is primary)
        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(403).build();
        }

        ChatDto.ChatResponse response = chatService.adminChat(request, userEmail);
        return ResponseEntity.ok(response);
    }
}
