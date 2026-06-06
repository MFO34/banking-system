package com.banking.transfer.repository;

import com.banking.transfer.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, UUID> {
}
