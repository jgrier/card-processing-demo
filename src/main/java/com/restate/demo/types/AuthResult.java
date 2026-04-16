package com.restate.demo.types;

public record AuthResult(String authId, AuthStatus status, String reason, String timestamp) {}
