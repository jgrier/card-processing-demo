package com.restate.demo.types;

public record ProcessedPayment(
        String paymentId,
        String merchantId,
        double amount,
        String currency,
        String cardNetwork,
        String multiRailReference,
        String processedAt) {}
