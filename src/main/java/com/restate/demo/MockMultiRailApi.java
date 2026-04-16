package com.restate.demo;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class MockMultiRailApi {

    public static String debitCardholder(String paymentId, double amount, String cardNetwork) {
        simulateLatency();
        if (ThreadLocalRandom.current().nextDouble() < 0.50) {
            System.out.println("[MultiRail] FAILED debit for " + paymentId + " via " + cardNetwork + " — transient network error, will retry");
            throw new RuntimeException("Multi-rail debit failed for " + paymentId + " on " + cardNetwork + " — transient network error");
        }
        String ref = "RAIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.println("[MultiRail] Debited " + amount + " for " + paymentId + " via " + cardNetwork + " -> " + ref);
        return ref;
    }

    public static void creditMerchant(String merchantId, double amount) {
        simulateLatency();
        if (ThreadLocalRandom.current().nextDouble() < 0.50) {
            System.out.println("[MultiRail] FAILED credit for merchant " + merchantId + " — transient network error, will retry");
            throw new RuntimeException("Multi-rail credit failed for merchant " + merchantId + " — transient network error");
        }
        System.out.println("[MultiRail] Credited " + amount + " to merchant " + merchantId);
    }

    private static void simulateLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(50, 200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
