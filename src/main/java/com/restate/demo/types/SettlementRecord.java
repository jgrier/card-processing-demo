package com.restate.demo.types;

import java.util.List;

public record SettlementRecord(
        String settlementId,
        String merchantId,
        List<String> paymentIds,
        double grossAmount,
        double fees,
        double netAmount,
        String currency,
        String settledAt) {}
