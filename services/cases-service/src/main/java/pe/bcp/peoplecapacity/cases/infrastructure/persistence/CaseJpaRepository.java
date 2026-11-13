package pe.bcp.peoplecapacity.cases.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CaseJpaRepository extends JpaRepository<CaseEntity, String> {

    Optional<CaseEntity> findByDedupeKey(String dedupeKey);
}
