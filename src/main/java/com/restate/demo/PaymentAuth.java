package com.restate.demo;

import com.restate.demo.types.AuthRequest;
import com.restate.demo.types.AuthResult;
import com.restate.demo.types.AuthStatus;
import dev.restate.sdk.ObjectContext;
import dev.restate.sdk.SharedObjectContext;
import dev.restate.sdk.annotation.Handler;
import dev.restate.sdk.annotation.Shared;
import dev.restate.sdk.annotation.VirtualObject;
import dev.restate.sdk.common.StateKey;
import dev.restate.serde.TypeTag;
import dev.restate.serde.TypeRef;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@VirtualObject
public class PaymentAuth {

    private static final StateKey<AuthResult> AUTH = StateKey.of("auth", AuthResult.class);
    private static final StateKey<List<String>> WAITERS =
            StateKey.of("waiters", TypeTag.of(new TypeRef<List<String>>() {}));

    @Handler
    public AuthResult authorize(ObjectContext ctx, AuthRequest request) {
        String authId = ctx.key();

        AuthStatus status;
        String reason;
        if (request.amount() > 10_000) {
            status = AuthStatus.DECLINED;
            reason = "Amount exceeds limit";
        } else {
            status = AuthStatus.APPROVED;
            reason = "Approved";
        }

        AuthResult result = new AuthResult(authId, status, reason, Instant.now().toString());
        ctx.set(AUTH, result);

        // Wake up any workflows waiting for this auth
        List<String> waiters = ctx.get(WAITERS).orElse(List.of());
        for (String awakeableId : waiters) {
            ctx.awakeableHandle(awakeableId).resolve(AuthResult.class, result);
        }
        ctx.clear(WAITERS);

        System.out.println("[PaymentAuth] " + authId + " -> " + status + " (" + reason + ")"
                + (waiters.isEmpty() ? "" : ", woke " + waiters.size() + " waiter(s)"));
        return result;
    }

    @Shared
    @Handler
    public AuthResult getAuth(SharedObjectContext ctx) {
        return ctx.get(AUTH).orElse(
                new AuthResult(ctx.key(), AuthStatus.NOT_FOUND, "No authorization on record", Instant.now().toString()));
    }

    @Handler
    public void onAuthReady(ObjectContext ctx, String awakeableId) {
        AuthResult existing = ctx.get(AUTH).orElse(null);
        if (existing != null) {
            // Auth already exists — resolve immediately
            ctx.awakeableHandle(awakeableId).resolve(AuthResult.class, existing);
        } else {
            // Store the awakeable ID — authorize() will resolve it later
            List<String> waiters = new ArrayList<>(ctx.get(WAITERS).orElse(List.of()));
            waiters.add(awakeableId);
            ctx.set(WAITERS, waiters);
            System.out.println("[PaymentAuth] " + ctx.key() + " registered waiter (total: " + waiters.size() + ")");
        }
    }
}
