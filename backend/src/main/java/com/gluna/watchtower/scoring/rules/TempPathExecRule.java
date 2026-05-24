package com.gluna.watchtower.scoring.rules;

import com.gluna.watchtower.model.ProcessEntity;
import com.gluna.watchtower.scoring.Rule;
import com.gluna.watchtower.scoring.RuleContext;
import com.gluna.watchtower.scoring.RuleResult;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TempPathExecRule implements Rule {

    public static final String CODE = "TEMP_PATH_EXEC";

    private static final List<String> SUSPICIOUS_PATH_PARTS = List.of(
            "\\appdata\\local\\temp\\",
            "\\appdata\\roaming\\",
            "\\users\\public\\",
            "\\windows\\temp\\",
            "c:\\temp\\",
            "c:\\tmp\\"
    );

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public Optional<RuleResult> evaluate(RuleContext ctx) {
        ProcessEntity process = ctx.process();
        if (process == null || process.getPath() == null || process.getPath().isBlank()) {
            return Optional.empty();
        }

        String path = process.getPath().toLowerCase(Locale.ROOT);
        boolean matched = SUSPICIOUS_PATH_PARTS.stream().anyMatch(path::contains);
        if (!matched) {
            return Optional.empty();
        }

        return Optional.of(new RuleResult(
                CODE,
                30,
                "Process executable is running from a temporary or user-writable path."
        ));
    }
}

