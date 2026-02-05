/**
 * ChatDto.java — shapes for the chat API.
 *
 * Conversation history design:
 *   The service is stateless. The frontend sends the last N message pairs
 *   with every request. We rebuild the OpenAI messages array from this history
 *   on each call. This avoids server-side session storage and works across
 *   page refreshes.
 *
 *   Frontend stores history in memory (React state). On page reload, history
 *   resets — which is fine for a support chat widget.
 */
package com.carspa.chatservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class ChatDto {

    // ── Inbound ──

    @Getter @Setter
    public static class ChatRequest {

        @NotBlank(message = "Message cannot be empty")
        @Size(max = 1000, message = "Message too long (max 1000 chars)")
        private String message;

        /**
         * Conversation history — list of {role, content} pairs from previous turns.
         * role is "user" or "assistant".
         * Frontend sends the last N pairs; we prepend them before the new message.
         * Can be null or empty for the first message in a conversation.
         */
        private List<HistoryMessage> history;
    }

    @Getter @Setter
    public static class HistoryMessage {
        private String role;     // "user" | "assistant"
        private String content;  // the message text
    }

    // ── Outbound ──

    @Getter @Builder
    public static class ChatResponse {
        private String reply;       // the assistant's response text
        private String role;        // always "assistant"
        private int    tokensUsed;  // for cost monitoring in dev
        private boolean adminMode;  // true if called via /admin endpoint
    }

    // ── Internal: OpenAI API request shape ──

    @Getter @Builder
    public static class OpenAiRequest {
        private String              model;
        private List<OpenAiMessage> messages;
        private int                 max_tokens;
        private double              temperature;
    }

    @Getter
    @Builder
    public static class OpenAiMessage {
        private String role;     // "system" | "user" | "assistant"
        private String content;
    }

    // ── Internal: OpenAI API response shape ──

    @Getter @Setter
    public static class OpenAiResponse {
        private List<Choice> choices;
        private Usage        usage;

        @Getter @Setter
        public static class Choice {
            private OpenAiMessage message;
        }

        @Getter @Setter
        public static class Usage {
            private int total_tokens;
        }
    }
}
