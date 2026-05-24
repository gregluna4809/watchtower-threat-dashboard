package com.gluna.watchtower.scoring;

public record RuleResult(String ruleCode, int points, String detail) {

    public RuleResult {
        if (points < 0) {
            throw new IllegalArgumentException("Rule points must be non-negative");
        }
    }
}

