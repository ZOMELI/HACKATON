package pe.bcp.peoplecapacity.cases.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface AlertSettingsJpaRepository extends JpaRepository<AlertSettingsEntity, Integer> {
}
