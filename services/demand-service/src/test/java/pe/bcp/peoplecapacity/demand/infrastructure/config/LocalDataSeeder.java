package pe.bcp.peoplecapacity.demand.infrastructure.config;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pe.bcp.peoplecapacity.demand.application.port.in.UpsertNeedsUseCase;
import pe.bcp.peoplecapacity.demand.application.port.in.UpsertNeedsUseCase.NeedCommand;
import pe.bcp.peoplecapacity.demand.application.port.in.UpsertNeedsUseCase.RecruitmentCommand;
import pe.bcp.peoplecapacity.demand.domain.Mechanism;
import pe.bcp.peoplecapacity.demand.domain.NeedStatus;
import pe.bcp.peoplecapacity.demand.domain.RecruitmentStage;

/**
 * Solo perfil local: necesidades de ejemplo coherentes con contracts/examples/claims.json,
 * para que la demo tenga contenido antes de integrar ingestion. Es idempotente (upsert por externalId).
 */
@Component
@ConditionalOnProperty(name = "demand.seed-examples", havingValue = "true")
class LocalDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalDataSeeder.class);
    private static final String EXCEL = "EXCEL_RECRUITMENT";
    private static final Instant OBSERVED_AT = Instant.parse("2026-11-13T08:00:00Z");

    private final UpsertNeedsUseCase upsertNeeds;

    LocalDataSeeder(UpsertNeedsUseCase upsertNeeds) {
        this.upsertNeeds = upsertNeeds;
    }

    @Override
    public void run(String... args) {
        var result = upsertNeeds.upsert(List.of(
                new NeedCommand("REQ-2026-001", "Pagos", "Core de Pagos", "Backend Developer", "Senior",
                        Mechanism.EXTERNAL_HIRE, LocalDate.of(2026, 12, 1), NeedStatus.OPEN, EXCEL, OBSERVED_AT,
                        new RecruitmentCommand(RecruitmentStage.INTERVIEWS, EXCEL, OBSERVED_AT), null, null),
                new NeedCommand("REQ-2026-002", "Canales Digitales", "App Móvil", "Scrum Master", "Senior",
                        Mechanism.INTERNAL_MOVE, LocalDate.of(2026, 12, 1), NeedStatus.OPEN, EXCEL, OBSERVED_AT,
                        new RecruitmentCommand(RecruitmentStage.NOT_STARTED, EXCEL, OBSERVED_AT), null, null),
                new NeedCommand("REQ-2026-003", "Data", "Lakehouse Fase 2", "Data Engineer", "Semi Senior",
                        Mechanism.INTERNAL_MOVE, LocalDate.of(2026, 12, 1), NeedStatus.OPEN, EXCEL, OBSERVED_AT,
                        new RecruitmentCommand(RecruitmentStage.SOURCING, EXCEL, OBSERVED_AT), null, null),
                // Sin seguimiento en Reclutamiento: viene de un correo
                new NeedCommand("MAIL-2026-0117", "Riesgos", "Motor de Fraude", "QA Engineer", "Semi Senior",
                        Mechanism.UNDEFINED, LocalDate.of(2027, 1, 15), NeedStatus.OPEN, "EMAIL",
                        Instant.parse("2026-11-12T15:30:00Z"), null,
                        "Para enero vamos a necesitar un QA adicional en el Motor de Fraude.", null),
                new NeedCommand("REQ-2026-004", "Pagos", "Pasarela QR", "Backend Developer", "Semi Senior",
                        Mechanism.EXTERNAL_HIRE, LocalDate.of(2026, 11, 2), NeedStatus.COVERED, EXCEL, OBSERVED_AT,
                        new RecruitmentCommand(RecruitmentStage.HIRED, EXCEL, OBSERVED_AT), null, null)));
        log.info("Perfil local: necesidades de ejemplo sembradas (creadas={}, actualizadas={})",
                result.created(), result.updated());
    }
}
