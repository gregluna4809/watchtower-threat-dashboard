package com.gluna.watchtower.scoring.rules;

import com.gluna.watchtower.model.ProcessEntity;
import com.gluna.watchtower.model.RemoteEndpoint;
import com.gluna.watchtower.scoring.Rule;
import com.gluna.watchtower.scoring.RuleContext;
import com.gluna.watchtower.scoring.RuleResult;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PrivateRangeBrowserRule implements Rule {

    public static final String CODE = "PRIVATE_RANGE_BROWSER";

    private final Set<String> knownBrowsers;

    public PrivateRangeBrowserRule(@Value("${watchtower.scoring.known-browsers}") Set<String> knownBrowsers) {
        this.knownBrowsers = knownBrowsers.stream()
                .map(browser -> browser.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public Optional<RuleResult> evaluate(RuleContext ctx) {
        ProcessEntity process = ctx.process();
        RemoteEndpoint endpoint = ctx.endpoint();
        if (process == null || endpoint == null || process.getName() == null) {
            return Optional.empty();
        }

        String processName = process.getName().toLowerCase(Locale.ROOT);
        if (!knownBrowsers.contains(processName) || !isRfc1918(endpoint.getIp())) {
            return Optional.empty();
        }

        // TODO: Exclude the actual local subnet after network-interface modeling is introduced.
        return Optional.of(new RuleResult(
                CODE,
                10,
                "Known browser connected to a private RFC1918 address."
        ));
    }

    private boolean isRfc1918(String ip) {
        try {
            byte[] bytes = InetAddress.getByName(ip).getAddress();
            if (bytes.length != 4) {
                return false;
            }
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            return first == 10
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168);
        } catch (UnknownHostException ex) {
            return false;
        }
    }
}

