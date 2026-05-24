package com.gluna.watchtower.scoring.rules;

import com.gluna.watchtower.model.Connection;
import com.gluna.watchtower.repo.ConnectionRepository;
import com.gluna.watchtower.scoring.Rule;
import com.gluna.watchtower.scoring.RuleContext;
import com.gluna.watchtower.scoring.RuleResult;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class BeaconCadenceRule implements Rule {

    private static final long MIN_MEAN_MS = Duration.ofSeconds(5).toMillis();
    private static final long MAX_MEAN_MS = Duration.ofHours(1).toMillis();

    private final ConnectionRepository connectionRepository;

    public BeaconCadenceRule(ConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    @Override
    public String code() {
        return "BEACON_CADENCE";
    }

    @Override
    public Optional<RuleResult> evaluate(RuleContext ctx) {
        if (ctx.process() == null || ctx.endpoint() == null) {
            return Optional.empty();
        }

        List<Connection> pairConnections = connectionRepository.findAllByProcessIdAndEndpointIdOrderByLastSeenAsc(
                ctx.process().getId(),
                ctx.endpoint().getId()
        );
        if (pairConnections.size() < 5) {
            return Optional.empty();
        }

        List<Long> gaps = gaps(pairConnections);
        if (gaps.size() < 4) {
            return Optional.empty();
        }

        double mean = gaps.stream().mapToLong(Long::longValue).average().orElse(0.0d);
        if (mean < MIN_MEAN_MS || mean > MAX_MEAN_MS) {
            return Optional.empty();
        }

        double variance = gaps.stream()
                .mapToDouble(gap -> Math.pow(gap - mean, 2))
                .average()
                .orElse(0.0d);
        double ratio = Math.sqrt(variance) / mean;
        if (ratio >= 0.2d) {
            return Optional.empty();
        }

        String detail = "Process and endpoint show regular cadence across "
                + pairConnections.size()
                + " observations.";
        return Optional.of(new RuleResult(code(), 20, detail));
    }

    private List<Long> gaps(List<Connection> connections) {
        List<Long> gaps = new ArrayList<>();
        Instant previous = null;
        for (Connection connection : connections) {
            if (connection.getLastSeen() == null) {
                continue;
            }
            Instant current = connection.getLastSeen().toInstant();
            if (previous != null && current.isAfter(previous)) {
                gaps.add(Duration.between(previous, current).toMillis());
            }
            previous = current;
        }
        return gaps;
    }
}
