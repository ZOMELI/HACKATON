package pe.bcp.peoplecapacity.cases.application.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.bcp.peoplecapacity.cases.application.port.out.CaseSummaryPort;
import pe.bcp.peoplecapacity.cases.application.port.out.CaseSummaryPort.Drafted;
import pe.bcp.peoplecapacity.cases.domain.model.CaseFile;
import pe.bcp.peoplecapacity.cases.domain.model.CaseOption;
import pe.bcp.peoplecapacity.cases.domain.model.Claim;
import pe.bcp.peoplecapacity.cases.domain.model.Subjects;
import pe.bcp.peoplecapacity.cases.domain.model.Summary;

/**
 * Pide la redacción al agente de expediente y la valida. Si no responde o no cumple
 * (texto vacío, opciones distintas a las de la regla) usa una plantilla local y lo indica con mode=FALLBACK.
 */
@Component
public class SummaryWriter {

    private static final Logger log = LoggerFactory.getLogger(SummaryWriter.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Lima"));

    private final CaseSummaryPort agent;

    public SummaryWriter(CaseSummaryPort agent) {
        this.agent = agent;
    }

    public CaseFile write(CaseFile caseFile) {
        Optional<Drafted> drafted = agent.draft(caseFile).filter(d -> valid(d, caseFile));
        if (drafted.isEmpty()) {
            log.warn("Caso {}: sin redacción válida del agente de expediente; se usa la plantilla local", caseFile.id());
            return caseFile.withSummary(new Summary(template(caseFile), false, "FALLBACK"), caseFile.options());
        }
        Drafted d = drafted.get();
        List<CaseOption> options = caseFile.options().stream()
                .map(o -> o.withText(d.optionTexts().get(o.id()))).toList();
        return caseFile.withSummary(new Summary(d.summary(), d.aiGenerated(), d.mode()), options);
    }

    private static boolean valid(Drafted d, CaseFile caseFile) {
        Set<String> expected = caseFile.options().stream().map(CaseOption::id).collect(Collectors.toSet());
        return d.summary() != null && !d.summary().isBlank()
                && d.optionTexts() != null && d.optionTexts().keySet().equals(expected)
                && d.optionTexts().values().stream().noneMatch(t -> t == null || t.isBlank());
    }

    static String template(CaseFile c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.personName()).append(" (").append(c.personId()).append("): ").append(c.title()).append('.');
        for (Claim claim : c.claims()) {
            sb.append(' ').append(Subjects.label(claim.subject())).append(": ").append(claim.value())
                    .append(" según ").append(claim.source());
            if (claim.observedAt() != null) {
                sb.append(", consultado el ").append(DATE.format(claim.observedAt()));
            }
            sb.append('.');
        }
        sb.append(c.evidence().isEmpty() ? " No hay evidencia adjunta." : " Evidencia adjunta: " + c.evidence().size() + ".");
        sb.append(" Debe decidir ").append(c.ownerName()).append('.');
        return sb.toString();
    }
}
