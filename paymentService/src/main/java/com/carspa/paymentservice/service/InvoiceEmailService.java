/**
 * InvoiceEmailService.java — sends the PDF invoice as an email attachment.
 *
 * Runs @Async so the /verify endpoint returns immediately
 * and email is delivered in the background.
 *
 * If SMTP is not configured, the exception is caught and logged —
 * the payment still succeeds, invoice is just not emailed.
 */
package com.carspa.paymentservice.service;

import com.carspa.paymentservice.model.Payment;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@carspa.com}")
    private String fromAddress;

    @Value("${payment.gst-rate:0.18}")
    private double gstRate;

    @Async
    public void sendInvoiceEmail(Payment payment, String invoicePath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(payment.getUserEmail());
            helper.setSubject("CarSpa Invoice #INV-" +
                String.format("%05d", payment.getId()) +
                " — Payment Confirmed");

            helper.setText(buildHtmlBody(payment), true);  // true = HTML

            // attach PDF
            File invoiceFile = new File(invoicePath);
            if (invoiceFile.exists()) {
                helper.addAttachment(
                    "CarSpa-Invoice-INV-" + String.format("%05d", payment.getId()) + ".pdf",
                    invoiceFile
                );
            }

            mailSender.send(message);
            log.info("Invoice email sent to {}", payment.getUserEmail());

        } catch (MessagingException e) {
            log.warn("Could not send invoice email to {}: {}", payment.getUserEmail(), e.getMessage());
        } catch (Exception e) {
            log.warn("Invoice email error: {}", e.getMessage());
        }
    }

    private String buildHtmlBody(Payment payment) {
        String total = payment.getTotalAmount() != null
            ? "₹" + payment.getTotalAmount().setScale(2, RoundingMode.HALF_UP)
            : "N/A";

        String gstPct = (int)(gstRate * 100) + "%";

        return """
            <html><body style="font-family:Arial,sans-serif;color:#333;max-width:600px;margin:auto">
              <div style="background:#0066cc;padding:24px;text-align:center">
                <h1 style="color:white;margin:0">CarSpa</h1>
                <p style="color:#cce5ff;margin:4px 0">On-Demand Car Wash</p>
              </div>
              <div style="padding:24px">
                <h2 style="color:#0066cc">Payment Confirmed ✓</h2>
                <p>Hi <strong>%s</strong>, your payment has been received successfully.</p>
                <table style="width:100%%;border-collapse:collapse;margin:16px 0">
                  <tr style="background:#f5f5f5">
                    <td style="padding:10px;border:1px solid #ddd"><strong>Invoice</strong></td>
                    <td style="padding:10px;border:1px solid #ddd">INV-%05d</td>
                  </tr>
                  <tr>
                    <td style="padding:10px;border:1px solid #ddd"><strong>Vehicle</strong></td>
                    <td style="padding:10px;border:1px solid #ddd">%s</td>
                  </tr>
                  <tr style="background:#f5f5f5">
                    <td style="padding:10px;border:1px solid #ddd"><strong>Service</strong></td>
                    <td style="padding:10px;border:1px solid #ddd">%s</td>
                  </tr>
                  <tr>
                    <td style="padding:10px;border:1px solid #ddd"><strong>Wash Centre</strong></td>
                    <td style="padding:10px;border:1px solid #ddd">%s</td>
                  </tr>
                  <tr style="background:#f5f5f5">
                    <td style="padding:10px;border:1px solid #ddd"><strong>GST (%s)</strong></td>
                    <td style="padding:10px;border:1px solid #ddd">₹%s</td>
                  </tr>
                  <tr>
                    <td style="padding:10px;border:1px solid #ddd"><strong>Total Paid</strong></td>
                    <td style="padding:10px;border:1px solid #ddd;font-size:18px;color:#0066cc"><strong>%s</strong></td>
                  </tr>
                </table>
                <p>Your invoice PDF is attached to this email.</p>
                <p style="color:#666;font-size:12px">Thank you for choosing CarSpa! 🚗</p>
              </div>
              <div style="background:#f0f0f0;padding:12px;text-align:center;font-size:11px;color:#888">
                CarSpa India Pvt. Ltd. — This is an automated email, please do not reply.
              </div>
            </body></html>
            """.formatted(
            payment.getUserName() != null ? payment.getUserName() : payment.getUserEmail(),
            payment.getId(),
            nvl(payment.getVehicleNumber()),
            nvl(payment.getServiceType()),
            nvl(payment.getWashCentre()),
            gstPct,
            payment.getGstAmount() != null
                ? payment.getGstAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()
                : "0.00",
            total
        );
    }

    private String nvl(String v) { return v != null ? v : "N/A"; }
}
