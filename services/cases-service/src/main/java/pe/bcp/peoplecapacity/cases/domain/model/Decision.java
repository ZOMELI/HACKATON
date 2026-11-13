package pe.bcp.peoplecapacity.cases.domain.model;

import java.time.Instant;
import java.util.List;

public record Decision(String optionId, String optionLabel, String comment, String decidedBy,
                       Instant decidedAt, List<String> ledgerClaimIds) {
}
