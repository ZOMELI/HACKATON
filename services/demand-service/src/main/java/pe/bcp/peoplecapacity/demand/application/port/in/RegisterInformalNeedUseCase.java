package pe.bcp.peoplecapacity.demand.application.port.in;

import java.time.LocalDate;
import pe.bcp.peoplecapacity.demand.domain.Mechanism;

/** Registra una necesidad informal desde la web. */
public interface RegisterInformalNeedUseCase {

    NeedWithCandidates register(InformalNeedCommand command, String userId);

    record InformalNeedCommand(
            String tribe,
            String project,
            String role,
            String seniority,
            Mechanism mechanism,
            LocalDate neededFrom,
            String note) {
    }
}
