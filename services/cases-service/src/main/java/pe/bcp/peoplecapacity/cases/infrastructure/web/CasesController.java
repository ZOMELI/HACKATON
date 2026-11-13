package pe.bcp.peoplecapacity.cases.infrastructure.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.bcp.peoplecapacity.cases.application.CaseException;
import pe.bcp.peoplecapacity.cases.application.port.in.AvailabilityUseCase;
import pe.bcp.peoplecapacity.cases.application.port.in.CaseUseCase;
import pe.bcp.peoplecapacity.cases.application.port.in.ReconcileUseCase;
import pe.bcp.peoplecapacity.cases.domain.model.AlertSettings;
import pe.bcp.peoplecapacity.cases.domain.model.AvailabilityItem;
import pe.bcp.peoplecapacity.cases.domain.model.CaseStatus;
import pe.bcp.peoplecapacity.cases.domain.model.CaseType;
import pe.bcp.peoplecapacity.cases.domain.model.UserContext;

@RestController
public class CasesController {

    private static final String USER_ID = "X-User-Id";
    private static final String USER_ROLE = "X-User-Role";

    private final ReconcileUseCase reconcile;
    private final CaseUseCase cases;
    private final AvailabilityUseCase availability;

    public CasesController(ReconcileUseCase reconcile, CaseUseCase cases, AvailabilityUseCase availability) {
        this.reconcile = reconcile;
        this.cases = cases;
        this.availability = availability;
    }

    @PostMapping("/reconciliations")
    public ReconcileUseCase.Result reconcile(@RequestBody(required = false) ReconciliationRequest request) {
        return reconcile.reconcile(request == null ? null : request.syncId());
    }

    @GetMapping("/cases")
    public List<CaseResponse> list(@RequestHeader(value = USER_ID, required = false) String userId,
                                   @RequestHeader(value = USER_ROLE, required = false) String role,
                                   @RequestParam(required = false) CaseStatus status,
                                   @RequestParam(required = false) CaseType type,
                                   @RequestParam(required = false) String personId) {
        return cases.list(new UserContext(userId, role), status, type, personId).stream().map(CaseResponse::of).toList();
    }

    @GetMapping("/cases/{id}")
    public CaseResponse get(@PathVariable String id) {
        return CaseResponse.of(cases.get(id));
    }

    @PostMapping("/cases/{id}/decisions")
    public CaseResponse decide(@PathVariable String id,
                               @RequestHeader(value = USER_ID, required = false) String userId,
                               @RequestHeader(value = USER_ROLE, required = false) String role,
                               @RequestBody DecisionRequest request) {
        if (request.optionId() == null || request.optionId().isBlank()) {
            throw new CaseException(400, "Falta optionId");
        }
        return CaseResponse.of(cases.decide(id, new UserContext(userId, role), request.optionId(), request.comment()));
    }

    @GetMapping("/availability")
    public List<AvailabilityItem> availability(@RequestHeader(value = USER_ID, required = false) String userId,
                                               @RequestHeader(value = USER_ROLE, required = false) String role) {
        return availability.availability(new UserContext(userId, role));
    }

    @GetMapping("/alert-settings")
    public AlertSettings settings() {
        return availability.settings();
    }

    @PutMapping("/alert-settings")
    public AlertSettings updateSettings(@RequestHeader(value = USER_ID, required = false) String userId,
                                        @RequestHeader(value = USER_ROLE, required = false) String role,
                                        @RequestBody SettingsRequest request) {
        if (request.contractExpiryDays() == null || request.availabilityDays() == null) {
            throw new CaseException(400, "Faltan contractExpiryDays y availabilityDays");
        }
        return availability.updateSettings(request.contractExpiryDays(), request.availabilityDays(), new UserContext(userId, role));
    }

    record ReconciliationRequest(String syncId) {
    }

    record DecisionRequest(String optionId, String comment) {
    }

    record SettingsRequest(Integer contractExpiryDays, Integer availabilityDays) {
    }
}
