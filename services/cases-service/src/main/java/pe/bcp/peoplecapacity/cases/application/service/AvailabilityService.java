package pe.bcp.peoplecapacity.cases.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.bcp.peoplecapacity.cases.application.CaseException;
import pe.bcp.peoplecapacity.cases.application.port.in.AvailabilityUseCase;
import pe.bcp.peoplecapacity.cases.application.port.out.AlertSettingsRepository;
import pe.bcp.peoplecapacity.cases.application.port.out.CaseRepository;
import pe.bcp.peoplecapacity.cases.application.port.out.LedgerPort;
import pe.bcp.peoplecapacity.cases.domain.model.AlertSettings;
import pe.bcp.peoplecapacity.cases.domain.model.AvailabilityItem;
import pe.bcp.peoplecapacity.cases.domain.model.CaseFile;
import pe.bcp.peoplecapacity.cases.domain.model.CaseStatus;
import pe.bcp.peoplecapacity.cases.domain.model.Claim;
import pe.bcp.peoplecapacity.cases.domain.model.ClaimIndex;
import pe.bcp.peoplecapacity.cases.domain.model.Subjects;
import pe.bcp.peoplecapacity.cases.domain.model.UserContext;
import pe.bcp.peoplecapacity.cases.domain.rules.ReconciliationRules;

@Service
public class AvailabilityService implements AvailabilityUseCase {

    private final LedgerPort ledger;
    private final CaseRepository cases;
    private final AlertSettingsRepository alertSettings;
    private final Clock clock;
    private final String defaultOwnerId;

    public AvailabilityService(LedgerPort ledger, CaseRepository cases, AlertSettingsRepository alertSettings, Clock clock,
                               @Value("${cases.default-owner-id}") String defaultOwnerId) {
        this.ledger = ledger;
        this.cases = cases;
        this.alertSettings = alertSettings;
        this.clock = clock;
        this.defaultOwnerId = defaultOwnerId;
    }

    @Override
    public List<AvailabilityItem> availability(UserContext user) {
        ClaimIndex idx = new ClaimIndex(ledger.currentClaims());
        LocalDate limit = LocalDate.now(clock).plusDays(alertSettings.get().availabilityDays());
        List<AvailabilityItem> items = new ArrayList<>();
        for (String pid : idx.personIds()) {
            // Una disponibilidad confirmada prevalece sobre la prevista de la misma persona.
            Claim claim = idx.latest(pid, Subjects.AVAILABILITY, null);
            Claim confirmed = idx.of(pid, Subjects.AVAILABILITY).stream()
                    .filter(c -> Subjects.CONFIRMED.equalsIgnoreCase(c.value())).findFirst().orElse(null);
            claim = confirmed != null ? confirmed : claim;
            if (claim == null || claim.validFrom() == null) {
                continue;
            }
            String manager = Optional.ofNullable(idx.managerOf(pid)).orElse(defaultOwnerId);
            if (!user.seesAll() && !idx.isSameOrAbove(user.userId(), manager)) {
                continue;
            }
            boolean planned = Subjects.PLANNED.equalsIgnoreCase(claim.value());
            Optional<CaseFile> caseFile = planned
                    ? cases.findByDedupeKey(ReconciliationRules.availabilityKey(pid, claim.validFrom()))
                    : Optional.empty();
            boolean decided = caseFile.map(c -> c.status() == CaseStatus.RESOLVED).orElse(false);
            boolean alert = planned && !decided && !claim.validFrom().isAfter(limit);
            items.add(new AvailabilityItem(pid, idx.nameOf(pid), idx.roleOf(pid), claim.validFrom(),
                    planned ? Subjects.PLANNED : Subjects.CONFIRMED, claim.source(), claim.observedAt(),
                    manager, idx.nameOf(manager), alert, caseFile.map(CaseFile::id).orElse(null)));
        }
        items.sort(Comparator.comparing(AvailabilityItem::availableFrom));
        return items;
    }

    @Override
    public AlertSettings settings() {
        return alertSettings.get();
    }

    @Override
    public AlertSettings updateSettings(int contractExpiryDays, int availabilityDays, UserContext user) {
        AlertSettings settings = new AlertSettings(contractExpiryDays, availabilityDays, clock.instant(), user.userId());
        if (!settings.valid()) {
            throw new CaseException(400, "Los días de anticipación deben estar entre 1 y 365");
        }
        return alertSettings.save(settings);
    }
}
