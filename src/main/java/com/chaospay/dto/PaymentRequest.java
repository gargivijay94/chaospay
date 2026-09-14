package com.chaospay.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentRequest(
    @NotNull UUID fromAccount,
    @NotNull UUID toAccount,
    @NotNull @Min(1) Long amount,
    @NotBlank String currency
) {}
