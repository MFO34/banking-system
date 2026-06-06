package com.banking.transfer.controller;

import com.banking.transfer.dto.CreateTransferRequest;
import com.banking.transfer.dto.TransferResponse;
import com.banking.transfer.entity.TransferStatus;
import com.banking.transfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> executeTransfer(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreateTransferRequest request) {
        TransferResponse response = transferService.executeTransfer(idempotencyKey, request);
        HttpStatus status = switch (response.getStatus()) {
            case COMPLETED -> HttpStatus.CREATED;
            case FAILED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case COMPENSATION_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.ACCEPTED;
        };
        return ResponseEntity.status(status).body(response);
    }
}
