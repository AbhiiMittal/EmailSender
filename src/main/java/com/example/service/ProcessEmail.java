package com.example.service;

import com.example.entity.EmailEntity;
import com.example.entity.EmailJob;
import com.example.entity.MailMQDTO;
import com.example.helper.ExcelEmailHandler;
import com.example.repository.EmailBounceRepo;
import com.example.repository.EmailEntityRepo;
import com.example.repository.UnsubscribedUserRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ProcessEmail {

    @Autowired
    EmailService emailService;

    @Autowired
    ExcelEmailHandler excelEmailReader;

    @Autowired
    EmailEntityRepo emailEntityRepo;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    UnsubscribedUserRepo unsubscribedUserRepo;

    @Autowired
    EmailBounceRepo emailBounceRepo;

    @Async // Runs in background so it doesn't block the Scheduler Heartbeat
    @Transactional
    public void processEmailBatch(EmailJob job) {
        log.info("Starting email batch processing for Job ID: {}", job.getId());

        try {
            int batchSize = 500;
            log.info("Fetching PENDING emails for Job {} in batches of {}...", job.getId(), batchSize);

            // Fetch ONLY the emails belonging to THIS specific job
            Slice<EmailEntity> emailEntities = emailEntityRepo.findByEmailJobIdAndStatus(
                    job.getId(), "PENDING", PageRequest.of(0, batchSize));

            int totalQueued = 0;

            while (emailEntities.hasContent()) {
                List<EmailEntity> processedEntities = new ArrayList<>();

                List<String> emailsInBatch = emailEntities.getContent().stream()
                        .map(EmailEntity::getToEmail)
                        .toList();

                List<String> blocklistedList = unsubscribedUserRepo.findEmailAddressesIn(emailsInBatch);
                List<String> nonExistingList = emailBounceRepo.findEmailAddressesIn(emailsInBatch);

                Set<String> blocklistedEmails = new HashSet<>(blocklistedList);
                Set<String> nonExistingEmails = new HashSet<>(nonExistingList);

                for (EmailEntity email : emailEntities.getContent()) {

                    if (blocklistedEmails.contains(email.getToEmail())) {
                        log.debug("Skipping unsubscribed user: {}", email.getToEmail());

                        email.setStatus("FAILED");
                        email.setErrorMessage("SKIPPED: User is unsubscribed globally.");
                        processedEntities.add(email);

                        continue; // Skip the RabbitMQ queueing below!
                    }else if (nonExistingEmails.contains(email.getToEmail())) {
                        log.debug("Skipping email does not exist: {}", email.getToEmail());

                        email.setStatus("FAILED");
                        email.setErrorMessage("SKIPPED: email does not exist globally.");
                        processedEntities.add(email);

                        continue; // Skip the RabbitMQ queueing below!
                    }

                    // Optimized DTO: Only passing the ID and Address to save RAM!
                    MailMQDTO mailMQDTO = new MailMQDTO();
                    mailMQDTO.setEmailId(email.getId());
                    mailMQDTO.setEmail(email.getToEmail());

                    rabbitTemplate.convertAndSend("mail-queue", mailMQDTO);

                    email.setStatus("PROCESSING");
                    processedEntities.add(email);
                }

                // Batch update the DB to PROCESSING
                emailEntityRepo.saveAll(processedEntities);

                totalQueued += processedEntities.size();
                log.info("Successfully pushed {} emails to RabbitMQ for Job {}...", totalQueued, job.getId());

                // Move to the next batch
                if (emailEntities.hasNext()) {
                    emailEntities = emailEntityRepo.findByEmailJobIdAndStatus(
                            job.getId(), "PENDING", emailEntities.nextPageable());
                } else {
                    break;
                }
            }

            log.info("Finished queuing! Total emails pushed to RabbitMQ for Job {}: {}", job.getId(), totalQueued);

        } catch (Exception e) {
            log.error("CRITICAL: Failed to process Job ID {}. Error: {}", job.getId(), e.getMessage(), e);
            throw new RuntimeException("Batch processing failed for Job " + job.getId(), e);
        }
    }
}
