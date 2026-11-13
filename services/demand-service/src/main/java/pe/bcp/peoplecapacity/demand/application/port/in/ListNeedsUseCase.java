package pe.bcp.peoplecapacity.demand.application.port.in;

import java.util.List;
import pe.bcp.peoplecapacity.demand.domain.NeedStatus;

/** Tablero de demanda con candidatos ordenados. */
public interface ListNeedsUseCase {

    /** @param status filtro opcional; null devuelve todas */
    List<NeedWithCandidates> list(NeedStatus status);
}
