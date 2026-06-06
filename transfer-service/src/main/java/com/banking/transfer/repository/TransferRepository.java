package com.banking.transfer.repository;

import com.banking.transfer.entity.Transfer;
import com.banking.transfer.entity.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    List<Transfer> findByFromAccountId(UUID fromAccountId);

    List<Transfer> findByStatus(TransferStatus status);
}
