package com.example.service;

import com.example.entity.EmailIdentity;
import com.example.entity.EmailVerificationJob;
import com.example.repository.EmailIdentityRepo;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.GetEmailIdentityRequest;
import software.amazon.awssdk.services.sesv2.model.GetEmailIdentityResponse;

@Service
public class EmailVerificationService {

    @Autowired
    EmailIdentityRepo emailIdentityRepo;

    @Autowired
    SesV2Client client;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "email-verification-queue")
    public void checkEmailVerification(EmailVerificationJob job){

        EmailIdentity emailIdentity =
                emailIdentityRepo.findById(job.getEmailId()).orElse(null);

        if(emailIdentity == null) return;

        GetEmailIdentityResponse response =
                client.getEmailIdentity(
                        GetEmailIdentityRequest.builder()
                                .emailIdentity(emailIdentity.getEmail())
                                .build()
                );

        String status = response.verificationStatusAsString();

        if("SUCCESS".equals(status)){
            emailIdentity.setStatus("VERIFIED");
            emailIdentityRepo.save(emailIdentity);
            return;
        }

        if(job.getRetryCount() >= 30) {   // example limit

            emailIdentity.setStatus("FAILED");
            emailIdentityRepo.save(emailIdentity);

            return;
        }

        job.setRetryCount(job.getRetryCount() + 1);

        rabbitTemplate.convertAndSend(
                "email-verification-delay-queue",
                job
        );
    }
}
