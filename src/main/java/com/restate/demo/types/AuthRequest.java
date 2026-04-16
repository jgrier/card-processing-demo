package com.restate.demo.types;

public record AuthRequest(String merchantId, String cardNumber, double amount, String currency) {}
