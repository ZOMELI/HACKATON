package com.peoplecapacity.ingestion.infrastructure.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SYNC_RUN")
public class SyncRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    public Long id;
    @Column(name = "STARTED_AT")
    public LocalDateTime startedAt;
    @Column(name = "FINISHED_AT")
    public LocalDateTime finishedAt;
    @Column(name = "STATUS")
    public String status;
    @Column(name = "CLAIMS_SENT")
    public int claimsSent;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "SYNC_SOURCE_RESULT", joinColumns = @JoinColumn(name = "SYNC_RUN_ID"))
    @OrderColumn(name = "ITEM_ORDER")
    public List<SourceResultEmbeddable> sources = new ArrayList<>();
}
