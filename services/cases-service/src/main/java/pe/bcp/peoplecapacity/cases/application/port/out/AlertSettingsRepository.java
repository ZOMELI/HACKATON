package pe.bcp.peoplecapacity.cases.application.port.out;

import pe.bcp.peoplecapacity.cases.domain.model.AlertSettings;

public interface AlertSettingsRepository {

    /** Devuelve los valores guardados o los valores por defecto. */
    AlertSettings get();

    AlertSettings save(AlertSettings settings);
}
