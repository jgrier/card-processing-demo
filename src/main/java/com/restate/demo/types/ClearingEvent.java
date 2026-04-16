package com.restate.demo.types;

public record ClearingEvent(
        String paymentId,
        String authId,
        String merchantId,
        double amount,
        String currency,
        String cardNetwork,
        String timestamp) {}
