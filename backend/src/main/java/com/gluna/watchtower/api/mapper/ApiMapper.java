package com.gluna.watchtower.api.mapper;

import com.gluna.watchtower.api.dto.EndpointSummary;
import com.gluna.watchtower.api.dto.ProcessSummary;
import com.gluna.watchtower.api.dto.RuleDto;
import com.gluna.watchtower.model.ProcessEntity;
import com.gluna.watchtower.model.RemoteEndpoint;
import com.gluna.watchtower.model.RuleDefinition;

public final class ApiMapper {

    private ApiMapper() {
    }

    public static ProcessSummary toSummary(ProcessEntity process) {
        if (process == null) {
            return null;
        }
        return new ProcessSummary(
                process.getId(),
                process.getPid(),
                process.getName(),
                process.getPath(),
                process.getSigned(),
                process.getSigner()
        );
    }

    public static EndpointSummary toSummary(RemoteEndpoint endpoint) {
        if (endpoint == null) {
            return null;
        }
        return new EndpointSummary(
                endpoint.getId(),
                endpoint.getIp(),
                endpoint.getCountryIso(),
                endpoint.getCountryName(),
                endpoint.getAsnOrg()
        );
    }

    public static RuleDto toDto(RuleDefinition ruleDefinition) {
        return new RuleDto(
                ruleDefinition.getCode(),
                ruleDefinition.getDisplayName(),
                ruleDefinition.getDescription(),
                ruleDefinition.getDefaultPoints(),
                ruleDefinition.getEnabled()
        );
    }
}

