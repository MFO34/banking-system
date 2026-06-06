package com.banking.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreateAccountRequest {

    @NotNull
    private UUID ownerId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal balance;

    @NotBlank
    private String currency;
}
