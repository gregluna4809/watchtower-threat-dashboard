package com.gluna.watchtower.api.dto;

import java.time.OffsetDateTime;

public record ScoreTimelinePoint(OffsetDateTime minute, Integer maxScore) {
}
