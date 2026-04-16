package com.restate.demo.types;

import java.util.List;

public record ClearingFile(List<ClearingEvent> events) {}
