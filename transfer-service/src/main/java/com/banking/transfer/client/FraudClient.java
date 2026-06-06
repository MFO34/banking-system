package com.banking.transfer.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class FraudClient {

    private final RestClient restClient;

    public FraudClient(@Value("${fraud.service.url}") String fraudServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(fraudServiceUrl)
                .build();
    }

    public FraudResult check(UUID transferId, UUID fromAccountId, UUID toAccountId,
                              BigDecimal amount, String currency) {
        return restClient.post()
                .uri("/fraud/check")
                .body(new FraudRequest(transferId, fromAccountId, toAccountId, amount, currency))
                .retrieve()
                .body(FraudResult.class);
    }

    record FraudRequest(UUID transferId, UUID fromAccountId, UUID toAccountId,
                        BigDecimal amount, String currency) {}

    @Getter
    @Setter
    public static class FraudResult {
        private boolean approved;
        private String reason;
    }
}
