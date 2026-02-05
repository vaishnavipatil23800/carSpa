/**
 * AdminDataService.java — fetches live business data and formats it
 * as a context string that gets injected into the admin AI's system prompt.
 *
 * This gives the AI real, up-to-date numbers so it can answer questions like:
 *   "How many bookings do we have today?"
 *   "What's our total revenue this month?"
 *   "Which service type is most popular?"
 *
 * Both Feign calls are wrapped in try-catch so if either service is down,
 * we just omit that section from the context rather than failing the chat.
 */
package com.carspa.chatservice.service;

import com.carspa.chatservice.client.BookingServiceClient;
import com.carspa.chatservice.client.PaymentServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDataService {

    private final BookingServiceClient bookingClient;
    private final PaymentServiceClient paymentClient;

    /**
     * Builds a plain-text context string with live stats.
     * This is injected into the admin system prompt so the AI
     * answers from real data, not hallucinated numbers.
     */
    public String buildAdminContext() {
        StringBuilder ctx = new StringBuilder();
        ctx.append("LIVE BUSINESS DATA (fetched at ")
           .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")))
           .append("):\n\n");

        // Booking stats
        try {
            Map<String, Object> bookingStats = bookingClient.getAdminStats();
            if (!bookingStats.isEmpty()) {
                ctx.append("=== BOOKINGS ===\n");
                ctx.append("Total bookings:      ").append(bookingStats.getOrDefault("total",      "N/A")).append("\n");
                ctx.append("Pending:             ").append(bookingStats.getOrDefault("pending",    "N/A")).append("\n");
                ctx.append("Confirmed:           ").append(bookingStats.getOrDefault("confirmed",  "N/A")).append("\n");
                ctx.append("In Progress:         ").append(bookingStats.getOrDefault("inProgress", "N/A")).append("\n");
                ctx.append("Done:                ").append(bookingStats.getOrDefault("done",       "N/A")).append("\n");
                ctx.append("Cancelled:           ").append(bookingStats.getOrDefault("cancelled",  "N/A")).append("\n");
                Object byType = bookingStats.get("byServiceType");
                if (byType != null) {
                    ctx.append("By service type:     ").append(byType).append("\n");
                }
                ctx.append("\n");
            } else {
                ctx.append("=== BOOKINGS === (data unavailable)\n\n");
            }
        } catch (Exception e) {
            log.warn("Could not fetch booking stats for admin context: {}", e.getMessage());
            ctx.append("=== BOOKINGS === (service unavailable)\n\n");
        }

        // Revenue stats
        try {
            Map<String, Object> revenueStats = paymentClient.getRevenueStats();
            if (!revenueStats.isEmpty()) {
                ctx.append("=== REVENUE ===\n");
                ctx.append("Total revenue (INR): ").append(revenueStats.getOrDefault("totalRevenue", "N/A")).append("\n");
                ctx.append("Successful payments: ").append(revenueStats.getOrDefault("successCount", "N/A")).append("\n");
                ctx.append("Failed payments:     ").append(revenueStats.getOrDefault("failedCount",  "N/A")).append("\n");
                Object byType = revenueStats.get("revenueByServiceType");
                if (byType != null) {
                    ctx.append("Revenue by type:     ").append(byType).append("\n");
                }
                ctx.append("\n");
            } else {
                ctx.append("=== REVENUE === (data unavailable)\n\n");
            }
        } catch (Exception e) {
            log.warn("Could not fetch revenue stats for admin context: {}", e.getMessage());
            ctx.append("=== REVENUE === (service unavailable)\n\n");
        }

        return ctx.toString();
    }
}
