package com.example.controller;

import com.example.repository.EmailEntityRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/api/tracking")
public class EmailTrackingController {

    @Autowired
    private EmailEntityRepo emailEntityRepo;

    // This is the hardcoded byte array for a 1x1 transparent GIF
    private static final byte[] TRANSPARENT_PIXEL = Base64.getDecoder()
            .decode("R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==");

    @GetMapping("/open/{emailId}/pixel.gif")
    public ResponseEntity<byte[]> trackEmailOpen(@PathVariable Long emailId) {

        try {
            // 1. Find the exact email sent
            emailEntityRepo.findById(emailId).ifPresent(entity -> {
                // Only update if it hasn't been opened yet
                if (entity.getOpenedAt() == null) {
                    entity.setOpenedAt(LocalDateTime.now());
                    emailEntityRepo.save(entity);
                    log.info("Email ID {} was just opened!", emailId);
                }
            });
        } catch (Exception e) {
            log.error("Failed to track open for Email ID {}: {}", emailId, e.getMessage());
        }

        // 2. Return the invisible image so the email client doesn't show a broken image icon
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_GIF);
        headers.setCacheControl("no-cache, no-store, must-revalidate"); // Prevent caching so we track every open

        return new ResponseEntity<>(TRANSPARENT_PIXEL, headers, HttpStatus.OK);
    }
}