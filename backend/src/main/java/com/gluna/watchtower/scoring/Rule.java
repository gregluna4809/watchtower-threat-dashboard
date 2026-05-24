package com.gluna.watchtower.scoring;

import java.util.Optional;

public interface Rule {

    String code();

    Optional<RuleResult> evaluate(RuleContext ctx);
}

