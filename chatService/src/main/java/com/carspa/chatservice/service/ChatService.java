/**
 * ChatService.java — the core AI chat logic.
 *
 * Two modes, two system prompts:
 * ────────────────────────────────────────────────────────────────
 * USER SYSTEM PROMPT:
 *   Friendly support assistant. Knows CarSpa's services and pricing.
 *   Can explain how to book, cancel, check status. Stays on topic.
 *   Refers complex issues to human support.
 *
 * ADMIN SYSTEM PROMPT:
 *   Business intelligence assistant. Receives LIVE booking + revenue
 *   data injected at call time from booking-service and payment-service.
 *   Can answer analytics questions with real numbers.
 * ────────────────────────────────────────────────────────────────
 *
 * HOW CONVERSATION HISTORY WORKS:
 *   1. Frontend sends { message: "...", history: [{role, content}, ...] }
 *   2. We build: [systemMessage, ...history, newUserMessage]
 *   3. Send full array to OpenAI
 *   4. Return reply to frontend
 *   5. Frontend appends both the user message and assistant reply to its history
 *   6. Next request includes the updated history
 *
 * The service itself stores nothing — no sessions, no DB.
 */
package com.carspa.chatservice.service;

import com.carspa.chatservice.dto.ChatDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChatService {

    private final WebClient       webClient;
    private final AdminDataService adminDataService;

    @Value("${openai.api-key}")    private String apiKey;
    @Value("${openai.api-url}")    private String apiUrl;
    @Value("${openai.model}")      private String model;
    @Value("${openai.max-tokens}") private int    maxTokens;
    @Value("${openai.temperature}")private double temperature;
    @Value("${chat.history-pairs:6}") private int historyPairs;

    public ChatService(WebClient openAiWebClient, AdminDataService adminDataService) {
        this.webClient        = openAiWebClient;
        this.adminDataService = adminDataService;
    }

    // ── User chat ──

    public ChatDto.ChatResponse chat(ChatDto.ChatRequest request, String userEmail, String userName) {
        log.debug("User chat from {} — message length: {}", userEmail, request.getMessage().length());

        List<ChatDto.OpenAiMessage> messages = buildMessages(
            userSystemPrompt(userName),
            request.getHistory(),
            request.getMessage()
        );

        return callOpenAi(messages, false);
    }

    // ── Admin chat ──

    public ChatDto.ChatResponse adminChat(ChatDto.ChatRequest request, String adminEmail) {
        log.debug("Admin chat from {} — fetching live business context", adminEmail);

        // fetch live data — this is what makes the admin bot useful
        String liveContext = adminDataService.buildAdminContext();

        List<ChatDto.OpenAiMessage> messages = buildMessages(
            adminSystemPrompt(liveContext),
            request.getHistory(),
            request.getMessage()
        );

        return callOpenAi(messages, true);
    }

    // ── private: build message array ──

    private List<ChatDto.OpenAiMessage> buildMessages(
        String systemPrompt,
        List<ChatDto.HistoryMessage> history,
        String newMessage
    ) {
        List<ChatDto.OpenAiMessage> messages = new ArrayList<>();

        // 1. System prompt always goes first
        messages.add(ChatDto.OpenAiMessage.builder()
            .role("system")
            .content(systemPrompt)
            .build());

        // 2. Include last N pairs of conversation history
        if (history != null && !history.isEmpty()) {
            // take last historyPairs*2 messages (each pair = 1 user + 1 assistant)
            int startIdx = Math.max(0, history.size() - (historyPairs * 2));
            history.subList(startIdx, history.size()).forEach(h ->
                messages.add(ChatDto.OpenAiMessage.builder()
                    .role(h.getRole())
                    .content(h.getContent())
                    .build())
            );
        }

        // 3. New user message at the end
        messages.add(ChatDto.OpenAiMessage.builder()
            .role("user")
            .content(newMessage)
            .build());

        return messages;
    }

    // ── private: call OpenAI ──

    private ChatDto.ChatResponse callOpenAi(List<ChatDto.OpenAiMessage> messages, boolean adminMode) {
        ChatDto.OpenAiRequest requestBody = ChatDto.OpenAiRequest.builder()
            .model(model)
            .messages(messages)
            .max_tokens(maxTokens)
            .temperature(temperature)
            .build();

        try {
            ChatDto.OpenAiResponse response = webClient.post()
                .uri(apiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ChatDto.OpenAiResponse.class)
                .block();   // blocking is fine — we're in WebMVC not WebFlux

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                log.warn("OpenAI returned empty response");
                return fallbackResponse(adminMode);
            }

            String reply      = response.getChoices().get(0).getMessage().getContent();
            int    tokensUsed = response.getUsage() != null ? response.getUsage().getTotal_tokens() : 0;

            log.debug("OpenAI reply — {} tokens used", tokensUsed);

            return ChatDto.ChatResponse.builder()
                .reply(reply)
                .role("assistant")
                .tokensUsed(tokensUsed)
                .adminMode(adminMode)
                .build();

        } catch (WebClientResponseException e) {
            log.error("OpenAI API error: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode().value() == 401) {
                throw new RuntimeException("Invalid OpenAI API key. Check application.properties.");
            }
            if (e.getStatusCode().value() == 429) {
                throw new RuntimeException("OpenAI rate limit reached. Wait a moment and try again.");
            }
            throw new RuntimeException("OpenAI API error: " + e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error calling OpenAI: {}", e.getMessage());
            throw new RuntimeException("Chat service error: " + e.getMessage());
        }
    }

    // ── private: system prompts ──

    private String userSystemPrompt(String userName) {
        String name = (userName != null && !userName.isBlank()) ? userName : "there";
        return """
            You are CarBot, the friendly AI assistant for CarSpa — an on-demand car wash booking service.
            
            You are speaking with %s.
            
            YOUR ROLE:
            - Help users with booking questions, service information, and account support
            - Be warm, concise, and helpful
            - Always stay on the topic of CarSpa services
            
            CARSPA SERVICES & PRICING (approximate):
            - BASIC Wash: exterior wash, wheel cleaning. ~₹249–₹299
            - PREMIUM Wash: exterior + interior, dashboard wipe, glass cleaning. ~₹449–₹549
            - FULL DETAIL: full service, wax polish, deep interior clean. ~₹799–₹1199
            
            HOW TO BOOK:
            - Register / Login → Dashboard → "Book a Wash" → pick vehicle, service, centre, slot time
            - Bookings can be cancelled until the wash starts (status: CONFIRMED)
            
            BOOKING STATUSES:
            CONFIRMED → IN_PROGRESS → DONE (or CANCELLED)
            
            PAYMENT:
            - Payments via Razorpay (card, UPI, net banking)
            - PDF invoice emailed after successful payment
            - GST (18%) is applied on all services
            
            RULES:
            - Do NOT invent pricing or features that don't exist above
            - For complex complaints, say: "Please contact our support team for this."
            - Keep replies short — 2–4 sentences unless a detailed explanation is needed
            - Do NOT answer questions unrelated to CarSpa
            """.formatted(name);
    }

    private String adminSystemPrompt(String liveContext) {
        return """
            You are CarSpa Admin Assistant — an AI business intelligence tool for CarSpa administrators.
            
            YOUR ROLE:
            - Answer questions about business performance using the live data provided below
            - Provide insights, trends, and actionable recommendations
            - Be professional, precise, and data-driven
            
            %s
            
            HOW TO USE THIS DATA:
            - When asked about bookings, revenue, or service popularity — use the numbers above
            - If the data shows "unavailable", say: "The data could not be fetched right now."
            - You can calculate percentages, compare service types, and identify trends
            - Suggest operational improvements based on the data
            
            EXAMPLES OF QUESTIONS YOU CAN ANSWER:
            - "How many bookings do we have in total?"
            - "What is our conversion rate (confirmed vs cancelled)?"
            - "Which service type generates the most revenue?"
            - "How many payments have failed?"
            - "What is our average revenue per booking?"
            
            Keep answers concise and numbers-focused.
            """.formatted(liveContext);
    }

    // ── private: fallback when OpenAI returns empty ──

    private ChatDto.ChatResponse fallbackResponse(boolean adminMode) {
        return ChatDto.ChatResponse.builder()
            .reply("I'm sorry, I couldn't process that right now. Please try again in a moment.")
            .role("assistant")
            .tokensUsed(0)
            .adminMode(adminMode)
            .build();
    }
}
