package com.example.repository;

import com.example.entity.EmailJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailJobRepo extends JpaRepository<EmailJob,Long> {
    List<EmailJob> findByStatusAndExecutionTimeLessThanEqual(String status, LocalDateTime currentTime);
    List<EmailJob> findByStatus(String status);
}
