package com.banking.transfer.service;

import com.banking.transfer.entity.IdempotencyRecord;
import com.banking.transfer.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    // REQUIRES_NEW: dış transaction'ı askıya alır, yeni transaction açar, hemen commit eder
    // Bu sayede claim diğer transaction'lara görünür olur
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void claim(UUID key) {
        idempotencyRepository.saveAndFlush(new IdempotencyRecord(key));
    }
}
