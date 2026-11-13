package com.peoplecapacity.ingestion.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SourceResultEmbeddable {

    @Column(name = "SOURCE")
    public String source;
    @Column(name = "STATUS")
    public String status;
    @Column(name = "RECORDS")
    public int records;
    @Column(name = "MESSAGE")
    public String message;
}
