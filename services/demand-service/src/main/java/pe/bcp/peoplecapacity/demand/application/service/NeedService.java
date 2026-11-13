package pe.bcp.peoplecapacity.demand.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.bcp.peoplecapacity.demand.application.port.in.ListNeedsUseCase;
import pe.bcp.peoplecapacity.demand.application.port.in.NeedWithCandidates;
import pe.bcp.peoplecapacity.demand.application.port.in.RegisterInformalNeedUseCase;
import pe.bcp.peoplecapacity.demand.application.port.in.UpsertNeedsUseCase;
import pe.bcp.peoplecapacity.demand.application.port.out.LedgerPort;
import pe.bcp.peoplecapacity.demand.application.port.out.NeedRepository;
import pe.bcp.peoplecapacity.demand.domain.AvailabilityClaim;
import pe.bcp.peoplecapacity.demand.domain.CandidateScorer;
import pe.bcp.peoplecapacity.demand.domain.InvalidNeedException;
import pe.bcp.peoplecapacity.demand.domain.Need;
import pe.bcp.peoplecapacity.demand.domain.NeedStatus;
import pe.bcp.peoplecapacity.demand.domain.RecruitmentStatus;

@Service
public class NeedService implements UpsertNeedsUseCase, RegisterInformalNeedUseCase, ListNeedsUseCase {

    private static final Logger log = LoggerFactory.getLogger(NeedService.class);
    static final String WEB_SOURCE = "WEB";

    /** Tablero: primero lo que se necesita antes; sin fecha al final. */
    private static final Comparator<Need> BOARD_ORDER = Comparator
            .comparing(Need::neededFrom, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Need::externalId, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Need::id);

    private final NeedRepository needs;
    private final LedgerPort ledger;
    private final CandidateScorer scorer;
    private final Clock clock;

    public NeedService(NeedRepository needs, LedgerPort ledger, CandidateScorer scorer, Clock clock) {
        this.needs = needs;
        this.ledger = ledger;
        this.scorer = scorer;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UpsertResult upsert(List<NeedCommand> commands) {
        if (commands == null) {
            throw new InvalidNeedException("El cuerpo debe incluir la lista de necesidades (needs)");
        }
        int created = 0;
        int updated = 0;
        // Si una necesidad es inválida, la excepción revierte la transacción completa.
        for (int i = 0; i < commands.size(); i++) {
            NeedCommand command = commands.get(i);
            try {
                if (command == null || isBlank(command.externalId())) {
                    throw new InvalidNeedException("La necesidad debe indicar su identificador externo (externalId)");
                }
                Optional<Need> existing = needs.findByExternalId(command.externalId());
                String id = existing.map(Need::id).orElseGet(() -> UUID.randomUUID().toString());
                needs.save(toNeed(id, command));
                if (existing.isPresent()) {
                    updated++;
                } else {
                    created++;
                }
            } catch (InvalidNeedException e) {
                String ref = command == null || isBlank(command.externalId()) ? "" : " (" + command.externalId() + ")";
                throw new InvalidNeedException("needs[%d]%s: %s".formatted(i, ref, e.getMessage()));
            }
        }
        return new UpsertResult(created, updated);
    }

    @Override
    @Transactional
    public NeedWithCandidates register(InformalNeedCommand command, String userId) {
        if (command == null || isBlank(command.role())) {
            throw new InvalidNeedException("Falta el perfil requerido (role)");
        }
        if (isBlank(command.tribe()) && isBlank(command.project())) {
            throw new InvalidNeedException("Falta la tribu o el proyecto (tribe o project)");
        }
        Need need = needs.save(new Need(
                UUID.randomUUID().toString(), null, true,
                command.tribe(), command.project(), command.role().trim(), command.seniority(),
                command.mechanism(), command.neededFrom(), NeedStatus.OPEN,
                WEB_SOURCE, Instant.now(clock), null, command.note(), null,
                isBlank(userId) ? null : userId.trim()));
        return withCandidates(need, loadAvailabilities());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NeedWithCandidates> list(NeedStatus status) {
        List<Need> found = needs.findAll(status);
        // Una sola consulta al libro para todo el tablero.
        Optional<List<AvailabilityClaim>> availabilities = loadAvailabilities();
        return found.stream()
                .sorted(BOARD_ORDER)
                .map(need -> withCandidates(need, availabilities))
                .toList();
    }

    private NeedWithCandidates withCandidates(Need need, Optional<List<AvailabilityClaim>> availabilities) {
        return availabilities
                .map(claims -> new NeedWithCandidates(need, scorer.rank(need, claims), true))
                .orElseGet(() -> new NeedWithCandidates(need, List.of(), false));
    }

    /** Vacío si el libro no responde: degradación segura, el tablero se muestra sin candidatos. */
    private Optional<List<AvailabilityClaim>> loadAvailabilities() {
        try {
            LocalDate today = LocalDate.now(clock);
            return Optional.of(latestPerPerson(ledger.findCurrentAvailabilities().stream()
                    .filter(claim -> claim.isCurrentOn(today))
                    .toList()));
        } catch (RuntimeException e) {
            log.warn("El libro de afirmaciones no respondió; se devuelven necesidades sin candidatos: {}",
                    e.getMessage());
            return Optional.empty();
        }
    }

    /** Si el libro trae más de una disponibilidad por persona, se usa la de consulta más reciente. */
    private static List<AvailabilityClaim> latestPerPerson(Collection<AvailabilityClaim> claims) {
        Map<String, AvailabilityClaim> byPerson = new LinkedHashMap<>();
        for (AvailabilityClaim claim : claims) {
            byPerson.merge(claim.personId(), claim, (a, b) -> isNewer(b, a) ? b : a);
        }
        return List.copyOf(byPerson.values());
    }

    private static boolean isNewer(AvailabilityClaim candidate, AvailabilityClaim current) {
        if (candidate.observedAt() == null) {
            return false;
        }
        return current.observedAt() == null || candidate.observedAt().isAfter(current.observedAt());
    }

    private static Need toNeed(String id, NeedCommand c) {
        RecruitmentStatus recruitment = null;
        boolean traceable = !isBlank(c.source()) && c.observedAt() != null; // si no, Need lo rechaza abajo
        if (c.recruitment() != null && traceable) {
            // El seguimiento hereda fuente y fecha de la necesidad si no trae las propias.
            recruitment = new RecruitmentStatus(
                    c.recruitment().status(),
                    isBlank(c.recruitment().source()) ? c.source() : c.recruitment().source(),
                    c.recruitment().observedAt() == null ? c.observedAt() : c.recruitment().observedAt());
        }
        return new Need(id, c.externalId().trim(), false, c.tribe(), c.project(),
                c.role() == null ? null : c.role().trim(), c.seniority(), c.mechanism(), c.neededFrom(),
                c.status(), c.source(), c.observedAt(), recruitment, c.note(), c.evidenceQuote(), null);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
