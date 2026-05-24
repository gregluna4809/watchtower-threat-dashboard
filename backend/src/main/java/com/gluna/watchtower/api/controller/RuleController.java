package com.gluna.watchtower.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.gluna.watchtower.api.dto.RuleDto;
import com.gluna.watchtower.api.mapper.ApiMapper;
import com.gluna.watchtower.exception.NotFoundException;
import com.gluna.watchtower.model.RuleDefinition;
import com.gluna.watchtower.repo.RuleDefinitionRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {

    private final RuleDefinitionRepository ruleDefinitionRepository;

    public RuleController(RuleDefinitionRepository ruleDefinitionRepository) {
        this.ruleDefinitionRepository = ruleDefinitionRepository;
    }

    @GetMapping
    public List<RuleDto> rules() {
        return ruleDefinitionRepository.findAll().stream()
                .map(ApiMapper::toDto)
                .toList();
    }

    @PatchMapping("/{code}")
    public RuleDto updateRule(@PathVariable String code, @RequestBody JsonNode body) {
        Boolean enabled = parseEnabled(body);
        RuleDefinition ruleDefinition = ruleDefinitionRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Rule not found: " + code));
        ruleDefinition.setEnabled(enabled);
        return ApiMapper.toDto(ruleDefinitionRepository.save(ruleDefinition));
    }

    private Boolean parseEnabled(JsonNode body) {
        if (body == null || !body.isObject() || body.size() != 1 || !body.has("enabled")) {
            throw new IllegalArgumentException("PATCH /rules/{code} accepts only the enabled field");
        }
        JsonNode enabled = body.get("enabled");
        if (enabled == null || !enabled.isBoolean()) {
            throw new IllegalArgumentException("enabled must be a boolean");
        }
        return enabled.booleanValue();
    }
}

