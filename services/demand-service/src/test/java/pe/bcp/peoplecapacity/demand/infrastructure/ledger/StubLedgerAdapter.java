package pe.bcp.peoplecapacity.demand.infrastructure.ledger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pe.bcp.peoplecapacity.demand.application.port.out.LedgerPort;
import pe.bcp.peoplecapacity.demand.domain.AvailabilityClaim;

/**
 * ledger.mode=stub (por defecto): afirmaciones en memoria cargadas de stub/claims.json,
 * copia de contracts/examples/claims.json. Permite trabajar sin el ledger-service.
 */
@Component
@ConditionalOnProperty(name = "ledger.mode", havingValue = "stub")
class StubLedgerAdapter implements LedgerPort {

    private final List<AvailabilityClaim> availabilities;

    StubLedgerAdapter(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource("stub/claims.json").getInputStream()) {
            List<ClaimDto> claims = objectMapper.readValue(in, new TypeReference<>() {
            });
            this.availabilities = ClaimDto.toAvailabilities(claims);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer stub/claims.json", e);
        }
    }

    @Override
    public List<AvailabilityClaim> findCurrentAvailabilities() {
        return availabilities;
    }
}
