package com.gluna.watchtower.scoring;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.gluna.watchtower.model.RuleDefinition;
import com.gluna.watchtower.model.TargetType;
import com.gluna.watchtower.model.ThreatScore;
import com.gluna.watchtower.repo.RuleDefinitionRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RulesEngine {

    private static final String ENABLED_RULES_CACHE_KEY = "enabledRules";

    private final List<Rule> rules;
    private final RuleDefinitionRepository ruleDefinitionRepository;
    private final Cache<String, Set<String>> enabledRuleCache;

    public RulesEngine(List<Rule> rules, RuleDefinitionRepository ruleDefinitionRepository) {
        this.rules = rules.stream()
                .sorted(Comparator.comparing(Rule::code))
                .toList();
        this.ruleDefinitionRepository = ruleDefinitionRepository;
        this.enabledRuleCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(30))
                .maximumSize(1)
                .build();
    }

    public ThreatScore score(RuleContext ctx) {
        Set<String> enabledCodes = enabledRuleCodes();
        List<RuleResult> results = new ArrayList<>();
        for (Rule rule : rules) {
            if (enabledCodes.contains(rule.code())) {
                rule.evaluate(ctx).ifPresent(results::add);
            }
        }

        int score = results.stream()
                .mapToInt(RuleResult::points)
                .sum();

        ThreatScore threatScore = new ThreatScore();
        threatScore.setTargetType(TargetType.CONNECTION);
        threatScore.setTargetId(ctx.connection().getId());
        threatScore.setScore(Math.min(score, 100));
        threatScore.setReasons(results.stream().map(this::toReason).toList());
        threatScore.setComputedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return threatScore;
    }

    private Set<String> enabledRuleCodes() {
        return enabledRuleCache.get(
                ENABLED_RULES_CACHE_KEY,
                ignored -> ruleDefinitionRepository.findAllByEnabledTrue().stream()
                        .map(RuleDefinition::getCode)
                        .collect(Collectors.toUnmodifiableSet())
        );
    }

    private Map<String, Object> toReason(RuleResult result) {
        return Map.of(
                "rule", result.ruleCode(),
                "points", result.points(),
                "detail", result.detail()
        );
    }
}

