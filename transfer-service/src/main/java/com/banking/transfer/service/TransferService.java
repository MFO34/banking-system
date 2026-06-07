package com.banking.transfer.service;

import com.banking.transfer.client.AccountClient;
import com.banking.transfer.client.FraudClient;
import com.banking.transfer.dto.CreateTransferRequest;
import com.banking.transfer.dto.TransferMapper;
import com.banking.transfer.dto.TransferResponse;
import com.banking.transfer.entity.IdempotencyRecord;
import com.banking.transfer.entity.OutboxEvent;
import com.banking.transfer.entity.OutboxEventType;
import com.banking.transfer.entity.Transfer;
import com.banking.transfer.entity.TransferStatus;
import com.banking.transfer.repository.IdempotencyRepository;
import com.banking.transfer.repository.OutboxEventRepository;
import com.banking.transfer.repository.TransferRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final IdempotencyService idempotencyService;
    private final OutboxEventRepository outboxEventRepository;
    private final TransferMapper transferMapper;
    private final AccountClient accountClient;
    private final FraudClient fraudClient;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Transactional
    public TransferResponse executeTransfer(UUID idempotencyKey, CreateTransferRequest request) {
        // Tamamlanmış mı?
        IdempotencyRecord existing = idempotencyRepository.findById(idempotencyKey).orElse(null);
        if (existing != null) {
            if (existing.isCompleted()) {
                return deserialize(existing.getResponse());
            }
            // Key var ama response null — başka bir istek işliyor
            throw new IllegalStateException("Transfer with this idempotency key is already in progress");
        }

        // DB unique constraint'i mutex olarak kullan
        // REQUIRES_NEW → hemen commit olur → diğer eşzamanlı istek DataIntegrityViolationException alır
        try {
            idempotencyService.claim(idempotencyKey);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Transfer with this idempotency key is already in progress");
        }

        return processAndSave(idempotencyKey, request);
    }

    private TransferResponse processAndSave(UUID idempotencyKey, CreateTransferRequest request) {
        Transfer transfer = transferMapper.toEntity(request);
        transferRepository.save(transfer);

        FraudClient.FraudResult fraudResult = fraudClient.check(
                transfer.getId(),
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                request.getCurrency()
        );
        if (!fraudResult.isApproved()) {
            transfer.setStatus(TransferStatus.FAILED);
            transferRepository.save(transfer);
            log.warn("Transfer {} rejected by fraud service: {}", transfer.getId(), fraudResult.getReason());
            TransferResponse response = transferMapper.toResponse(transfer);
            completeIdempotencyRecord(idempotencyKey, response);
            return response;
        }

        boolean debitDone = false;
        try {
            accountClient.debit(request.getFromAccountId(), request.getAmount());
            debitDone = true;

            accountClient.credit(request.getToAccountId(), request.getAmount());
            transfer.setStatus(TransferStatus.COMPLETED);

        } catch (Exception e) {
            if (debitDone) {
                try {
                    accountClient.credit(request.getFromAccountId(), request.getAmount());
                    transfer.setStatus(TransferStatus.FAILED);
                    log.warn("Compensation succeeded for transfer {}", transfer.getId());
                } catch (Exception compensationEx) {
                    transfer.setStatus(TransferStatus.COMPENSATION_FAILED);
                    log.error("CRITICAL: Compensation failed for transfer {}. Manual intervention required!", transfer.getId());
                }
            } else {
                transfer.setStatus(TransferStatus.FAILED);
            }
        } finally {
            transferRepository.save(transfer);
        }

        TransferResponse response = transferMapper.toResponse(transfer);

        // Idempotency ve Outbox aynı transaction'da — ya ikisi commit olur ya ikisi rollback
        completeIdempotencyRecord(idempotencyKey, response);

        OutboxEventType eventType = switch (transfer.getStatus()) {
            case COMPLETED -> OutboxEventType.TRANSFER_COMPLETED;
            case FAILED -> OutboxEventType.TRANSFER_FAILED;
            case COMPENSATION_FAILED -> OutboxEventType.TRANSFER_COMPENSATION_FAILED;
            default -> OutboxEventType.TRANSFER_PROCESSING;
        };
        outboxEventRepository.save(new OutboxEvent(transfer.getId(), eventType, serialize(response)));

        return response;
    }

    private void completeIdempotencyRecord(UUID idempotencyKey, TransferResponse response) {
        IdempotencyRecord record = idempotencyRepository.findById(idempotencyKey).orElseThrow();
        record.complete(serialize(response));
        idempotencyRepository.save(record);
    }

    private String serialize(TransferResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    private TransferResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, TransferResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }
}
