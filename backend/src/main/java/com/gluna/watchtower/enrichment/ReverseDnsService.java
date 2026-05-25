package com.gluna.watchtower.enrichment;

import jakarta.annotation.PreDestroy;
import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReverseDnsService {

    private static final Logger log = LoggerFactory.getLogger(ReverseDnsService.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final long timeoutMs;

    public ReverseDnsService(@Value("${watchtower.enrichment.rdns-timeout-ms:2000}") long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Optional<String> resolve(String ip) {
        if (!IpUtils.isPublicAddress(ip)) {
            return Optional.empty();
        }

        Future<String> future = executorService.submit(() -> InetAddress.getByName(ip).getCanonicalHostName());
        try {
            String canonicalHostName = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (canonicalHostName == null || canonicalHostName.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(canonicalHostName);
        } catch (TimeoutException ex) {
            future.cancel(true);
            log.debug("Reverse DNS lookup timed out for IP {}", ip);
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException ex) {
            log.debug("Reverse DNS lookup failed for IP {}: {}", ip, ex.getMessage());
            return Optional.empty();
        }
    }

    @PreDestroy
    public void close() {
        executorService.shutdownNow();
    }
}
