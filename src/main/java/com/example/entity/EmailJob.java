package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "email_jobs")
public class EmailJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status; // PENDING, PROCESSING, COMPLETED

    @Column(name = "job_user")
    private String user;

    @Column(name = "execution_time")
    private LocalDateTime executionTime;

    // A job has ONE template
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "email_details_id")
    private EmailDetails emailDetails;

    // A job has MANY recipients
    @OneToMany(mappedBy = "emailJob", cascade = CascadeType.ALL)
    private List<EmailEntity> emailEntities = new ArrayList<>();

    // Helper method to keep both sides of the relationship in sync
    public void addEmailEntity(EmailEntity entity) {
        emailEntities.add(entity);
        entity.setEmailJob(this);
    }
}
