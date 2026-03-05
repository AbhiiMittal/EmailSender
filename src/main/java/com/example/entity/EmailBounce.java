package com.example.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "email_bounce")
public class EmailBounce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "to_email")
    String emailId;

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }
}