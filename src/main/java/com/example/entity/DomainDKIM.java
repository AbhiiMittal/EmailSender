package com.example.entity;

import jakarta.persistence.*;


import jakarta.persistence.*;

@Entity
@Table(name = "domain_dkim")
public class DomainDKIM {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dkim")
    private String dkim;

    @ManyToOne
    @JoinColumn(name = "domain_id")
    private DomainIdentity domainIdentity;

    public Long getId() {
        return id;
    }

    public String getDkim() {
        return dkim;
    }

    public void setDkim(String dkim) {
        this.dkim = dkim;
    }

    public DomainIdentity getDomainIdentity() {
        return domainIdentity;
    }

    public void setDomainIdentity(DomainIdentity domainIdentity) {
        this.domainIdentity = domainIdentity;
    }
}