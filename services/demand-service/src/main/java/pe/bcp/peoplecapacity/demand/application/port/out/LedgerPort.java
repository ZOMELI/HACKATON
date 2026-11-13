package pe.bcp.peoplecapacity.demand.application.port.out;

import java.util.List;
import pe.bcp.peoplecapacity.demand.domain.AvailabilityClaim;

/** Libro de afirmaciones (ledger-service). */
public interface LedgerPort {

    /**
     * Afirmaciones AVAILABILITY actuales (no reemplazadas).
     *
     * @throws LedgerUnavailableException si el libro no responde o responde con error
     */
    List<AvailabilityClaim> findCurrentAvailabilities();
}
