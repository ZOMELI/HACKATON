package pe.bcp.peoplecapacity.cases.infrastructure.ledger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pe.bcp.peoplecapacity.cases.application.port.out.LedgerPort;
import pe.bcp.peoplecapacity.cases.domain.model.Claim;

/**
 * Libro simulado en memoria con las afirmaciones de contracts/examples/claims.json.
 * Solo para pruebas (ledger.mode=stub): en ejecución normal siempre se usa el ledger-service real.
 */
@Component
@ConditionalOnProperty(name = "ledger.mode", havingValue = "stub")
public class StubLedgerAdapter implements LedgerPort {

    private static final Logger log = LoggerFactory.getLogger(StubLedgerAdapter.class);

    private final List<Claim> claims = new CopyOnWriteArrayList<>();
    private final AtomicInteger sequence = new AtomicInteger();

    public StubLedgerAdapter(ObjectMapper mapper) {
        try (InputStream in = new ClassPathResource("stub/claims.json").getInputStream()) {
            claims.addAll(mapper.readValue(in, new TypeReference<List<Claim>>() {
            }));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        log.warn("LEDGER_MODE=stub: libro simulado en memoria con {} afirmaciones de ejemplo", claims.size());
    }

    @Override
    public List<Claim> currentClaims() {
        Set<String> superseded = claims.stream().map(Claim::supersedes).filter(Objects::nonNull).collect(Collectors.toSet());
        return claims.stream().filter(c -> !superseded.contains(c.id())).toList();
    }

    @Override
    public Claim record(Claim c) {
        Claim stored = new Claim(String.format("CLM-D%04d", sequence.incrementAndGet()), c.personId(), c.personName(),
                c.personRole(), c.subject(), c.value(), c.attributes(), c.source(), c.sourceRef(), c.observedAt(),
                Instant.now(), c.validFrom(), c.validUntil(), c.supersedes(), c.evidence(), c.recordedBy());
        claims.add(stored);
        return stored;
    }
}
