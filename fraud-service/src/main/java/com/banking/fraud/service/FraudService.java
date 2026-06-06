package com.banking.fraud.service;

import com.banking.fraud.dto.FraudCheckRequest;
import com.banking.fraud.dto.FraudCheckResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FraudService {

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("10000");
    private static final int MAX_TRANSFERS_PER_MINUTE = 3;

    private final Map<UUID, List<LocalDateTime>> transferHistory = new ConcurrentHashMap<>();

    public FraudCheckResponse check(FraudCheckRequest request) {
        if (request.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            log.warn("Fraud: high amount {} for transfer {}", request.getAmount(), request.getTransferId());
            return FraudCheckResponse.reject("Transfer amount exceeds limit: " + request.getAmount());
        }

        if (!"TRY".equalsIgnoreCase(request.getCurrency())) {
            log.warn("Fraud: foreign currency {} for transfer {}", request.getCurrency(), request.getTransferId());
            return FraudCheckResponse.reject("Foreign currency not allowed: " + request.getCurrency());
        }

        if (isHighFrequency(request.getFromAccountId())) {
            log.warn("Fraud: high frequency from account {}", request.getFromAccountId());
            return FraudCheckResponse.reject("Too many transfers in a short period");
        }

        recordTransfer(request.getFromAccountId());
        return FraudCheckResponse.approve();
    }

    private boolean isHighFrequency(UUID accountId) {
        List<LocalDateTime> times = transferHistory.getOrDefault(accountId, List.of());
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long recentCount = times.stream().filter(t -> t.isAfter(oneMinuteAgo)).count();
        return recentCount >= MAX_TRANSFERS_PER_MINUTE;
    }

    private void recordTransfer(UUID accountId) {
        transferHistory.computeIfAbsent(accountId, k -> new ArrayList<>()).add(LocalDateTime.now());
    }
}
