package com.restate.demo.types;

import java.util.List;

public record ReconciliationReport(
        String merchantId,
        List<ProcessedPayment> unsettledPayments,
        List<ProcessedPayment> settledPayments,
        List<SettlementRecord> settlements) {}
