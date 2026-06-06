package com.banking.account.controller;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable UUID id) {
        return accountService.getAccount(id);
    }

    @GetMapping("/owner/{ownerId}")
    public List<AccountResponse> getAccountsByOwner(@PathVariable UUID ownerId) {
        return accountService.getAccountsByOwner(ownerId);
    }

    @PostMapping("/{id}/debit")
    public AccountResponse debit(@PathVariable UUID id, @RequestParam BigDecimal amount) {
        return accountService.debit(id, amount);
    }

    @PostMapping("/{id}/credit")
    public AccountResponse credit(@PathVariable UUID id, @RequestParam BigDecimal amount) {
        return accountService.credit(id, amount);
    }
}
