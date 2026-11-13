package pe.bcp.peoplecapacity.cases.application.port.in;

import java.util.List;
import pe.bcp.peoplecapacity.cases.domain.model.AlertSettings;
import pe.bcp.peoplecapacity.cases.domain.model.AvailabilityItem;
import pe.bcp.peoplecapacity.cases.domain.model.UserContext;

public interface AvailabilityUseCase {

    List<AvailabilityItem> availability(UserContext user);

    AlertSettings settings();

    AlertSettings updateSettings(int contractExpiryDays, int availabilityDays, UserContext user);
}
