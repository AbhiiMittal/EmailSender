package com.example.service;

import com.example.entity.EmailEntity;
import com.example.entity.MailMQDTO;
import com.example.repository.EmailDetailsRepo;
import com.example.repository.EmailEntityRepo;
import com.google.common.util.concurrent.RateLimiter;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Service
public class MailMQImpl implements MailMQ {

    private final RateLimiter rateLimiter = RateLimiter.create(1.0);

    @Autowired
    EmailService emailService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    EmailEntityRepo emailEntityRepo;

    @Autowired
    EmailDetailsRepo emailDetailsRepo;

    private static final int MAX_RETRIES = 3;

    @Override
    @RabbitListener(queues = "mail-queue", containerFactory = "rabbitListenerContainerFactory")
    public void processEmailQueue(MailMQDTO mailMQDTO, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        Long emailId = mailMQDTO.getEmailId();
        String toAddress = mailMQDTO.getEmail();

        log.debug("Picked up message from queue for Email ID: {} | To: {}", emailId, toAddress);

        // 1. Fetch the exact record from the DB
        EmailEntity entity = emailEntityRepo.findById(emailId).orElse(null);

        if (entity == null) {
            log.warn("EmailEntity not found in DB for Email ID: {}. It may have been deleted. Discarding message.", emailId);
            channel.basicAck(tag, false);
            return;
        }

        String subject = entity.getEmailJob().getEmailDetails().getSubject();
        String rawHtml = entity.getEmailJob().getEmailDetails().getBody();

// Your server's public domain (e.g., https://api.yourdomain.com)
        String serverUrl = "http://localhost:8080";

        String trackingPixelUrl = serverUrl + "/api/tracking/open/" + emailId + "/pixel.gif";
        String unsubscribeUrl = serverUrl + "/api/emails/unsubscribe?email=" + toAddress;

// Append the pixel and unsubscribe link to the bottom of whatever HTML the user uploaded
        String finalizedHtml = rawHtml +
                "<br><br><p style='font-size: 10px; color: #999;'><a href='" + unsubscribeUrl + "'>Unsubscribe</a></p>" +
                "<img src='" + trackingPixelUrl + "' width='1' height='1' alt='' />";

        try {
            log.info("Attempting to send Email ID: {} | To: {}", emailId, toAddress);

            rateLimiter.acquire();

            // 2. Attempt to send
            ResponseEntity<?> response = emailService.sendEmail(
                    toAddress,
                    subject,
                    finalizedHtml
            );

            // 3. Success! Update DB and Ack message
            if (response.getStatusCode().is2xxSuccessful()) {
                entity.setStatus("SENT");
                entity.setSentAt(LocalDateTime.now());
                emailEntityRepo.save(entity);

                channel.basicAck(tag, false);
                log.info("Successfully sent and acknowledged Email ID: {}", emailId);
            } else {
                // Throwing an exception here pushes it immediately to the catch block for retry logic
                throw new RuntimeException("Email API returned status code: " + response.getStatusCode());
            }

        }catch (MailException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "";

            // 1. FATAL ERRORS (Do NOT retry)
            // If the SMTP server returns a 5xx code (like 554 Message rejected or 501 Invalid format)
            if (errorMessage.contains("554") || errorMessage.contains("501") || errorMessage.contains("Message rejected")) {

                log.error("FATAL SMTP Error for Email ID {}: {}. Dropping message.", emailId, errorMessage);

                entity.setStatus("FAILED");
                entity.setErrorMessage("FATAL : " + e.getMessage());
                emailEntityRepo.save(entity);

                // Ack the message to permanently remove it from RabbitMQ. Do NOT retry.
                channel.basicAck(tag, false);
                return;
            }

            // 2. RETRYABLE ERRORS (Throttling or Temporary SMTP issues)
            // If SES returns 454 Throttling, 421 Too many connections, or a general timeout
            executeRetryLogic(entity, channel, tag, emailId, errorMessage);

        } catch (Exception e) {
            // 3. GENERAL ERRORS (Database connection drops, generic IO issues) -> Retry
            executeRetryLogic(entity, channel, tag, emailId, e.getMessage());
        }
    }
    private void executeRetryLogic(EmailEntity entity, Channel channel, long tag, Long emailId, String errorMessage) throws IOException {
        int currentRetries = entity.getRetryCount();

        if (currentRetries < MAX_RETRIES) {
            entity.setRetryCount(currentRetries + 1);
            emailEntityRepo.save(entity);

            log.warn("Retryable error for Email ID: {} | Attempt {} of {} | Requeueing. Reason: {}",
                    emailId, currentRetries + 1, MAX_RETRIES, errorMessage);

            channel.basicReject(tag, false);
        } else {
            entity.setStatus("FAILED");
            entity.setErrorMessage("MAX RETRIES EXHAUSTED: " + errorMessage);
            emailEntityRepo.save(entity);

            log.error("Max retries exhausted for Email ID: {}. Marking FAILED. Error: {}", emailId, errorMessage);
            channel.basicAck(tag, false);
        }
    }
}
