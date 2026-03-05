package com.example.controller;

import com.example.entity.UnsubscribedUser;
import com.example.repository.UnsubscribedUserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/emails")
public class UnsubscribeController {

    @Autowired
    private UnsubscribedUserRepo unsubscribedUserRepo; // Your new Repo

    @GetMapping(value = "/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unsubscribeUser(@RequestParam("email") String email) {

        try {
            // 1. Check if they are already unsubscribed
            if (!unsubscribedUserRepo.existsByEmailAddress(email)) {

                // 2. Add them to the blocklist
                UnsubscribedUser blockedUser = new UnsubscribedUser();
                blockedUser.setEmailAddress(email);
                blockedUser.setUnsubscribedAt(LocalDateTime.now());
                unsubscribedUserRepo.save(blockedUser);

                log.info("User {} has successfully unsubscribed.", email);
            }

            // 3. Return a friendly web page directly from the backend
            String successHtml = """
                <html>
                    <body style="font-family: Arial, sans-serif; text-align: center; padding-top: 50px;">
                        <h2 style="color: #2c3e50;">You have been successfully unsubscribed.</h2>
                        <p>You will no longer receive emails from us at <b>%s</b>.</p>
                    </body>
                </html>
                """.formatted(email);

            return ResponseEntity.ok(successHtml);

        } catch (Exception e) {
            log.error("Unsubscribe failed for email {}: {}", email, e.getMessage());
            return ResponseEntity.internalServerError().body("An error occurred. Please try again later.");
        }
    }
}
