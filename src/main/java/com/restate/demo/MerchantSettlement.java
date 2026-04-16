package com.restate.demo;

import com.restate.demo.types.ProcessedPayment;
import com.restate.demo.types.ReconciliationReport;
import com.restate.demo.types.SettlementRecord;
import dev.restate.sdk.ObjectContext;
import dev.restate.sdk.SharedObjectContext;
import dev.restate.sdk.annotation.Handler;
import dev.restate.sdk.annotation.Shared;
import dev.restate.sdk.annotation.VirtualObject;
import dev.restate.sdk.common.StateKey;
import dev.restate.sdk.common.TerminalException;
import dev.restate.serde.TypeTag;
import dev.restate.serde.TypeRef;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@VirtualObject
public class MerchantSettlement {

    private static final StateKey<List<ProcessedPayment>> UNSETTLED =
            StateKey.of("unsettled", TypeTag.of(new TypeRef<List<ProcessedPayment>>() {}));
    private static final StateKey<List<ProcessedPayment>> SETTLED =
            StateKey.of("settled", TypeTag.of(new TypeRef<List<ProcessedPayment>>() {}));
    private static final StateKey<List<SettlementRecord>> SETTLEMENTS =
            StateKey.of("settlements", TypeTag.of(new TypeRef<List<SettlementRecord>>() {}));

    @Handler
    public void addPayment(ObjectContext ctx, ProcessedPayment payment) {
        List<ProcessedPayment> unsettled = new ArrayList<>(ctx.get(UNSETTLED).orElse(List.of()));
        unsettled.add(payment);
        ctx.set(UNSETTLED, unsettled);
        System.out.println("[MerchantSettlement] " + ctx.key() + " added payment " + payment.paymentId()
                + " (unsettled: " + unsettled.size() + ")");
    }

    @Handler
    public SettlementRecord settle(ObjectContext ctx) {
        String merchantId = ctx.key();
        List<ProcessedPayment> unsettled = ctx.get(UNSETTLED).orElse(List.of());

        if (unsettled.isEmpty()) {
            throw new TerminalException(400, "No unsettled payments for merchant " + merchantId);
        }

        // Calculate totals
        double grossAmount = unsettled.stream().mapToDouble(ProcessedPayment::amount).sum();
        double fees = grossAmount * 0.021 + unsettled.size() * 0.10; // 2.1% discount rate + $0.10/txn
        double netAmount = grossAmount - fees;
        List<String> paymentIds = unsettled.stream().map(ProcessedPayment::paymentId).toList();

        // Credit merchant via multi-rail
        ctx.run(() -> MockMultiRailApi.creditMerchant(merchantId, netAmount));

        SettlementRecord record = new SettlementRecord(
                "SETTLE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                merchantId, paymentIds, grossAmount, fees, netAmount,
                unsettled.getFirst().currency(), Instant.now().toString());

        // Move unsettled -> settled, record settlement
        List<ProcessedPayment> settled = new ArrayList<>(ctx.get(SETTLED).orElse(List.of()));
        settled.addAll(unsettled);
        ctx.set(SETTLED, settled);
        ctx.set(UNSETTLED, List.of());

        List<SettlementRecord> settlements = new ArrayList<>(ctx.get(SETTLEMENTS).orElse(List.of()));
        settlements.add(record);
        ctx.set(SETTLEMENTS, settlements);

        System.out.println("[MerchantSettlement] Settled " + merchantId + ": gross=" + grossAmount
                + " fees=" + String.format("%.2f", fees) + " net=" + String.format("%.2f", netAmount));
        return record;
    }

    @Shared
    @Handler
    public ReconciliationReport getReconciliation(SharedObjectContext ctx) {
        return new ReconciliationReport(
                ctx.key(),
                ctx.get(UNSETTLED).orElse(List.of()),
                ctx.get(SETTLED).orElse(List.of()),
                ctx.get(SETTLEMENTS).orElse(List.of()));
    }
}
