package pe.bcp.peoplecapacity.cases.application.port.in;

import java.time.Instant;
import java.util.List;

public interface ReconcileUseCase {

    Result reconcile(String syncId);

    record Result(String reconciliationId, Instant ranAt, int claimsRead, int casesOpened,
                  int casesAlreadyKnown, List<String> openedCaseIds) {
    }
}
