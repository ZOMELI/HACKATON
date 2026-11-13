package pe.bcp.peoplecapacity.demand.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import pe.bcp.peoplecapacity.demand.domain.Mechanism;
import pe.bcp.peoplecapacity.demand.domain.Need;
import pe.bcp.peoplecapacity.demand.domain.NeedStatus;
import pe.bcp.peoplecapacity.demand.domain.RecruitmentStage;
import pe.bcp.peoplecapacity.demand.domain.RecruitmentStatus;

/** Fila de demand.needs (el esquema viene de hibernate.default_schema). */
@Entity
@Table(name = "needs")
public class NeedEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "external_id", length = 100, unique = true)
    private String externalId;

    @Column(nullable = false)
    private boolean informal;

    @Column(length = 150)
    private String tribe;

    @Column(length = 200)
    private String project;

    @Column(name = "role_name", nullable = false, length = 150)
    private String role;

    @Column(length = 60)
    private String seniority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Mechanism mechanism;

    @Column(name = "needed_from")
    private LocalDate neededFrom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NeedStatus status;

    @Column(nullable = false, length = 60)
    private String source;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "recruitment_status", length = 20)
    private RecruitmentStage recruitmentStatus;

    @Column(name = "recruitment_source", length = 60)
    private String recruitmentSource;

    @Column(name = "recruitment_observed_at")
    private Instant recruitmentObservedAt;

    @Column(length = 2000)
    private String note;

    @Column(name = "evidence_quote", length = 2000)
    private String evidenceQuote;

    @Column(name = "created_by", length = 60)
    private String createdBy;

    /** Momento de la última escritura en este servicio (no confundir con observedAt, que es de la fuente). */
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected NeedEntity() {
    }

    static NeedEntity fromDomain(Need need) {
        NeedEntity e = new NeedEntity();
        e.id = need.id();
        e.externalId = need.externalId();
        e.informal = need.informal();
        e.tribe = need.tribe();
        e.project = need.project();
        e.role = need.role();
        e.seniority = need.seniority();
        e.mechanism = need.mechanism();
        e.neededFrom = need.neededFrom();
        e.status = need.status();
        e.source = need.source();
        e.observedAt = need.observedAt();
        if (need.recruitment() != null) {
            e.recruitmentStatus = need.recruitment().status();
            e.recruitmentSource = need.recruitment().source();
            e.recruitmentObservedAt = need.recruitment().observedAt();
        }
        e.note = need.note();
        e.evidenceQuote = need.evidenceQuote();
        e.createdBy = need.createdBy();
        e.recordedAt = Instant.now();
        return e;
    }

    Need toDomain() {
        RecruitmentStatus recruitment = recruitmentStatus == null
                ? null
                : new RecruitmentStatus(recruitmentStatus, recruitmentSource, recruitmentObservedAt);
        return new Need(id, externalId, informal, tribe, project, role, seniority, mechanism, neededFrom,
                status, source, observedAt, recruitment, note, evidenceQuote, createdBy);
    }

    @PrePersist
    @PreUpdate
    void touch() {
        recordedAt = Instant.now();
    }
}
