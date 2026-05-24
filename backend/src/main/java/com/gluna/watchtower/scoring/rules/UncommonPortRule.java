package com.gluna.watchtower.scoring.rules;

import com.gluna.watchtower.scoring.Rule;
import com.gluna.watchtower.scoring.RuleContext;
import com.gluna.watchtower.scoring.RuleResult;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UncommonPortRule implements Rule {

    public static final String CODE = "UNCOMMON_PORT";

    private final Set<Integer> commonPorts;

    public UncommonPortRule(@Value("${watchtower.scoring.common-ports}") Set<Integer> commonPorts) {
        this.commonPorts = commonPorts.stream().collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public Optional<RuleResult> evaluate(RuleContext ctx) {
        Integer remotePort = ctx.connection().getRemotePort();
        if (remotePort == null || commonPorts.contains(remotePort)) {
            return Optional.empty();
        }

        return Optional.of(new RuleResult(
                CODE,
                5,
                "Remote port " + remotePort + " is outside the common port allowlist."
        ));
    }
}

