package com.example.controller;

import com.example.entity.EmailBounce;
import com.example.entity.EmailEntity;
import com.example.repository.EmailBounceRepo;
import com.example.repository.EmailEntityRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/aws-sns")
public class AwsSnsWebhookController {

    @Autowired
    private EmailEntityRepo emailEntityRepo;

    @Autowired
    private EmailBounceRepo emailBounceRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/bounces")
    @Transactional
    public ResponseEntity<String> handleSnsWebhook(@RequestHeader(value = "x-amz-sns-message-type", required = false) String messageType,
                                                   @RequestBody String payload) {
        try {
            JsonNode rootNode = objectMapper.readTree(payload);

            // 1. Handle AWS SNS Subscription Confirmation (Crucial!)
            if ("SubscriptionConfirmation".equals(messageType)) {
                String subscribeUrl = rootNode.get("SubscribeURL").asText();
                log.info("Received SNS Subscription Confirmation. Confirming URL: {}", subscribeUrl);

                // AWS requires us to send a simple GET request to this URL to activate the webhook
                restTemplate.getForEntity(subscribeUrl, String.class);
                return ResponseEntity.ok("Subscription Confirmed");
            }

            // 2. Handle the Actual Bounce Notification
            if ("Notification".equals(messageType)) {
                // The actual SES data is a JSON string hidden INSIDE the SNS "Message" field
                String messageString = rootNode.get("Message").asText();
                JsonNode sesMessage = objectMapper.readTree(messageString);

                String notificationType = sesMessage.get("notificationType").asText();

                if ("Bounce".equals(notificationType)) {
                    JsonNode bounceData = sesMessage.get("bounce");
                    String bounceType = bounceData.get("bounceType").asText(); // "Permanent" or "Transient"
                    String bounceSubType = bounceData.get("bounceSubType").asText();

                    // Loop through the bounced recipients (usually just 1, but AWS sends it as an array)
                    JsonNode bouncedRecipients = bounceData.get("bouncedRecipients");
                    for (JsonNode recipient : bouncedRecipients) {
                        String bouncedEmailAddress = recipient.get("emailAddress").asText();
                        String diagnosticCode = recipient.has("diagnosticCode") ? recipient.get("diagnosticCode").asText() : "No diagnostic code provided";

                        log.warn("HARD BOUNCE DETECTED for email: {}. Type: {}", bouncedEmailAddress, bounceType);

                        // 3. Find this user in the database and mark them as FAILED
                        // (We fetch the most recently created record for this email address)
                        List<EmailEntity> entities = emailEntityRepo.findTopByToEmailOrderByIdDesc(bouncedEmailAddress);

                        if (!entities.isEmpty()) {
                            EmailEntity entity = entities.get(0);
                            entity.setStatus("FAILED");
                            entity.setErrorMessage("AWS SNS BOUNCE [" + bounceType + " - " + bounceSubType + "]: " + diagnosticCode);
                            EmailBounce emailBounce = new EmailBounce();
                            emailBounce.setEmailId(entities.get(0).getToEmail());
                            emailBounceRepo.save(emailBounce);
                            emailEntityRepo.save(entity);
                            log.info("Successfully updated DB: Marked {} as FAILED due to Hard Bounce.", bouncedEmailAddress);
                        }
                    }
                }
            }

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            log.error("Failed to process AWS SNS Webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Webhook processing failed");
        }
    }
}
