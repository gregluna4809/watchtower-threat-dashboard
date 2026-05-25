package com.gluna.watchtower.api.controller;

import java.time.Duration;

final class ObservationWindows {

    private static final int MAX_BUCKETS = 500;

    private ObservationWindows() {
    }

    static ObservationWindow parse(String window) {
        String normalized = window == null || window.isBlank() ? "24h" : window.trim().toLowerCase();
        ObservationWindow observationWindow = switch (normalized) {
            case "1h" -> new ObservationWindow(Duration.ofHours(1), Duration.ofMinutes(1));
            case "24h" -> new ObservationWindow(Duration.ofHours(24), Duration.ofMinutes(5));
            case "7d" -> new ObservationWindow(Duration.ofDays(7), Duration.ofHours(1));
            case "30d" -> new ObservationWindow(Duration.ofDays(30), Duration.ofHours(6));
            default -> throw new IllegalArgumentException("window must be one of 1h, 24h, 7d, or 30d");
        };

        long bucketCount = observationWindow.window().toSeconds() / observationWindow.bucketSize().toSeconds();
        if (bucketCount > MAX_BUCKETS) {
            throw new IllegalArgumentException("window produces more than 500 buckets");
        }
        return observationWindow;
    }

    record ObservationWindow(Duration window, Duration bucketSize) {
    }
}
