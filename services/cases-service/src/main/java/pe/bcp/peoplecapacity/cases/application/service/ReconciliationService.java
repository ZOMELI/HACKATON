package pe.bcp.peoplecapacity.cases.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.bcp.peoplecapacity.cases.application.port.in.ReconcileUseCase;
import pe.bcp.peoplecapacity.cases.application.port.out.AlertSettingsRepository;
import pe.bcp.peoplecapacity.cases.application.port.out.CaseRepository;
import pe.bcp.peoplecapacity.cases.application.port.out.LedgerPort;
import pe.bcp.peoplecapacity.cases.domain.model.CaseFile;
import pe.bcp.peoplecapacity.cases.domain.model.ClaimIndex;
import pe.bcp.peoplecapacity.cases.domain.model.DetectedCase;
import pe.bcp.peoplecapacity.cases.domain.rules.ReconciliationRules;

/** Lee el libro, aplica las reglas y abre los casos que aún no se conocen. Nunca cierra ni decide. */
@Service
public class ReconciliationService implements ReconcileUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final LedgerPort ledger;
    private final CaseRepository cases;
    private final AlertSettingsRepository alertSettings;
    private final SummaryWriter summaryWriter;
    private final Clock clock;
    private final String defaultOwnerId;

    public ReconciliationService(LedgerPort ledger, CaseRepository cases, AlertSettingsRepository alertSettings,
                                 SummaryWriter summaryWriter, Clock clock,
                                 @Value("${cases.default-owner-id}") String defaultOwnerId) {
        this.ledger = ledger;
        this.cases = cases;
        this.alertSettings = alertSettings;
        this.summaryWriter = summaryWriter;
        this.clock = clock;
        this.defaultOwnerId = defaultOwnerId;
    }

    @Override
    public synchronized Result reconcile(String syncId) {
        ClaimIndex idx = new ClaimIndex(ledger.currentClaims());
        List<DetectedCase> detected = ReconciliationRules.detect(idx, LocalDate.now(clock), alertSettings.get(), defaultOwnerId);
        List<String> opened = new ArrayList<>();
        int known = 0;
        for (DetectedCase d : detected) {
            // Un caso ya abierto o ya decidido no se vuelve a abrir: la decisión del responsable se respeta.
            if (cases.findByDedupeKey(d.dedupeKey()).isPresent()) {
                known++;
                continue;
            }
            String id = "CASE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            CaseFile caseFile = summaryWriter.write(CaseFile.open(id, d, clock.instant()));
            cases.save(caseFile);
            opened.add(id);
        }
        log.info("Conciliación {}: {} afirmaciones, {} casos abiertos, {} ya conocidos", syncId, idx.size(), opened.size(), known);
        return new Result("REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), clock.instant(),
                idx.size(), opened.size(), known, opened);
    }
}
