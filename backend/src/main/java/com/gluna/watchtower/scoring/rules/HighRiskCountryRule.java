package com.gluna.watchtower.scoring.rules;

import com.gluna.watchtower.model.RemoteEndpoint;
import com.gluna.watchtower.scoring.Rule;
import com.gluna.watchtower.scoring.RuleContext;
import com.gluna.watchtower.scoring.RuleResult;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HighRiskCountryRule implements Rule {

    public static final String CODE = "HIGH_RISK_COUNTRY";

    private final Set<String> highRiskCountries;

    public HighRiskCountryRule(@Value("${watchtower.scoring.high-risk-countries}") Set<String> highRiskCountries) {
        this.highRiskCountries = highRiskCountries.stream()
                .map(country -> country.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public Optional<RuleResult> evaluate(RuleContext ctx) {
        RemoteEndpoint endpoint = ctx.endpoint();
        if (endpoint == null || endpoint.getCountryIso() == null || highRiskCountries.isEmpty()) {
            return Optional.empty();
        }

        String country = endpoint.getCountryIso().toUpperCase(Locale.ROOT);
        if (!highRiskCountries.contains(country)) {
            return Optional.empty();
        }

        return Optional.of(new RuleResult(
                CODE,
                15,
                "Remote endpoint geolocates to configured high-risk country " + country + "."
        ));
    }
}

