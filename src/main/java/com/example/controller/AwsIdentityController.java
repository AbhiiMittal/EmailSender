package com.example.controller;

import com.example.DTO.DnsRecord;
import com.example.DTO.DomainRequest;
import com.example.service.AwsIdentityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/identity")
public class AwsIdentityController {

    @Autowired
    AwsIdentityService awsIdentityService;

    @PostMapping("/create/domain")
    public List<DnsRecord> createDomainIdentity(@RequestBody DomainRequest request){
        System.out.println(request.getDomainName());
        List<DnsRecord> dkimRecords = awsIdentityService.createDomainIdentity(request.getDomainName());
        return dkimRecords;
    }

    @GetMapping("/verify/domain")
    public boolean verifyDomainIdentity(@RequestBody DomainRequest request){
        System.out.println(request.getDomainName());
        boolean domainRequest = awsIdentityService.verifyDomainIdentity(request.getDomainName());
        return domainRequest;
    }

    @PostMapping("/create/email")
    public String createEmailIdentity(@RequestBody DomainRequest request){
        System.out.println(request.getDomainName());
        awsIdentityService.createEmailIdentity(request.getDomainName());
        return "sent";
    }

    @GetMapping("/verify/email")
    public boolean verifyEmailIdentity(@RequestBody DomainRequest request){
        System.out.println(request.getDomainName());
        boolean domainRequest = awsIdentityService.verifyDomainIdentity(request.getDomainName());
        return domainRequest;
    }
}
