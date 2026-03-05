package com.example.repository;

import com.example.entity.DomainIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainIdentityRepo extends JpaRepository<DomainIdentity,Long> {
    boolean existsByDomainName(String domainName);
    DomainIdentity findByDomainName(String domainName);
}
