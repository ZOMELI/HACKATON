package com.peoplecapacity.ledger.infrastructure.adapter.out.persistence;

import com.peoplecapacity.ledger.domain.model.Claim;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CLAIM")
public class ClaimEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    public Long id;
    @Column(name = "PERSON_ID")
    public String personId;
    @Column(name = "PERSON_NAME")
    public String personName;
    @Column(name = "PERSON_EMAIL")
    public String personEmail;
    @Column(name = "DIMENSION")
    public String dimension;
    @Column(name = "CLAIM_VALUE")
    public String value;
    @Column(name = "SOURCE")
    public String source;
    @Column(name = "VALID_FROM")
    public LocalDate validFrom;
    @Column(name = "VALID_TO")
    public LocalDate validTo;
    @Column(name = "OBSERVED_AT")
    public LocalDate observedAt;
    @Column(name = "STATUS")
    public String status;
    @Column(name = "EVIDENCE_REF")
    public String evidenceRef;
    @Column(name = "AUTHOR")
    public String author;
    @Column(name = "RECORDED_AT")
    public LocalDateTime recordedAt;

    public static ClaimEntity fromDomain(Claim c) {
        ClaimEntity e = new ClaimEntity();
        e.personId = c.personId();
        e.personName = c.personName();
        e.personEmail = c.personEmail();
        e.dimension = c.dimension();
        e.value = c.value();
        e.source = c.source();
        e.validFrom = c.validFrom();
        e.validTo = c.validTo();
        e.observedAt = c.observedAt();
        e.status = c.status();
        e.evidenceRef = c.evidenceRef();
        e.author = c.author();
        e.recordedAt = c.recordedAt();
        return e;
    }

    public Claim toDomain() {
        return new Claim(id, personId, personName, personEmail, dimension, value, source, validFrom, validTo,
                observedAt, status, evidenceRef, author, recordedAt);
    }
}
