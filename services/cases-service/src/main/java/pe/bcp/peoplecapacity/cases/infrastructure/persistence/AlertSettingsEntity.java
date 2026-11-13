package pe.bcp.peoplecapacity.cases.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "alert_settings", schema = "cases")
public class AlertSettingsEntity {

    static final int SINGLETON_ID = 1;

    @Id
    private Integer id;
    @Column(name = "contract_expiry_days", nullable = false)
    private Integer contractExpiryDays;
    @Column(name = "availability_days", nullable = false)
    private Integer availabilityDays;
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Column(name = "updated_by", length = 40)
    private String updatedBy;

    protected AlertSettingsEntity() {
    }

    AlertSettingsEntity(int contractExpiryDays, int availabilityDays, Instant updatedAt, String updatedBy) {
        this.id = SINGLETON_ID;
        this.contractExpiryDays = contractExpiryDays;
        this.availabilityDays = availabilityDays;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    Integer getContractExpiryDays() {
        return contractExpiryDays;
    }

    Integer getAvailabilityDays() {
        return availabilityDays;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    String getUpdatedBy() {
        return updatedBy;
    }
}
