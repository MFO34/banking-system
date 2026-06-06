package com.banking.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FraudCheckResponse {
    private boolean approved;
    private String reason;

    public static FraudCheckResponse approve() {
        return new FraudCheckResponse(true, null);
    }

    public static FraudCheckResponse reject(String reason) {
        return new FraudCheckResponse(false, reason);
    }
}
