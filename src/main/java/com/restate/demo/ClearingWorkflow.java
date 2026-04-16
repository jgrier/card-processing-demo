package com.restate.demo;

import com.restate.demo.types.*;
import dev.restate.sdk.Awakeable;
import dev.restate.sdk.WorkflowContext;
import dev.restate.sdk.annotation.Workflow;
import dev.restate.sdk.common.TerminalException;

import java.time.Instant;
import java.time.LocalDate;

@Workflow
public class ClearingWorkflow {

    @Workflow
    public ProcessedPayment run(WorkflowContext ctx, ClearingEvent event) {
        System.out.println("[ClearingWorkflow] Processing " + event.paymentId());

        // Step 1: Check authorization
        AuthResult auth = PaymentAuthClient.fromContext(ctx, event.authId()).getAuth().await();

        if (auth.status() == AuthStatus.NOT_FOUND) {
            // Auth doesn't exist yet — create an awakeable and wait.
            // PaymentAuth.authorize() will resolve it when the auth arrives.
            System.out.println("[ClearingWorkflow] Auth " + event.authId() + " not found, waiting...");
            Awakeable<AuthResult> awakeable = ctx.awakeable(AuthResult.class);
            PaymentAuthClient.fromContext(ctx, event.authId())
                    .onAuthReady(awakeable.id()).await();
            auth = awakeable.await();
        }

        if (auth.status() == AuthStatus.DECLINED) {
            throw new TerminalException(400, "Authorization " + event.authId() + " was declined: " + auth.reason());
        }

        System.out.println("[ClearingWorkflow] Auth " + event.authId() + " confirmed for " + event.paymentId());

        // Step 2: Call multi-rail API (side effect with durable retry)
        String multiRailRef = ctx.run(String.class,
                () -> MockMultiRailApi.debitCardholder(event.paymentId(), event.amount(), event.cardNetwork()));

        ProcessedPayment payment = new ProcessedPayment(
                event.paymentId(),
                event.merchantId(),
                event.amount(),
                event.currency(),
                event.cardNetwork(),
                multiRailRef,
                Instant.now().toString());

        // Step 3: Add to merchant settlement
        MerchantSettlementClient.fromContext(ctx, event.merchantId()).addPayment(payment).await();

        // Step 4: Register merchant in today's settlement window
        String today = ctx.run(String.class, () -> LocalDate.now().toString());
        SettlementWindowClient.fromContext(ctx, today).addMerchant(event.merchantId()).await();

        System.out.println("[ClearingWorkflow] Completed " + event.paymentId());
        return payment;
    }
}
