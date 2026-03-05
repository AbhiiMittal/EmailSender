package com.example.repository;

import com.example.entity.EmailIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailIdentityRepo extends JpaRepository<EmailIdentity,Long> {
    boolean existsByEmail(String email);
    EmailIdentity findByEmail(String email);
}
