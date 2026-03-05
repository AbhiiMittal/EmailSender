package com.example.service;

import com.example.entity.EmailJob;
import com.example.repository.EmailEntityRepo;
import com.example.repository.EmailJobRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class EmailSchedulerService {

    @Autowired
    private EmailJobRepo emailJobRepo;

    @Autowired
    private ProcessEmail processEmail;

    @Autowired
    EmailEntityRepo emailEntityRepo;

    // Runs every minute, exactly at the top of the minute (e.g., 10:01:00, 10:02:00)
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void checkAndExecuteDueJobs() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Scheduler Heartbeat: Checking for due jobs at {}", now);

        // 1. Fetch jobs that are due RIGHT NOW or were missed while the server was off
        List<EmailJob> dueJobs = emailJobRepo.findByStatusAndExecutionTimeLessThanEqual("PENDING", now);

        if (dueJobs.isEmpty()) {
            return; // Nothing to do this minute
        }

        log.info("Found {} jobs ready for execution!", dueJobs.size());

        for (EmailJob job : dueJobs) {
            // 2. Lock the job immediately so it doesn't get picked up again next minute
            job.setStatus("PROCESSING");
            emailJobRepo.save(job);

            log.info("Starting execution for Job ID: {}", job.getId());

            // 3. Hand it off to your RabbitMQ producer!
            // (We will update processEmailBatch to accept an EmailJob)
            processEmail.processEmailBatch(job);
        }
    }

    @Scheduled(fixedDelay = 120000)
    @Transactional
    public void sweepAndCompleteJobs() {
        log.debug("Sweeper Heartbeat: Checking for completed jobs...");

        // Fetch all jobs currently marked as PROCESSING
        List<EmailJob> processingJobs = emailJobRepo.findByStatus("PROCESSING");

        for (EmailJob job : processingJobs) {

            // Ask the database if there are any emails still waiting or sending
            long unfinishedCount = emailEntityRepo.countUnfinishedEmailsByJobId(job.getId());

            if (unfinishedCount == 0) {
                // If 0, every single email is either SENT or FAILED. The job is done!
                job.setStatus("COMPLETED");
                emailJobRepo.save(job);
                log.info("SWEEPER: Job ID {} has successfully COMPLETED all emails.", job.getId());
            } else {
                log.debug("SWEEPER: Job ID {} is still running. {} emails remaining.", job.getId(), unfinishedCount);
            }
        }
    }
}
