package com.peoplecapacity.ledger.application.port.out;

import java.util.Optional;

/** Puerto de salida: consulta el grado salarial en SIGA. El dato nunca se guarda en el libro. */
public interface SalaryGradePort {

    Optional<String> find(String personId, String requesterId);
}
