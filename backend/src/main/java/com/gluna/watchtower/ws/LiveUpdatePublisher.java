package com.gluna.watchtower.ws;

import com.gluna.watchtower.api.ApiReadService;
import com.gluna.watchtower.api.dto.ConnectionDto;
import com.gluna.watchtower.exception.NotFoundException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class LiveUpdatePublisher {

    private static final Logger log = LoggerFactory.getLogger(LiveUpdatePublisher.class);
    private static final long STATS_THROTTLE_MS = 2_000L;

    private final SimpMessagingTemplate messagingTemplate;
    private final ApiReadService apiReadService;
    private volatile long lastStatsPublishedAt;

    public LiveUpdatePublisher(SimpMessagingTemplate messagingTemplate, ApiReadService apiReadService) {
        this.messagingTemplate = messagingTemplate;
        this.apiReadService = apiReadService;
    }

    public void publishConnectionsAfterCommit(Collection<Long> connectionIds) {
        Set<Long> ids = connectionIds.stream()
                .filter(id -> id != null)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        if (ids.isEmpty()) {
            return;
        }

        afterCommit(() -> {
            List<ConnectionDto> updates = ids.stream()
                    .map(this::connectionOrNull)
                    .filter(connection -> connection != null)
                    .toList();
            if (!updates.isEmpty()) {
                messagingTemplate.convertAndSend("/topic/connections", updates);
            }
        });
    }

    public void publishStatsAfterCommit() {
        afterCommit(() -> {
            long now = System.currentTimeMillis();
            if (now - lastStatsPublishedAt < STATS_THROTTLE_MS) {
                return;
            }
            lastStatsPublishedAt = now;
            messagingTemplate.convertAndSend("/topic/stats", apiReadService.summary());
        });
    }

    private ConnectionDto connectionOrNull(Long id) {
        try {
            return apiReadService.connection(id);
        } catch (NotFoundException ex) {
            log.debug("Skipping live update for missing connection {}", id);
            return null;
        }
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
