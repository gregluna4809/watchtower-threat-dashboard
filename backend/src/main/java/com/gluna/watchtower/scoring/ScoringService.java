package com.gluna.watchtower.scoring;

import com.gluna.watchtower.model.Connection;
import com.gluna.watchtower.model.IntelCache;
import com.gluna.watchtower.model.IntelSource;
import com.gluna.watchtower.model.ProcessEntity;
import com.gluna.watchtower.model.RemoteEndpoint;
import com.gluna.watchtower.model.TargetType;
import com.gluna.watchtower.model.ThreatScore;
import com.gluna.watchtower.repo.ConnectionRepository;
import com.gluna.watchtower.repo.IntelCacheRepository;
import com.gluna.watchtower.repo.ThreatScoreRepository;
import com.gluna.watchtower.ws.LiveUpdatePublisher;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScoringService {

    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);

    private final ConnectionRepository connectionRepository;
    private final IntelCacheRepository intelCacheRepository;
    private final ThreatScoreRepository threatScoreRepository;
    private final RulesEngine rulesEngine;
    private final LiveUpdatePublisher liveUpdatePublisher;

    public ScoringService(
            ConnectionRepository connectionRepository,
            IntelCacheRepository intelCacheRepository,
            ThreatScoreRepository threatScoreRepository,
            RulesEngine rulesEngine,
            LiveUpdatePublisher liveUpdatePublisher
    ) {
        this.connectionRepository = connectionRepository;
        this.intelCacheRepository = intelCacheRepository;
        this.threatScoreRepository = threatScoreRepository;
        this.rulesEngine = rulesEngine;
        this.liveUpdatePublisher = liveUpdatePublisher;
    }

    @Scheduled(fixedDelayString = "${watchtower.scoring.interval-ms:5000}")
    @Transactional
    public void scoreBatch() {
        List<Connection> connections = connectionRepository.findTop100NeedingScore();
        int connectionScores = 0;
        List<Long> scoreChangedConnectionIds = new ArrayList<>();
        for (Connection connection : connections) {
            ThreatScore previous = threatScoreRepository
                    .findFirstByTargetTypeAndTargetIdOrderByComputedAtDesc(TargetType.CONNECTION, connection.getId())
                    .orElse(null);
            ThreatScore score = rulesEngine.score(toContext(connection));
            threatScoreRepository.save(score);
            if (previous == null || !previous.getScore().equals(score.getScore())) {
                scoreChangedConnectionIds.add(connection.getId());
            }
            connectionScores++;
        }

        int processScores = scoreProcesses();
        if (connectionScores > 0 || processScores > 0) {
            log.info("Scoring: wrote {} connection scores and {} process scores", connectionScores, processScores);
        }
        liveUpdatePublisher.publishConnectionsAfterCommit(scoreChangedConnectionIds);
    }

    private RuleContext toContext(Connection connection) {
        ProcessEntity process = connection.getProcess();
        RemoteEndpoint endpoint = connection.getEndpoint();
        return new RuleContext(connection, process, endpoint, intelFor(endpoint));
    }

    private Map<String, Object> intelFor(RemoteEndpoint endpoint) {
        if (endpoint == null || endpoint.getIp() == null) {
            return Map.of();
        }
        return intelCacheRepository.findByIpAndSource(endpoint.getIp(), IntelSource.ABUSEIPDB)
                .map(IntelCache::getPayload)
                .orElse(Map.of());
    }

    private int scoreProcesses() {
        List<Object[]> rows = threatScoreRepository.findLatestConnectionScoreMaxByProcess();
        int written = 0;
        for (Object[] row : rows) {
            Long processId = (Long) row[0];
            Integer maxScore = ((Number) row[1]).intValue();
            ThreatScore latest = threatScoreRepository
                    .findFirstByTargetTypeAndTargetIdOrderByComputedAtDesc(TargetType.PROCESS, processId)
                    .orElse(null);
            if (latest != null && latest.getScore().equals(maxScore)) {
                continue;
            }

            ThreatScore score = new ThreatScore();
            score.setTargetType(TargetType.PROCESS);
            score.setTargetId(processId);
            score.setScore(maxScore);
            score.setReasons(List.of(Map.of(
                    "rule",
                    "PROCESS_MAX_CONNECTION_SCORE",
                    "points",
                    maxScore,
                    "detail",
                    "Process score is the maximum latest score across its connections."
            )));
            score.setComputedAt(OffsetDateTime.now(ZoneOffset.UTC));
            threatScoreRepository.save(score);
            written++;
        }
        return written;
    }
}
