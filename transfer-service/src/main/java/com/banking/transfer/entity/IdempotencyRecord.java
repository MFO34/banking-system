package com.banking.transfer.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records")
@Getter
@NoArgsConstructor
public class IdempotencyRecord {

    @Id
    private UUID idempotencyKey;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public IdempotencyRecord(UUID idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
        this.createdAt = LocalDateTime.now();
    }

    public void complete(String response) {
        this.response = response;
    }

    public boolean isCompleted() {
        return response != null;
    }
}
