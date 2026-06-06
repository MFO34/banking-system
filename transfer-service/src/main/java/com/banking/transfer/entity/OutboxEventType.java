package com.banking.transfer.entity;

public enum OutboxEventType {
    TRANSFER_PROCESSING,
    TRANSFER_COMPLETED,
    TRANSFER_FAILED,
    TRANSFER_COMPENSATION_FAILED
}
