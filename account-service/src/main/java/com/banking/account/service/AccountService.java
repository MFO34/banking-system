package com.banking.account.service;

import com.banking.account.dto.AccountMapper;
import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.entity.Account;
import com.banking.account.repository.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = accountMapper.toEntity(request);
        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    @Cacheable(value = "accounts", key = "#id")
    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + id));
        return accountMapper.toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByOwner(UUID ownerId) {
        return accountRepository.findByOwnerId(ownerId)
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @CacheEvict(value = "accounts", key = "#id")
    @Transactional
    public AccountResponse debit(UUID id, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + id));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));
        return accountMapper.toResponse(accountRepository.save(account));
    }

    @CacheEvict(value = "accounts", key = "#id")
    @Transactional
    public AccountResponse credit(UUID id, BigDecimal amount) {
        Account account = accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + id));

        account.setBalance(account.getBalance().add(amount));
        return accountMapper.toResponse(accountRepository.save(account));
    }
}
