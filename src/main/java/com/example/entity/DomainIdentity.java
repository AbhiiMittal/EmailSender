package com.example.entity;

import jakarta.persistence.*;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "domain_entity")
public class DomainIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "domain_name")
    private String domainName;

    @Column(name = "status")
    private String status;

    @OneToMany(mappedBy = "domainIdentity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DomainDKIM> dkims;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<DomainDKIM> getDkims() {
        return dkims;
    }

    public void setDkims(List<DomainDKIM> dkims) {
        this.dkims = dkims;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
