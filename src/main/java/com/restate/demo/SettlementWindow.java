package com.restate.demo;

import com.restate.demo.types.SettlementRecord;
import dev.restate.sdk.DurableFuture;
import dev.restate.sdk.ObjectContext;
import dev.restate.sdk.annotation.Handler;
import dev.restate.sdk.annotation.VirtualObject;
import dev.restate.sdk.common.StateKey;
import dev.restate.serde.TypeTag;
import dev.restate.serde.TypeRef;

import java.util.*;

@VirtualObject
public class SettlementWindow {

    private static final StateKey<Set<String>> MERCHANTS =
            StateKey.of("merchants", TypeTag.of(new TypeRef<Set<String>>() {}));

    @Handler
    public void addMerchant(ObjectContext ctx, String merchantId) {
        Set<String> merchants = new HashSet<>(ctx.get(MERCHANTS).orElse(Set.of()));
        merchants.add(merchantId);
        ctx.set(MERCHANTS, merchants);
        System.out.println("[SettlementWindow] " + ctx.key() + " registered merchant " + merchantId
                + " (total: " + merchants.size() + ")");
    }

    @Handler
    public List<SettlementRecord> triggerSettlement(ObjectContext ctx) {
        Set<String> merchants = ctx.get(MERCHANTS).orElse(Set.of());
        System.out.println("[SettlementWindow] " + ctx.key() + " triggering settlement for " + merchants.size() + " merchants");

        if (merchants.isEmpty()) {
            return List.of();
        }

        // Fan out settle() calls as parallel futures
        List<DurableFuture<?>> futures = new ArrayList<>();
        List<DurableFuture<SettlementRecord>> typed = new ArrayList<>();
        for (String merchantId : merchants) {
            DurableFuture<SettlementRecord> f = MerchantSettlementClient.fromContext(ctx, merchantId).settle();
            futures.add(f);
            typed.add(f);
        }

        // Await all in parallel
        DurableFuture.all(futures).await();

        List<SettlementRecord> results = new ArrayList<>();
        for (DurableFuture<SettlementRecord> future : typed) {
            results.add(future.await());
        }

        System.out.println("[SettlementWindow] " + ctx.key() + " settlement complete: " + results.size() + " merchants settled");
        return results;
    }
}
