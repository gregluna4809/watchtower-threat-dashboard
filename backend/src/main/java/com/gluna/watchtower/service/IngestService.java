package com.gluna.watchtower.service;

import com.gluna.watchtower.capture.ConnectionSnapshot;
import com.gluna.watchtower.capture.NetstatPoller;
import com.gluna.watchtower.model.Connection;
import com.gluna.watchtower.model.ConnectionState;
import com.gluna.watchtower.model.ProcessEntity;
import com.gluna.watchtower.model.Protocol;
import com.gluna.watchtower.model.RemoteEndpoint;
import com.gluna.watchtower.process.ProcessInfo;
import com.gluna.watchtower.process.TasklistService;
import com.gluna.watchtower.repo.ConnectionRepository;
import com.gluna.watchtower.repo.ProcessRepository;
import com.gluna.watchtower.repo.RemoteEndpointRepository;
import com.gluna.watchtower.ws.LiveUpdatePublisher;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final NetstatPoller netstatPoller;
    private final TasklistService tasklistService;
    private final ProcessRepository processRepository;
    private final RemoteEndpointRepository remoteEndpointRepository;
    private final ConnectionRepository connectionRepository;
    private final LiveUpdatePublisher liveUpdatePublisher;

    public IngestService(
            NetstatPoller netstatPoller,
            TasklistService tasklistService,
            ProcessRepository processRepository,
            RemoteEndpointRepository remoteEndpointRepository,
            ConnectionRepository connectionRepository,
            LiveUpdatePublisher liveUpdatePublisher
    ) {
        this.netstatPoller = netstatPoller;
        this.tasklistService = tasklistService;
        this.processRepository = processRepository;
        this.remoteEndpointRepository = remoteEndpointRepository;
        this.connectionRepository = connectionRepository;
        this.liveUpdatePublisher = liveUpdatePublisher;
    }

    @Async("ingestExecutor")
    @Scheduled(fixedDelayString = "${watchtower.ingest.interval-ms:3500}")
    @Transactional
    public void ingestLatestSnapshots() {
        List<ConnectionSnapshot> snapshots = netstatPoller.getLatest();
        if (snapshots.isEmpty()) {
            log.debug("Ingest skipped: no snapshots available");
            return;
        }

        Map<Integer, ProcessInfo> processInfoByPid = loadProcessInfo(snapshots);
        Map<String, RemoteEndpoint> endpointByIp = ensureRemoteEndpoints(snapshots);
        Map<ProcessKey, ProcessEntity> processByKey = ensureProcesses(snapshots, processInfoByPid);

        int insertedConnections = 0;
        int updatedConnections = 0;
        List<Long> changedConnectionIds = new ArrayList<>();
        for (ConnectionSnapshot snapshot : snapshots) {
            ProcessEntity process = resolveProcess(snapshot, processInfoByPid, processByKey).orElse(null);
            RemoteEndpoint endpoint = hasRemoteEndpoint(snapshot) ? endpointByIp.get(snapshot.remoteIp()) : null;
            Protocol protocol = Protocol.valueOf(snapshot.protocol());
            OffsetDateTime observedAt = OffsetDateTime.ofInstant(snapshot.observedAt(), ZoneOffset.UTC);

            Optional<Connection> existing = connectionRepository.findByUniqueTuple(
                    process == null ? null : process.getId(),
                    snapshot.localPort(),
                    endpoint == null ? null : endpoint.getId(),
                    snapshot.remotePort(),
                    protocol
            );

            if (existing.isPresent()) {
                Connection connection = updateConnection(existing.get(), snapshot, process, endpoint, observedAt);
                changedConnectionIds.add(connection.getId());
                updatedConnections++;
            } else {
                Connection connection = createConnection(snapshot, process, endpoint, observedAt);
                changedConnectionIds.add(connectionRepository.save(connection).getId());
                insertedConnections++;
            }
        }

        log.info(
                "Ingested {} snapshots ({} inserted connections, {} updated connections)",
                snapshots.size(),
                insertedConnections,
                updatedConnections
        );
        liveUpdatePublisher.publishConnectionsAfterCommit(changedConnectionIds);
        liveUpdatePublisher.publishStatsAfterCommit();
    }

    private Map<Integer, ProcessInfo> loadProcessInfo(List<ConnectionSnapshot> snapshots) {
        Set<Integer> pids = snapshots.stream()
                .map(ConnectionSnapshot::pid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, ProcessInfo> processInfoByPid = new HashMap<>();
        for (Integer pid : pids) {
            tasklistService.lookup(pid).ifPresent(processInfo -> processInfoByPid.put(pid, processInfo));
        }
        return processInfoByPid;
    }

    private Map<String, RemoteEndpoint> ensureRemoteEndpoints(List<ConnectionSnapshot> snapshots) {
        Map<String, OffsetDateTime> lastSeenByIp = snapshots.stream()
                .filter(this::hasRemoteEndpoint)
                .collect(Collectors.toMap(
                        ConnectionSnapshot::remoteIp,
                        snapshot -> OffsetDateTime.ofInstant(snapshot.observedAt(), ZoneOffset.UTC),
                        (left, right) -> left.isAfter(right) ? left : right,
                        LinkedHashMap::new
                ));

        Map<String, RemoteEndpoint> endpointByIp = new HashMap<>();
        for (Map.Entry<String, OffsetDateTime> entry : lastSeenByIp.entrySet()) {
            RemoteEndpoint endpoint = remoteEndpointRepository.findByIp(entry.getKey())
                    .map(existing -> updateRemoteEndpoint(existing, entry.getValue()))
                    .orElseGet(() -> createRemoteEndpoint(entry.getKey(), entry.getValue()));
            endpointByIp.put(entry.getKey(), remoteEndpointRepository.save(endpoint));
        }
        return endpointByIp;
    }

    private RemoteEndpoint updateRemoteEndpoint(RemoteEndpoint endpoint, OffsetDateTime lastSeen) {
        if (endpoint.getLastSeen().isBefore(lastSeen)) {
            endpoint.setLastSeen(lastSeen);
        }
        return endpoint;
    }

    private RemoteEndpoint createRemoteEndpoint(String ip, OffsetDateTime observedAt) {
        RemoteEndpoint endpoint = new RemoteEndpoint();
        endpoint.setIp(ip);
        endpoint.setFirstSeen(observedAt);
        endpoint.setLastSeen(observedAt);
        return endpoint;
    }

    private Map<ProcessKey, ProcessEntity> ensureProcesses(
            List<ConnectionSnapshot> snapshots,
            Map<Integer, ProcessInfo> processInfoByPid
    ) {
        Map<ProcessKey, OffsetDateTime> lastSeenByProcess = new LinkedHashMap<>();
        for (ConnectionSnapshot snapshot : snapshots) {
            processKey(snapshot, processInfoByPid).ifPresent(key -> {
                OffsetDateTime observedAt = OffsetDateTime.ofInstant(snapshot.observedAt(), ZoneOffset.UTC);
                lastSeenByProcess.merge(key, observedAt, (left, right) -> left.isAfter(right) ? left : right);
            });
        }

        Map<ProcessKey, ProcessEntity> processByKey = new HashMap<>();
        for (Map.Entry<ProcessKey, OffsetDateTime> entry : lastSeenByProcess.entrySet()) {
            ProcessKey key = entry.getKey();
            OffsetDateTime observedAt = entry.getValue();
            ProcessEntity process = processRepository.findFirstByPidAndNameOrderByFirstSeenDesc(key.pid(), key.name())
                    .map(existing -> updateProcess(existing, observedAt))
                    .orElseGet(() -> createProcess(key, observedAt));
            processByKey.put(key, processRepository.save(process));
        }
        return processByKey;
    }

    private Optional<ProcessKey> processKey(ConnectionSnapshot snapshot, Map<Integer, ProcessInfo> processInfoByPid) {
        Integer pid = snapshot.pid();
        if (pid == null) {
            return Optional.empty();
        }

        ProcessInfo processInfo = processInfoByPid.get(pid);
        String name = processInfo == null ? "pid-" + pid : processInfo.name();
        return Optional.of(new ProcessKey(pid, name));
    }

    private ProcessEntity updateProcess(ProcessEntity process, OffsetDateTime lastSeen) {
        if (process.getLastSeen().isBefore(lastSeen)) {
            process.setLastSeen(lastSeen);
        }
        return process;
    }

    private ProcessEntity createProcess(ProcessKey key, OffsetDateTime observedAt) {
        ProcessEntity process = new ProcessEntity();
        process.setPid(key.pid());
        process.setName(key.name());
        process.setPath(null);
        process.setSigned(null);
        process.setSigner(null);
        process.setFirstSeen(observedAt);
        process.setLastSeen(observedAt);
        return process;
    }

    private Optional<ProcessEntity> resolveProcess(
            ConnectionSnapshot snapshot,
            Map<Integer, ProcessInfo> processInfoByPid,
            Map<ProcessKey, ProcessEntity> processByKey
    ) {
        return processKey(snapshot, processInfoByPid).map(processByKey::get);
    }

    private Connection updateConnection(
            Connection connection,
            ConnectionSnapshot snapshot,
            ProcessEntity process,
            RemoteEndpoint endpoint,
            OffsetDateTime observedAt
    ) {
        connection.setProcess(process);
        connection.setEndpoint(endpoint);
        connection.setLocalIp(snapshot.localIp());
        connection.setState(toConnectionState(snapshot.state()));
        connection.setLastSeen(observedAt);
        connection.setObservationCount(connection.getObservationCount() + 1);
        return connectionRepository.save(connection);
    }

    private Connection createConnection(
            ConnectionSnapshot snapshot,
            ProcessEntity process,
            RemoteEndpoint endpoint,
            OffsetDateTime observedAt
    ) {
        Connection connection = new Connection();
        connection.setProcess(process);
        connection.setEndpoint(endpoint);
        connection.setLocalIp(snapshot.localIp());
        connection.setLocalPort(snapshot.localPort());
        connection.setRemotePort(snapshot.remotePort());
        connection.setProtocol(Protocol.valueOf(snapshot.protocol()));
        connection.setState(toConnectionState(snapshot.state()));
        connection.setFirstSeen(observedAt);
        connection.setLastSeen(observedAt);
        connection.setObservationCount(1);
        return connection;
    }

    private boolean hasRemoteEndpoint(ConnectionSnapshot snapshot) {
        return snapshot.remoteIp() != null
                && snapshot.remotePort() != null
                && snapshot.remotePort() > 0
                && !"LISTENING".equals(snapshot.state());
    }

    private ConnectionState toConnectionState(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }
        try {
            return ConnectionState.valueOf(state);
        } catch (IllegalArgumentException ex) {
            return ConnectionState.UNKNOWN;
        }
    }

    private record ProcessKey(int pid, String name) {
    }
}
