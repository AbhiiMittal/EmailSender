package com.example.service;

import com.example.helper.ExcelEmailHandler;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    // Consider injecting this from application.properties using @Value("${app.email.sender}")
    String sendingEmail = "abhishek@accountsandtax.xyz";

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    ExcelEmailHandler excelEmailHandler;

    @Transactional
    public ResponseEntity<?> sendEmail(String email, String subject, String body) {
        // INFO is perfect for tracking the start of the process
        log.info("Preparing to send email to: {} | Subject: {}", email, subject);

        // DEBUG: Logging the full body can consume massive disk space for 1M emails.
        // Logging the length is safer, or you can log the full body only on TRACE level.
        log.debug("Email Body length: {} characters", body != null ? body.length() : 0);

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true); // true = multipart message

            helper.setTo(email); // FIXED: Changed from 'receivingEmail' to the method parameter
            helper.setFrom(sendingEmail);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML format
            long startTime = System.currentTimeMillis();
            Thread.sleep(3000);
            javaMailSender.send(message);
            long endTime = System.currentTimeMillis();
            System.out.println(endTime-startTime);

            log.info("Successfully dispatched email to: {}", email);
            return ResponseEntity.ok("sent successfully");

        } catch (Exception e) {
            // ERROR captures the exact reason SES or your SMTP server rejected it
            log.error("Failed to send email to: {}. Error: {}", email, e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }
}

