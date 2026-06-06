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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String response;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public IdempotencyRecord(UUID idempotencyKey, String response) {
        this.idempotencyKey = idempotencyKey;
        this.response = response;
        this.createdAt = LocalDateTime.now();
    }
}
