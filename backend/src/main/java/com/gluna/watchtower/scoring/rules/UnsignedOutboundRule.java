package com.gluna.watchtower.scoring.rules;

import com.gluna.watchtower.enrichment.GeoIpService;
import com.gluna.watchtower.model.ConnectionState;
import com.gluna.watchtower.model.ProcessEntity;
import com.gluna.watchtower.model.RemoteEndpoint;
import com.gluna.watchtower.scoring.Rule;
import com.gluna.watchtower.scoring.RuleContext;
import com.gluna.watchtower.scoring.RuleResult;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UnsignedOutboundRule implements Rule {

    public static final String CODE = "UNSIGNED_OUTBOUND";

    private final GeoIpService geoIpService;

    public UnsignedOutboundRule(GeoIpService geoIpService) {
        this.geoIpService = geoIpService;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public Optional<RuleResult> evaluate(RuleContext ctx) {
        ProcessEntity process = ctx.process();
        RemoteEndpoint endpoint = ctx.endpoint();
        if (process == null
                || endpoint == null
                || !Boolean.FALSE.equals(process.getSigned())
                || ctx.connection().getState() != ConnectionState.ESTABLISHED
                || !geoIpService.isPublicAddress(endpoint.getIp())) {
            return Optional.empty();
        }

        return Optional.of(new RuleResult(
                CODE,
                20,
                "Unsigned process has an established outbound connection."
        ));
    }
}

