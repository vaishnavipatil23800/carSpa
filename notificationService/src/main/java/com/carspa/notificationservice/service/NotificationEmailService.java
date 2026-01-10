/**
 * NotificationEmailService.java — builds and sends HTML emails for every event.
 *
 * Email templates:
 *   booking.confirmed    → "Your wash is booked! 🚗"
 *   booking.cancelled    → "Your booking has been cancelled"
 *   booking.in_progress  → "Your car wash has started! 🧼"
 *   booking.done         → "Your car is sparkling clean! ✨"
 *   payment.success      → "Payment received — invoice attached (from payment-service)"
 *   payment.failed       → "Payment failed — please retry"
 *
 * All emails are @Async so RabbitMQ listener threads are never blocked by SMTP.
 * If SMTP is misconfigured, the exception is caught and logged — the queue
 * message is still acked so it doesn't loop.
 */
package com.carspa.notificationservice.service;

import com.carspa.notificationservice.model.BookingEvent;
import com.carspa.notificationservice.model.PaymentEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@carspa.com}")
    private String fromAddress;

    private static final DateTimeFormatter SLOT_FMT =
        DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mm");

    // ══════════════════════════════════════════════════
    //  BOOKING EMAILS
    // ══════════════════════════════════════════════════

    @Async
    public void sendBookingConfirmed(BookingEvent event) {
        String subject = "Your CarSpa booking is confirmed! 🚗 (#" + event.getBookingId() + ")";
        String slotStr = event.getSlotTime() != null ? event.getSlotTime().format(SLOT_FMT) : "TBD";
        String body = bookingEmailBase(event,
            "Booking Confirmed ✓",
            "#28a745",
            "Your car wash appointment is all set.",
            """
            <tr style="background:#f8f9fa">
              <td style="padding:10px;border:1px solid #dee2e6"><strong>Status</strong></td>
              <td style="padding:10px;border:1px solid #dee2e6;color:#28a745"><strong>CONFIRMED</strong></td>
            </tr>
            <tr>
              <td style="padding:10px;border:1px solid #dee2e6"><strong>Slot</strong></td>
              <td style="padding:10px;border:1px solid #dee2e6">""" + slotStr + """
              </td>
            </tr>
            """,
            "Please arrive 5 minutes before your slot time. See you soon!"
        );
        sendEmail(event.getUserEmail(), subject, body, "booking.confirmed");
    }

    @Async
    public void sendBookingCancelled(BookingEvent event) {
        String subject = "Your CarSpa booking has been cancelled (#" + event.getBookingId() + ")";
        String body = bookingEmailBase(event,
            "Booking Cancelled",
            "#dc3545",
            "We're sorry to see you go.",
            """
            <tr style="background:#f8f9fa">
              <td style="padding:10px;border:1px solid #dee2e6"><strong>Status</strong></td>
              <td style="padding:10px;border:1px solid #dee2e6;color:#dc3545"><strong>CANCELLED</strong></td>
            </tr>
            """,
            "You can book a new appointment anytime. We'd love to have you back!"
        );
        sendEmail(event.getUserEmail(), subject, body, "booking.cancelled");
    }

    @Async
    public void sendBookingInProgress(BookingEvent event) {
        String subject = "Your car wash has started! 🧼 (#" + event.getBookingId() + ")";
        String body = bookingEmailBase(event,
            "Wash In Progress 🧼",
            "#fd7e14",
            "Our team has started washing your vehicle.",
            """
            <tr style="background:#f8f9fa">
              <td style="padding:10px;border:1px solid #dee2e6"><strong>Status</strong></td>
              <td style="padding:10px;border:1px solid #dee2e6;color:#fd7e14"><strong>IN PROGRESS</strong></td>
            </tr>
            """,
            "Sit back and relax — we'll notify you when it's done!"
        );
        sendEmail(event.getUserEmail(), subject, body, "booking.in_progress");
    }

    @Async
    public void sendBookingDone(BookingEvent event) {
        String subject = "Your car is sparkling clean! ✨ (#" + event.getBookingId() + ")";
        String body = bookingEmailBase(event,
            "Wash Complete ✨",
            "#0066cc",
            "Your vehicle is ready for pickup.",
            """
            <tr style="background:#f8f9fa">
              <td style="padding:10px;border:1px solid #dee2e6"><strong>Status</strong></td>
              <td style="padding:10px;border:1px solid #dee2e6;color:#0066cc"><strong>DONE</strong></td>
            </tr>
            """,
            "Thank you for choosing CarSpa! We hope to see you again. 🚗"
        );
        sendEmail(event.getUserEmail(), subject, body, "booking.done");
    }

    // ══════════════════════════════════════════════════
    //  PAYMENT EMAILS
    // ══════════════════════════════════════════════════

    @Async
    public void sendPaymentSuccess(PaymentEvent event) {
        String subject = "Payment confirmed ₹" +
            (event.getTotalAmount() != null
                ? event.getTotalAmount().setScale(2, RoundingMode.HALF_UP)
                : "0.00") +
            " — CarSpa Invoice #PAY-" + event.getPaymentId();

        String amountStr = event.getTotalAmount() != null
            ? "₹" + event.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()
            : "N/A";

        String body = paymentEmailBase(event,
            "Payment Successful ✓",
            "#28a745",
            "We've received your payment.",
            """
            <tr style="background:#f8f9fa">
              <td style="padding:10px;border:1px solid #dee2e6"><strong>Status</strong></td>
              <td style="padding:10px;border:1px solid #dee2e6;color:#28a745"><strong>PAID</strong></td>
            </tr>
            <tr>
              <td style="padding:10px;border:1px solid #dee2e6"><strong>Amount Paid</strong></td>
              <td style="padding:10px;border:1px solid #dee2e6;font-size:18px;color:#0066cc"><strong>"""
                + amountStr + """
              </strong></td>
            </tr>
            <tr style="background:#f8f9fa">
              <td style="padding:10px;border:1px solid #dee2e6"><strong>Payment ID</strong></td>
              <td style="padding:10px;border:1px solid #dee2e6">"""
                + nvl(event.getRazorpayPaymentId()) + """
              </td>
            </tr>
            """,
            "Your detailed invoice has been sent separately by our payment service."
        );
        sendEmail(event.getUserEmail(), subject, body, "payment.success");
    }

    @Async
    public void sendPaymentFailed(PaymentEvent event) {
        String subject = "Payment failed — CarSpa Booking #" + event.getBookingId();
        String body = paymentEmailBase(event,
            "Payment Failed ✗",
            "#dc3545",
            "Unfortunately your payment could not be processed.",
            """
            <tr style="background:#f8f9fa">
              <td style="padding:10px;border:1px solid #dee2e6"><strong>Status</strong></td>
              <td style="padding:10px;border:1px solid #dee2e6;color:#dc3545"><strong>FAILED</strong></td>
            </tr>
            """,
            "Please try paying again from your dashboard. If the problem persists, contact support."
        );
        sendEmail(event.getUserEmail(), subject, body, "payment.failed");
    }

    // ══════════════════════════════════════════════════
    //  PRIVATE HELPERS — HTML template builders
    // ══════════════════════════════════════════════════

    private String bookingEmailBase(
        BookingEvent event,
        String heading,
        String headingColor,
        String subtitle,
        String extraRows,
        String footer
    ) {
        return """
            <html><body style="font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto">
              <div style="background:#0066cc;padding:24px;text-align:center">
                <h1 style="color:white;margin:0">CarSpa</h1>
                <p style="color:#cce5ff;margin:4px 0">On-Demand Car Wash</p>
              </div>
              <div style="padding:24px">
                <h2 style="color:%s">%s</h2>
                <p>Hi <strong>%s</strong>, %s</p>
                <table style="width:100%%;border-collapse:collapse;margin:16px 0">
                  <tr>
                    <td style="padding:10px;border:1px solid #dee2e6"><strong>Booking ID</strong></td>
                    <td style="padding:10px;border:1px solid #dee2e6">#%d</td>
                  </tr>
                  <tr style="background:#f8f9fa">
                    <td style="padding:10px;border:1px solid #dee2e6"><strong>Vehicle</strong></td>
                    <td style="padding:10px;border:1px solid #dee2e6">%s</td>
                  </tr>
                  <tr>
                    <td style="padding:10px;border:1px solid #dee2e6"><strong>Service</strong></td>
                    <td style="padding:10px;border:1px solid #dee2e6">%s</td>
                  </tr>
                  <tr style="background:#f8f9fa">
                    <td style="padding:10px;border:1px solid #dee2e6"><strong>Wash Centre</strong></td>
                    <td style="padding:10px;border:1px solid #dee2e6">%s</td>
                  </tr>
                  %s
                </table>
                <p style="color:#555">%s</p>
              </div>
              <div style="background:#f0f0f0;padding:12px;text-align:center;font-size:11px;color:#888">
                CarSpa India Pvt. Ltd. — Automated notification, please do not reply.
              </div>
            </body></html>
            """.formatted(
            headingColor, heading,
            nvl(event.getUserName()), subtitle,
            event.getBookingId(),
            nvl(event.getVehicleNumber()),
            nvl(event.getServiceType()),
            nvl(event.getWashCentre()),
            extraRows,
            footer
        );
    }

    private String paymentEmailBase(
        PaymentEvent event,
        String heading,
        String headingColor,
        String subtitle,
        String extraRows,
        String footer
    ) {
        return """
            <html><body style="font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto">
              <div style="background:#0066cc;padding:24px;text-align:center">
                <h1 style="color:white;margin:0">CarSpa</h1>
                <p style="color:#cce5ff;margin:4px 0">On-Demand Car Wash</p>
              </div>
              <div style="padding:24px">
                <h2 style="color:%s">%s</h2>
                <p>Hi <strong>%s</strong>, %s</p>
                <table style="width:100%%;border-collapse:collapse;margin:16px 0">
                  <tr>
                    <td style="padding:10px;border:1px solid #dee2e6"><strong>Payment ID</strong></td>
                    <td style="padding:10px;border:1px solid #dee2e6">#%d</td>
                  </tr>
                  <tr style="background:#f8f9fa">
                    <td style="padding:10px;border:1px solid #dee2e6"><strong>Booking ID</strong></td>
                    <td style="padding:10px;border:1px solid #dee2e6">#%d</td>
                  </tr>
                  <tr>
                    <td style="padding:10px;border:1px solid #dee2e6"><strong>Vehicle</strong></td>
                    <td style="padding:10px;border:1px solid #dee2e6">%s</td>
                  </tr>
                  <tr style="background:#f8f9fa">
                    <td style="padding:10px;border:1px solid #dee2e6"><strong>Service</strong></td>
                    <td style="padding:10px;border:1px solid #dee2e6">%s</td>
                  </tr>
                  %s
                </table>
                <p style="color:#555">%s</p>
              </div>
              <div style="background:#f0f0f0;padding:12px;text-align:center;font-size:11px;color:#888">
                CarSpa India Pvt. Ltd. — Automated notification, please do not reply.
              </div>
            </body></html>
            """.formatted(
            headingColor, heading,
            nvl(event.getUserName()), subtitle,
            event.getPaymentId(),
            event.getBookingId(),
            nvl(event.getVehicleNumber()),
            nvl(event.getServiceType()),
            extraRows,
            footer
        );
    }

    private void sendEmail(String to, String subject, String htmlBody, String eventType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent → {} [{}]", to, eventType);
        } catch (MessagingException e) {
            log.warn("Failed to send '{}' email to {}: {}", eventType, to, e.getMessage());
        } catch (Exception e) {
            log.warn("Email error for event '{}': {}", eventType, e.getMessage());
        }
    }

    private String nvl(String v) { return v != null ? v : "N/A"; }
}
