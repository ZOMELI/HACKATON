package pe.bcp.peoplecapacity.demand.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.bcp.peoplecapacity.demand.domain.NeedStatus;

interface NeedJpaRepository extends JpaRepository<NeedEntity, String> {

    Optional<NeedEntity> findByExternalId(String externalId);

    List<NeedEntity> findByStatus(NeedStatus status);
}
