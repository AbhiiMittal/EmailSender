package com.example.service;

import com.example.entity.DomainIdentity;
import com.example.entity.DomainVerificationJob;
import com.example.repository.DomainIdentityRepo;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.GetEmailIdentityRequest;
import software.amazon.awssdk.services.sesv2.model.GetEmailIdentityResponse;


@Service
public class DomainVerificationService {

    @Autowired
    DomainIdentityRepo domainIdentityRepo;

    @Autowired
    SesV2Client client;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "domain-verification-queue")
    public void checkDomainVerification(DomainVerificationJob job) {

        DomainIdentity domain = domainIdentityRepo.findById(job.getDomainId()).orElse(null);

        if(domain == null) return;

        GetEmailIdentityRequest request = GetEmailIdentityRequest.builder()
                .emailIdentity(domain.getDomainName())
                .build();

        GetEmailIdentityResponse response = client.getEmailIdentity(request);

        String status = response.verificationStatusAsString();

        if("SUCCESS".equals(status)) {

            domain.setStatus("VERIFIED");
            domainIdentityRepo.save(domain);
            return;
        }

        if(job.getRetryCount() >= 30) {   // example limit

            domain.setStatus("FAILED");
            domainIdentityRepo.save(domain);

            return;
        }

        job.setRetryCount(job.getRetryCount() + 1);

        rabbitTemplate.convertAndSend(
                "domain-verification-delay-queue",
                job
        );
    }
}
