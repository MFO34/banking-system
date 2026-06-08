package com.banking.transfer.service;

import com.banking.transfer.client.AccountClient;
import com.banking.transfer.client.FraudClient;
import com.banking.transfer.dto.CreateTransferRequest;
import com.banking.transfer.dto.TransferMapper;
import com.banking.transfer.dto.TransferResponse;
import com.banking.transfer.entity.IdempotencyRecord;
import com.banking.transfer.entity.Transfer;
import com.banking.transfer.entity.TransferStatus;
import com.banking.transfer.repository.IdempotencyRepository;
import com.banking.transfer.repository.OutboxEventRepository;
import com.banking.transfer.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock private TransferRepository transferRepository;
    @Mock private IdempotencyRepository idempotencyRepository;
    @Mock private IdempotencyService idempotencyService;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private TransferMapper transferMapper;
    @Mock private AccountClient accountClient;
    @Mock private FraudClient fraudClient;

    @InjectMocks
    private TransferService transferService;

    private UUID idempotencyKey;
    private CreateTransferRequest request;
    private Transfer transfer;
    private TransferResponse response;

    @BeforeEach
    void setUp() {
        idempotencyKey = UUID.randomUUID();

        request = new CreateTransferRequest();
        request.setFromAccountId(UUID.randomUUID());
        request.setToAccountId(UUID.randomUUID());
        request.setAmount(new BigDecimal("500.00"));
        request.setCurrency("TRY");

        transfer = new Transfer();
        // reflection ile id ve status set ediyoruz (entity private field)
        setField(transfer, "id", UUID.randomUUID());
        setField(transfer, "status", TransferStatus.PENDING);

        response = new TransferResponse();
        response.setId(transfer.getId());
        response.setStatus(TransferStatus.COMPLETED);
        response.setAmount(request.getAmount());
    }

    // --- 1. Idempotency: aynı key tekrar gelince cached response döner ---

    @Test
    void executeTransfer_whenKeyAlreadyCompleted_returnsCachedResponse() {
        IdempotencyRecord existingRecord = new IdempotencyRecord(idempotencyKey);
        String cachedJson = """
                {"id":"%s","status":"COMPLETED","amount":500.00,"currency":"TRY"}
                """.formatted(UUID.randomUUID());
        setField(existingRecord, "response", cachedJson);

        when(idempotencyRepository.findById(idempotencyKey)).thenReturn(Optional.of(existingRecord));

        TransferResponse result = transferService.executeTransfer(idempotencyKey, request);

        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        verify(accountClient, never()).debit(any(), any());
        verify(accountClient, never()).credit(any(), any());
    }

    // --- 2. Fraud rejection: fraud service reddederse FAILED döner, para hareket etmez ---

    @Test
    void executeTransfer_whenFraudRejected_returnsFailed() {
        when(idempotencyRepository.findById(idempotencyKey)).thenReturn(Optional.empty());
        when(transferMapper.toEntity(request)).thenReturn(transfer);
        when(transferRepository.save(any())).thenReturn(transfer);

        FraudClient.FraudResult rejected = new FraudClient.FraudResult();
        rejected.setApproved(false);
        rejected.setReason("Amount exceeds limit");
        when(fraudClient.check(any(), any(), any(), any(), any())).thenReturn(rejected);

        TransferResponse failedResponse = new TransferResponse();
        failedResponse.setStatus(TransferStatus.FAILED);
        when(transferMapper.toResponse(transfer)).thenReturn(failedResponse);
        when(idempotencyRepository.findById(idempotencyKey)).thenReturn(Optional.empty())
                .thenReturn(Optional.of(new IdempotencyRecord(idempotencyKey)));

        TransferResponse result = transferService.executeTransfer(idempotencyKey, request);

        assertThat(result.getStatus()).isEqualTo(TransferStatus.FAILED);
        verify(accountClient, never()).debit(any(), any());
        verify(accountClient, never()).credit(any(), any());
    }

    // --- 3. Happy path: debit + credit başarılı → COMPLETED ---

    @Test
    void executeTransfer_whenSuccess_returnsCompleted() {
        when(idempotencyRepository.findById(idempotencyKey)).thenReturn(Optional.empty());
        when(transferMapper.toEntity(request)).thenReturn(transfer);
        when(transferRepository.save(any())).thenReturn(transfer);

        FraudClient.FraudResult approved = new FraudClient.FraudResult();
        approved.setApproved(true);
        when(fraudClient.check(any(), any(), any(), any(), any())).thenReturn(approved);

        when(accountClient.debit(any(), any())).thenReturn(null);
        when(accountClient.credit(any(), any())).thenReturn(null);

        TransferResponse completedResponse = new TransferResponse();
        completedResponse.setStatus(TransferStatus.COMPLETED);
        when(transferMapper.toResponse(transfer)).thenReturn(completedResponse);

        IdempotencyRecord record = new IdempotencyRecord(idempotencyKey);
        when(idempotencyRepository.findById(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(record));

        TransferResponse result = transferService.executeTransfer(idempotencyKey, request);

        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        verify(accountClient).debit(request.getFromAccountId(), request.getAmount());
        verify(accountClient).credit(request.getToAccountId(), request.getAmount());
    }

    // --- 4. Debit başarılı, credit başarısız → compensation → FAILED ---

    @Test
    void executeTransfer_whenCreditFails_compensatesAndReturnsFailed() {
        when(idempotencyRepository.findById(idempotencyKey)).thenReturn(Optional.empty());
        when(transferMapper.toEntity(request)).thenReturn(transfer);
        when(transferRepository.save(any())).thenReturn(transfer);

        FraudClient.FraudResult approved = new FraudClient.FraudResult();
        approved.setApproved(true);
        when(fraudClient.check(any(), any(), any(), any(), any())).thenReturn(approved);

        when(accountClient.debit(any(), any())).thenReturn(null);
        when(accountClient.credit(eq(request.getToAccountId()), any())).thenThrow(new RuntimeException("credit failed"));
        when(accountClient.credit(eq(request.getFromAccountId()), any())).thenReturn(null); // compensation

        TransferResponse failedResponse = new TransferResponse();
        failedResponse.setStatus(TransferStatus.FAILED);
        when(transferMapper.toResponse(transfer)).thenReturn(failedResponse);

        IdempotencyRecord record = new IdempotencyRecord(idempotencyKey);
        when(idempotencyRepository.findById(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(record));

        TransferResponse result = transferService.executeTransfer(idempotencyKey, request);

        assertThat(result.getStatus()).isEqualTo(TransferStatus.FAILED);
        // Compensation: fromAccount'a geri credit yapıldı mı?
        verify(accountClient).credit(request.getFromAccountId(), request.getAmount());
    }

    // --- 5. Compensation da başarısız → COMPENSATION_FAILED ---

    @Test
    void executeTransfer_whenCompensationAlsoFails_returnsCompensationFailed() {
        when(idempotencyRepository.findById(idempotencyKey)).thenReturn(Optional.empty());
        when(transferMapper.toEntity(request)).thenReturn(transfer);
        when(transferRepository.save(any())).thenReturn(transfer);

        FraudClient.FraudResult approved = new FraudClient.FraudResult();
        approved.setApproved(true);
        when(fraudClient.check(any(), any(), any(), any(), any())).thenReturn(approved);

        when(accountClient.debit(any(), any())).thenReturn(null);
        // credit her iki çağrıda da (toAccount ve compensation) başarısız
        when(accountClient.credit(any(), any())).thenThrow(new RuntimeException("credit failed"));

        TransferResponse compensationFailedResponse = new TransferResponse();
        compensationFailedResponse.setStatus(TransferStatus.COMPENSATION_FAILED);
        when(transferMapper.toResponse(transfer)).thenReturn(compensationFailedResponse);

        IdempotencyRecord record = new IdempotencyRecord(idempotencyKey);
        when(idempotencyRepository.findById(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(record));

        TransferResponse result = transferService.executeTransfer(idempotencyKey, request);

        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPENSATION_FAILED);
    }

    // Helper: private field'lara reflection ile değer yaz
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Field set failed: " + fieldName, e);
        }
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) return findField(clazz.getSuperclass(), name);
            throw e;
        }
    }
}
