package com.peoplecapacity.ledger.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Registro de identidad: matrícula o código de proveedor como llave, correo como puente hacia Jira. */
@Entity
@Table(name = "PERSON")
public class PersonEntity {

    @Id
    @Column(name = "PERSON_ID")
    public String personId;
    @Column(name = "FULL_NAME")
    public String fullName;
    @Column(name = "EMAIL")
    public String email;
    @Column(name = "CREATED_AT")
    public LocalDateTime createdAt;
}
