package pe.bcp.peoplecapacity.cases.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.bcp.peoplecapacity.cases.application.CaseException;
import pe.bcp.peoplecapacity.cases.application.LedgerUnavailableException;
import pe.bcp.peoplecapacity.cases.application.port.in.CaseUseCase;
import pe.bcp.peoplecapacity.cases.application.port.out.CaseRepository;
import pe.bcp.peoplecapacity.cases.application.port.out.LedgerPort;
import pe.bcp.peoplecapacity.cases.domain.model.CaseFile;
import pe.bcp.peoplecapacity.cases.domain.model.CaseOption;
import pe.bcp.peoplecapacity.cases.domain.model.CaseStatus;
import pe.bcp.peoplecapacity.cases.domain.model.CaseType;
import pe.bcp.peoplecapacity.cases.domain.model.Claim;
import pe.bcp.peoplecapacity.cases.domain.model.ClaimIndex;
import pe.bcp.peoplecapacity.cases.domain.model.Decision;
import pe.bcp.peoplecapacity.cases.domain.model.OptionEffect;
import pe.bcp.peoplecapacity.cases.domain.model.Subjects;
import pe.bcp.peoplecapacity.cases.domain.model.UserContext;

@Service
public class CaseService implements CaseUseCase {

    private static final Logger log = LoggerFactory.getLogger(CaseService.class);

    private final CaseRepository cases;
    private final LedgerPort ledger;
    private final Clock clock;

    public CaseService(CaseRepository cases, LedgerPort ledger, Clock clock) {
        this.cases = cases;
        this.ledger = ledger;
        this.clock = clock;
    }

    @Override
    public List<CaseFile> list(UserContext user, CaseStatus status, CaseType type, String personId) {
        ClaimIndex hierarchy = user.seesAll() ? null : hierarchyOrNull();
        return cases.findAll().stream()
                .filter(c -> status == null || c.status() == status)
                .filter(c -> type == null || c.type() == type)
                .filter(c -> personId == null || personId.isBlank() || personId.equals(c.personId()))
                .filter(c -> canSee(user, c, hierarchy))
                .sorted(Comparator.comparing(CaseFile::dueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    public CaseFile get(String id) {
        return cases.findById(id).orElseThrow(() -> CaseException.notFound(id));
    }

    @Override
    public CaseFile decide(String id, UserContext user, String optionId, String comment) {
        if (user.userId() == null || user.userId().isBlank()) {
            throw new CaseException(400, "Falta la cabecera X-User-Id: toda decisión necesita un responsable identificado");
        }
        CaseFile caseFile = get(id);
        if (caseFile.status() == CaseStatus.RESOLVED) {
            throw new CaseException(409, "El caso " + id + " ya fue resuelto");
        }
        CaseOption option = caseFile.option(optionId);
        if (option == null) {
            throw new CaseException(400, "La opción " + optionId + " no pertenece al caso " + id);
        }
        // Para decidir hace falta leer la jerarquía del libro; si el libro no responde la decisión no se toma.
        if (!user.seesAll() && !canSee(user, caseFile, new ClaimIndex(ledger.currentClaims()))) {
            throw new CaseException(403, "Solo el responsable de " + caseFile.personName() + " o su línea de mando puede decidir este caso");
        }

        // Primero se registra en el libro; si el libro falla, el caso sigue abierto.
        Instant now = clock.instant();
        List<String> ledgerIds = new ArrayList<>();
        ledgerIds.add(ledger.record(decisionClaim(caseFile, option, comment, user, now)).id());
        if (option.effect() != null) {
            ledgerIds.add(ledger.record(effectClaim(caseFile, option.effect(), user, now)).id());
        }
        Decision decision = new Decision(option.id(), option.label(), comment, user.userId(), now, ledgerIds);
        log.info("Caso {} resuelto por {} con la opción {}", id, user.userId(), option.id());
        return cases.save(caseFile.resolve(decision));
    }

    private ClaimIndex hierarchyOrNull() {
        try {
            return new ClaimIndex(ledger.currentClaims());
        } catch (LedgerUnavailableException e) {
            log.warn("Libro no disponible: la bandeja muestra solo los casos propios del usuario");
            return null;
        }
    }

    private static boolean canSee(UserContext user, CaseFile c, ClaimIndex hierarchy) {
        if (user.seesAll() || user.userId().equals(c.ownerId())) {
            return true;
        }
        return hierarchy != null && hierarchy.isSameOrAbove(user.userId(), c.ownerId());
    }

    private static Claim decisionClaim(CaseFile c, CaseOption option, String comment, UserContext user, Instant now) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("caseId", c.id());
        attributes.put("caseType", c.type().name());
        attributes.put("optionLabel", option.label());
        if (comment != null && !comment.isBlank()) {
            attributes.put("comment", comment);
        }
        return new Claim(null, c.personId(), c.personName(), null, Subjects.DECISION, option.id(), attributes,
                Subjects.SRC_DECISION, c.id(), now, null, null, null, null, c.evidence(), user.userId());
    }

    private static Claim effectClaim(CaseFile c, OptionEffect effect, UserContext user, Instant now) {
        return new Claim(null, c.personId(), c.personName(), null, effect.subject(), effect.value(),
                Map.of("caseId", c.id()), Subjects.SRC_DECISION, c.id(), now, null, effect.validFrom(), null,
                effect.supersedesClaimId(), c.evidence(), user.userId());
    }
}
