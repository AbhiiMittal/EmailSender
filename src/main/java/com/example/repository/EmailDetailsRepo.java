package com.example.repository;

import com.example.entity.EmailDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailDetailsRepo extends JpaRepository<EmailDetails,Long> {
}
