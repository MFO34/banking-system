package com.banking.transfer.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class AccountClient {

    private final RestClient restClient;

    public AccountClient(@Value("${account.service.url}") String accountServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(accountServiceUrl)
                .build();
    }

    @CircuitBreaker(name = "accountService")
    public AccountDto debit(UUID accountId, BigDecimal amount) {
        return restClient.post()
                .uri("/accounts/{id}/debit?amount={amount}", accountId, amount)
                .retrieve()
                .body(AccountDto.class);
    }

    @CircuitBreaker(name = "accountService")
    public AccountDto credit(UUID accountId, BigDecimal amount) {
        return restClient.post()
                .uri("/accounts/{id}/credit?amount={amount}", accountId, amount)
                .retrieve()
                .body(AccountDto.class);
    }

    @Getter
    @Setter
    public static class AccountDto {
        private UUID id;
        private BigDecimal balance;
        private String currency;
        private String status;
        private LocalDateTime createdAt;
    }
}
