package com.gluna.watchtower.capture;

import com.gluna.watchtower.exception.CaptureException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WindowsCommandRunner {

    private static final Logger log = LoggerFactory.getLogger(WindowsCommandRunner.class);

    public String run(List<String> command, Duration timeout) {
        if (command == null || command.isEmpty()) {
            throw new CaptureException("Command must not be empty");
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new CaptureException("Command timeout must be positive");
        }

        log.debug("Running command: {}", command);

        Process process;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
        } catch (IOException ex) {
            throw new CaptureException("Failed to start command: " + command, ex);
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> outputFuture = executor.submit(readOutput(process));
        try {
            boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new CaptureException("Command timed out after " + timeout.toMillis() + " ms: " + command);
            }

            String output = outputFuture.get();
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.warn("Command exited with code {}: {}", exitCode, command);
                throw new CaptureException("Command exited with code " + exitCode + ": " + command);
            }
            return output;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new CaptureException("Interrupted while running command: " + command, ex);
        } catch (ExecutionException ex) {
            throw new CaptureException("Failed to read command output: " + command, ex.getCause());
        } finally {
            executor.shutdownNow();
        }
    }

    public List<String> runLines(List<String> command, Duration timeout) {
        return run(command, timeout).lines().collect(Collectors.toList());
    }

    private Callable<String> readOutput(Process process) {
        return () -> {
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }
            return output.toString();
        };
    }
}

