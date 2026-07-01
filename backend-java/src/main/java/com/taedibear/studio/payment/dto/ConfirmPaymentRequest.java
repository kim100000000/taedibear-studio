package com.taedibear.studio.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConfirmPaymentRequest(
        @NotBlank String paymentKey,
        @NotBlank String orderId,
        @NotNull @Positive Integer amount
) {}
