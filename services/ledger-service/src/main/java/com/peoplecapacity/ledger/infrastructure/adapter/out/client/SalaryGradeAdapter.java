package com.peoplecapacity.ledger.infrastructure.adapter.out.client;

import com.peoplecapacity.ledger.application.port.out.SalaryGradePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.util.Optional;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/** Adaptador de salida: pide el grado a SIGA con la matrícula de quien consulta. Una negativa devuelve vacío. */
@ApplicationScoped
public class SalaryGradeAdapter implements SalaryGradePort {

    @Inject
    @RestClient
    SigaClient siga;

    @Override
    public Optional<String> find(String personId, String requesterId) {
        if (requesterId == null || requesterId.isBlank()) {
            return Optional.empty();
        }
        try {
            Object grade = siga.salaryGrade(personId, requesterId).get("salaryGrade");
            return Optional.ofNullable(grade).map(Object::toString);
        } catch (WebApplicationException denied) {
            return Optional.empty();
        }
    }
}
