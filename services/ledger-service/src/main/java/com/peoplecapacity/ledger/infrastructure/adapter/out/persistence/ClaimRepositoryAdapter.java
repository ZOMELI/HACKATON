package com.peoplecapacity.ledger.infrastructure.adapter.out.persistence;

import com.peoplecapacity.ledger.application.port.out.ClaimRepositoryPort;
import com.peoplecapacity.ledger.domain.model.Claim;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Adaptador de salida: persiste afirmaciones en el esquema LEDGER. Solo inserta; no hay update ni delete. */
@ApplicationScoped
public class ClaimRepositoryAdapter implements ClaimRepositoryPort, PanacheRepository<ClaimEntity> {

    @Inject
    EntityManager entityManager;

    @Override
    public Claim append(Claim claim) {
        registerPerson(claim);
        ClaimEntity entity = ClaimEntity.fromDomain(claim);
        persist(entity);
        return entity.toDomain();
    }

    @Override
    public List<Claim> search(String personId, String dimension, String status) {
        StringBuilder query = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (personId != null) {
            query.append(" and personId = :personId");
            params.put("personId", personId);
        }
        if (dimension != null) {
            query.append(" and dimension = :dimension");
            params.put("dimension", dimension.toUpperCase());
        }
        if (status != null) {
            query.append(" and status = :status");
            params.put("status", status.toUpperCase());
        }
        query.append(" order by recordedAt desc, id desc");
        return list(query.toString(), params).stream().map(ClaimEntity::toDomain).toList();
    }

    private void registerPerson(Claim claim) {
        if (entityManager.find(PersonEntity.class, claim.personId()) != null) {
            return;
        }
        PersonEntity person = new PersonEntity();
        person.personId = claim.personId();
        person.fullName = claim.personName();
        person.email = claim.personEmail() == null ? null : claim.personEmail().toLowerCase();
        person.createdAt = LocalDateTime.now();
        entityManager.persist(person);
    }
}
