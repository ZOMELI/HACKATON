package pe.bcp.peoplecapacity.cases.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** El caso completo va como JSON en payload; las demás columnas sirven para buscar. */
@Entity
@Table(name = "case_file", schema = "cases")
public class CaseEntity {

    @Id
    @Column(length = 40)
    private String id;
    @Column(name = "dedupe_key", nullable = false, unique = true, length = 200)
    private String dedupeKey;
    @Column(nullable = false, length = 40)
    private String type;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "person_id", nullable = false, length = 120)
    private String personId;
    @Column(name = "owner_id", length = 20)
    private String ownerId;
    @Column(name = "due_date")
    private LocalDate dueDate;
    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    protected CaseEntity() {
    }

    public CaseEntity(String id, String dedupeKey, String type, String status, String personId, String ownerId,
                      LocalDate dueDate, Instant openedAt, String payload) {
        this.id = id;
        this.dedupeKey = dedupeKey;
        this.type = type;
        this.status = status;
        this.personId = personId;
        this.ownerId = ownerId;
        this.dueDate = dueDate;
        this.openedAt = openedAt;
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}
