package com.gluna.watchtower.api.dto;

import java.util.List;

public record Page<T>(List<T> items, int total, int limit, int offset) {
}

