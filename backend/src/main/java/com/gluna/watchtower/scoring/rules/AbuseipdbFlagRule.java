package com.gluna.watchtower.scoring.rules;

import com.gluna.watchtower.scoring.Rule;
import com.gluna.watchtower.scoring.RuleContext;
import com.gluna.watchtower.scoring.RuleResult;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AbuseipdbFlagRule implements Rule {

    public static final String CODE = "ABUSEIPDB_FLAG";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public Optional<RuleResult> evaluate(RuleContext ctx) {
        Optional<Integer> maybeConfidence = intValue(ctx.intel().get("abuseConfidenceScore"));
        if (maybeConfidence.isEmpty()) {
            return Optional.empty();
        }

        int confidence = maybeConfidence.get();
        int points = Math.max(0, Math.min(40, Math.round(confidence * 0.4f)));
        if (points < 10) {
            return Optional.empty();
        }

        int totalReports = intValue(ctx.intel().get("totalReports")).orElse(0);
        return Optional.of(new RuleResult(
                CODE,
                points,
                "AbuseIPDB confidence " + confidence + ", " + totalReports + " reports"
        ));
    }

    private Optional<Integer> intValue(Object value) {
        if (value instanceof Number number) {
            return Optional.of(number.intValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Optional.of(Integer.parseInt(text));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
