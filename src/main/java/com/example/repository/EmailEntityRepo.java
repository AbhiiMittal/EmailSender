package com.example.repository;

import com.example.entity.EmailEntity;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface EmailEntityRepo extends JpaRepository<EmailEntity, Long> {
    Slice<EmailEntity> findByEmailJobIdAndStatus(Long jobId,String status, Pageable pageable);

    @Query("SELECT COUNT(e) FROM EmailEntity e WHERE e.emailJob.id = :jobId AND e.status IN ('PENDING', 'PROCESSING')")
    long countUnfinishedEmailsByJobId(@Param("jobId") Long jobId);

    List<EmailEntity> findTopByToEmailOrderByIdDesc(String toEmail);
}
