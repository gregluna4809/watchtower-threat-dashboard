package com.gluna.watchtower.process;

import com.gluna.watchtower.capture.WindowsCommandRunner;
import com.gluna.watchtower.exception.CaptureException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TasklistService {

    private static final Logger log = LoggerFactory.getLogger(TasklistService.class);
    private static final Duration CACHE_TTL = Duration.ofSeconds(10);

    private final WindowsCommandRunner commandRunner;
    private final Duration commandTimeout;
    private final Object cacheLock = new Object();

    private Instant cacheLoadedAt = Instant.EPOCH;
    private Map<Integer, ProcessInfo> cachedProcesses = Map.of();

    public TasklistService(
            WindowsCommandRunner commandRunner,
            @Value("${watchtower.capture.command-timeout-ms:5000}") long commandTimeoutMs
    ) {
        this.commandRunner = commandRunner;
        this.commandTimeout = Duration.ofMillis(commandTimeoutMs);
    }

    public Optional<ProcessInfo> lookup(int pid) {
        return Optional.ofNullable(snapshot().get(pid));
    }

    public Optional<ProcessInfo> lookupDetailed(int pid) {
        Optional<ProcessInfo> tasklistInfo = lookupDetailedTasklist(pid);
        if (tasklistInfo.isEmpty()) {
            return Optional.empty();
        }

        ProcessInfo processInfo = tasklistInfo.get();
        String path = resolvePath(pid).orElse(null);
        return Optional.of(new ProcessInfo(
                processInfo.pid(),
                processInfo.name(),
                path,
                null,
                null
        ));
    }

    private Map<Integer, ProcessInfo> snapshot() {
        synchronized (cacheLock) {
            Instant now = Instant.now();
            if (Duration.between(cacheLoadedAt, now).compareTo(CACHE_TTL) < 0) {
                return cachedProcesses;
            }

            cachedProcesses = loadProcesses();
            cacheLoadedAt = now;
            return cachedProcesses;
        }
    }

    private Map<Integer, ProcessInfo> loadProcesses() {
        try {
            List<String> lines = commandRunner.runLines(List.of("tasklist", "/fo", "csv", "/nh"), commandTimeout);
            Map<Integer, ProcessInfo> processes = new HashMap<>();
            for (String line : lines) {
                parseLine(line).ifPresent(processInfo -> processes.put(processInfo.pid(), processInfo));
            }
            return Map.copyOf(processes);
        } catch (CaptureException ex) {
            log.warn("Tasklist lookup failed: {}", ex.getMessage());
            return cachedProcesses;
        }
    }

    private Optional<ProcessInfo> lookupDetailedTasklist(int pid) {
        try {
            List<String> lines = commandRunner.runLines(
                    List.of("tasklist", "/fo", "csv", "/v", "/fi", "PID eq " + pid),
                    commandTimeout
            );
            for (String line : lines) {
                Optional<ProcessInfo> processInfo = parseLine(line);
                if (processInfo.isPresent() && processInfo.get().pid() == pid) {
                    return processInfo;
                }
            }
        } catch (CaptureException ex) {
            log.warn("Detailed tasklist lookup failed for pid {}: {}", pid, ex.getMessage());
        }
        return Optional.empty();
    }

    private Optional<String> resolvePath(int pid) {
        String script = "Get-Process -Id " + pid + " -ErrorAction Stop | Select-Object -ExpandProperty Path";
        try {
            String output = commandRunner.run(
                    List.of("powershell.exe", "-NoProfile", "-Command", script),
                    commandTimeout
            );
            return output.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .findFirst();
        } catch (CaptureException ex) {
            log.debug("Process path lookup failed for pid {}: {}", pid, ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ProcessInfo> parseLine(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }

        List<String> columns = parseCsv(line);
        if (columns.size() < 2) {
            return Optional.empty();
        }

        String name = columns.getFirst();
        Integer pid = parseInteger(columns.get(1));
        if (name.isBlank() || pid == null) {
            return Optional.empty();
        }

        return Optional.of(new ProcessInfo(pid, name, null, null, null));
    }

    private List<String> parseCsv(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                columns.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        columns.add(current.toString());
        return columns;
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
