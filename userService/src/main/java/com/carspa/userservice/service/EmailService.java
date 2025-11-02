/**
 * EmailService.java — sends a welcome email after registration.
 * @Async so the register API response isn't delayed by SMTP.
 * If mail config is empty, the error is caught and logged — won't break registration.
 */
package com.carspa.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Welcome to CarSpa! 🚗");
            message.setText(
                "Hi " + fullName + ",\n\n" +
                "Welcome to CarSpa — your on-demand car wash service!\n\n" +
                "You can now book a wash, track your bookings, and manage your vehicles " +
                "from your dashboard.\n\n" +
                "Happy driving,\n" +
                "The CarSpa Team"
            );
            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);
        } catch (Exception e) {
            // don't let a mail failure affect the registration response
            log.warn("Could not send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }
}
