package pe.bcp.peoplecapacity.demand.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.bcp.peoplecapacity.demand.domain.Need;
import pe.bcp.peoplecapacity.demand.domain.NeedStatus;

public interface NeedRepository {

    Optional<Need> findByExternalId(String externalId);

    Need save(Need need);

    /** @param status filtro opcional; null devuelve todas */
    List<Need> findAll(NeedStatus status);
}
