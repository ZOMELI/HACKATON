package pe.bcp.peoplecapacity.demand.infrastructure.ledger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import pe.bcp.peoplecapacity.demand.domain.AvailabilityClaim;
import pe.bcp.peoplecapacity.demand.domain.AvailabilityStatus;

/** Subconjunto del esquema `Claim` de contracts/ledger.yaml que este servicio necesita. */
@JsonIgnoreProperties(ignoreUnknown = true)
record ClaimDto(
        String id,
        String personId,
        String personName,
        String personRole,
        String subject,
        String value,
        String source,
        Instant observedAt,
        LocalDate validFrom,
        LocalDate validUntil,
        String supersedes) {

    static final String AVAILABILITY = "AVAILABILITY";

    /**
     * Convierte a disponibilidades del dominio. Filtra de forma defensiva: solo AVAILABILITY,
     * sin las reemplazadas vía `supersedes` y con valor PLANNED o CONFIRMED.
     */
    static List<AvailabilityClaim> toAvailabilities(Collection<ClaimDto> claims) {
        Set<String> superseded = claims.stream()
                .map(ClaimDto::supersedes)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return claims.stream()
                .filter(c -> AVAILABILITY.equals(c.subject()))
                .filter(c -> c.id() == null || !superseded.contains(c.id()))
                .filter(c -> c.personId() != null && status(c.value()) != null)
                .map(c -> new AvailabilityClaim(c.personId(), c.personName(), c.personRole(), status(c.value()),
                        c.validFrom(), c.validUntil(), c.source(), c.observedAt()))
                .toList();
    }

    private static AvailabilityStatus status(String value) {
        try {
            return value == null ? null : AvailabilityStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
