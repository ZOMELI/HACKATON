package pe.bcp.peoplecapacity.cases.application.port.out;

import java.util.List;
import pe.bcp.peoplecapacity.cases.domain.model.Claim;

/** Libro de afirmaciones (ledger-service). Lanza LedgerUnavailableException si no responde. */
public interface LedgerPort {

    /** Afirmaciones vigentes (no reemplazadas) de todas las personas. */
    List<Claim> currentClaims();

    /** Registra una afirmación nueva y devuelve la registrada, con su id. */
    Claim record(Claim newClaim);
}
