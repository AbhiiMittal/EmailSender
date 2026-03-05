package com.example.service;

import com.example.DTO.DnsRecord;
import com.example.entity.DomainDKIM;
import com.example.entity.DomainIdentity;
import com.example.entity.DomainVerificationJob;
import com.example.entity.EmailIdentity;
import com.example.repository.DomainDKIMRepo;
import com.example.repository.DomainIdentityRepo;
import com.example.repository.EmailIdentityRepo;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.CreateEmailIdentityRequest;
import software.amazon.awssdk.services.sesv2.model.CreateEmailIdentityResponse;
import software.amazon.awssdk.services.sesv2.model.GetEmailIdentityRequest;
import software.amazon.awssdk.services.sesv2.model.GetEmailIdentityResponse;

import java.util.ArrayList;
import java.util.List;

@Service
public class AwsIdentityService {

    @Autowired
    SesV2Client client;

    @Autowired
    DomainIdentityRepo domainIdentityRepo;

    @Autowired
    DomainDKIMRepo domainDKIMRepo;

    @Autowired
    EmailIdentityRepo emailIdentityRepo;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public List<DnsRecord> createDomainIdentity(String domainName){
        try{

            if(!isValidDomain(domainName)) return new ArrayList<>();

            List<DnsRecord> records = new ArrayList<>();

            if(domainIdentityRepo.existsByDomainName(domainName)){

                DomainIdentity domainIdentity = domainIdentityRepo.findByDomainName(domainName);

                List<DomainDKIM> dkims = domainIdentity.getDkims();

                for(DomainDKIM dkim : dkims){

                    String token = dkim.getDkim();
                    String name = token + "._domainkey." + domainName;
                    String value = token + ".dkim.amazonses.com";

                    records.add(new DnsRecord(name, "CNAME", value));
                }

            }else{

                CreateEmailIdentityRequest request = CreateEmailIdentityRequest.builder()
                        .emailIdentity(domainName)
                        .build();

                CreateEmailIdentityResponse response = client.createEmailIdentity(request);

                List<String> tokens = response.dkimAttributes().tokens();

                // create domain entity
                DomainIdentity domainIdentity = new DomainIdentity();
                domainIdentity.setDomainName(domainName);
                domainIdentity.setStatus("PENDING");

                domainIdentity = domainIdentityRepo.save(domainIdentity);

                List<DomainDKIM> dkimEntities = new ArrayList<>();

                for(String token : tokens){

                    String name = token + "._domainkey." + domainName;
                    String value = token + ".dkim.amazonses.com";

                    records.add(new DnsRecord(name, "CNAME", value));

                    DomainDKIM domainDKIM = new DomainDKIM();
                    domainDKIM.setDkim(token);
                    domainDKIM.setDomainIdentity(domainIdentity);

                    dkimEntities.add(domainDKIM);
                }

                domainDKIMRepo.saveAll(dkimEntities);
                DomainVerificationJob domainVerificationJob = new DomainVerificationJob();
                domainVerificationJob.setDomainId(domainIdentity.getId());;
                domainVerificationJob.setRetryCount(0);
                rabbitTemplate.convertAndSend(
                        "domain-verification-queue",
                        domainVerificationJob
                );
            }

            // SPF Record
            records.add(
                    new DnsRecord(
                            domainName,
                            "TXT",
                            "v=spf1 include:amazonses.com ~all"
                    )
            );

            // DMARC Record
            records.add(
                    new DnsRecord(
                            "_dmarc." + domainName,
                            "TXT",
                            "v=DMARC1; p=none;"
                    )
            );

            return records;

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static boolean isValidDomain(String domain) {
        String regex = "^(?!-)[A-Za-z0-9-]{1,63}(\\.[A-Za-z]{2,})+$";
        return domain.matches(regex);
    }

    public boolean verifyDomainIdentity(String domainName){
        if(domainIdentityRepo.existsByDomainName(domainName)){
            DomainIdentity domainIdentity = domainIdentityRepo.findByDomainName(domainName);
            if(domainIdentity.getStatus().equals("PENDING")){
                GetEmailIdentityRequest request = GetEmailIdentityRequest.builder()
                        .emailIdentity(domainName)
                        .build();

                GetEmailIdentityResponse response = client.getEmailIdentity(request);

                String status = response.verificationStatusAsString();
                if(!status.equals("PENDING")){
                    return true;
                }
            }
        }
        return false;
    }

    public void createEmailIdentity(String email){

        if(emailIdentityRepo.existsByEmail(email)){
            return;
        }

        CreateEmailIdentityRequest request =
                CreateEmailIdentityRequest.builder()
                        .emailIdentity(email)
                        .build();

        client.createEmailIdentity(request);

        EmailIdentity entity = new EmailIdentity();
        entity.setEmail(email);
        entity.setStatus("PENDING");

        emailIdentityRepo.save(entity);

        rabbitTemplate.convertAndSend(
                "email-verification-queue",
                entity.getId()
        );
    }

    public boolean verifyEmailIdentity(String email){
        if(emailIdentityRepo.existsByEmail(email)){
            EmailIdentity emailIdentity = emailIdentityRepo.findByEmail(email);
            if(emailIdentity.getStatus().equals("PENDING")){
                GetEmailIdentityRequest request = GetEmailIdentityRequest.builder()
                        .emailIdentity(email)
                        .build();

                GetEmailIdentityResponse response = client.getEmailIdentity(request);

                String status = response.verificationStatusAsString();
                if(!status.equals("PENDING")){
                    return true;
                }
            }
        }
        return false;
    }
}
