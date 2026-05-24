package com.gluna.watchtower.scoring;

import com.gluna.watchtower.model.Connection;
import com.gluna.watchtower.model.ProcessEntity;
import com.gluna.watchtower.model.RemoteEndpoint;
import java.util.Map;

public record RuleContext(
        Connection connection,
        ProcessEntity process,
        RemoteEndpoint endpoint,
        Map<String, Object> intel
) {
}

