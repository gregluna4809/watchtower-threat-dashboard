package com.gluna.watchtower.enrichment;

import com.gluna.watchtower.model.ProcessEntity;
import com.gluna.watchtower.model.RemoteEndpoint;
import com.gluna.watchtower.process.ProcessInfo;
import com.gluna.watchtower.process.SignatureResult;
import com.gluna.watchtower.process.SignatureService;
import com.gluna.watchtower.process.TasklistService;
import com.gluna.watchtower.repo.ProcessRepository;
import com.gluna.watchtower.repo.RemoteEndpointRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentService.class);

    private final RemoteEndpointRepository remoteEndpointRepository;
    private final ProcessRepository processRepository;
    private final GeoIpService geoIpService;
    private final ReverseDnsService reverseDnsService;
    private final ThreatIntelService threatIntelService;
    private final TasklistService tasklistService;
    private final SignatureService signatureService;
    private final TransactionTemplate transactionTemplate;
    private final int rdnsBatchSize;

    public EnrichmentService(
            RemoteEndpointRepository remoteEndpointRepository,
            ProcessRepository processRepository,
            GeoIpService geoIpService,
            ReverseDnsService reverseDnsService,
            ThreatIntelService threatIntelService,
            TasklistService tasklistService,
            SignatureService signatureService,
            TransactionTemplate transactionTemplate,
            @Value("${watchtower.enrichment.rdns-batch-size:20}") int rdnsBatchSize
    ) {
        this.remoteEndpointRepository = remoteEndpointRepository;
        this.processRepository = processRepository;
        this.geoIpService = geoIpService;
        this.reverseDnsService = reverseDnsService;
        this.threatIntelService = threatIntelService;
        this.tasklistService = tasklistService;
        this.signatureService = signatureService;
        this.transactionTemplate = transactionTemplate;
        this.rdnsBatchSize = rdnsBatchSize;
    }

    @Async("enrichmentExecutor")
    @Scheduled(fixedDelayString = "${watchtower.enrichment.interval-ms:10000}")
    public void enrich() {
        enrichRemoteEndpoints();
        enrichProcesses();
    }

    @Async("enrichmentExecutor")
    @Scheduled(fixedDelay = 30000)
    public void enrichThreatIntel() {
        List<RemoteEndpoint> endpoints = remoteEndpointRepository.findTop10NeedingAbuseIpdbCheck();
        int checked = 0;
        for (RemoteEndpoint endpoint : endpoints) {
            try {
                if (!geoIpService.isPublicAddress(endpoint.getIp())) {
                    continue;
                }
                if (threatIntelService.checkIp(endpoint.getIp()).isPresent()) {
                    checked++;
                }
            } catch (RuntimeException ex) {
                log.warn("Threat intel enrichment failed for endpoint {}: {}", endpoint.getId(), ex.getMessage());
            }
        }
        if (checked > 0) {
            log.info("Threat intel enrichment: {} AbuseIPDB cache rows refreshed", checked);
        }
    }

    @Async("enrichmentExecutor")
    @Scheduled(fixedDelayString = "${watchtower.enrichment.rdns-interval-ms:30000}")
    public void enrichReverseDns() {
        List<RemoteEndpoint> endpoints = remoteEndpointRepository.findTopNeedingReverseDns(rdnsBatchSize);
        int enriched = 0;
        for (RemoteEndpoint endpoint : endpoints) {
            try {
                if (!IpUtils.isPublicAddress(endpoint.getIp())) {
                    continue;
                }
                boolean saved = Boolean.TRUE.equals(transactionTemplate.execute(status ->
                        reverseDnsService.resolve(endpoint.getIp())
                                .map(hostname -> saveReverseDns(endpoint.getId(), hostname))
                                .orElse(false)
                ));
                if (saved) {
                    enriched++;
                }
            } catch (RuntimeException ex) {
                log.warn("Reverse DNS enrichment failed for endpoint {}: {}", endpoint.getId(), ex.getMessage());
            }
        }
        if (!endpoints.isEmpty()) {
            log.info("Reverse DNS enrichment: {} endpoints updated from {} candidates", enriched, endpoints.size());
        }
    }

    private void enrichRemoteEndpoints() {
        List<RemoteEndpoint> endpoints = remoteEndpointRepository.findTop50ByCountryIsoIsNullOrderByLastSeenDesc();
        int enriched = 0;
        for (RemoteEndpoint endpoint : endpoints) {
            try {
                if (!geoIpService.isPublicAddress(endpoint.getIp())) {
                    continue;
                }
                boolean saved = Boolean.TRUE.equals(transactionTemplate.execute(status ->
                        geoIpService.lookup(endpoint.getIp())
                                .map(result -> saveGeoIp(endpoint.getId(), result))
                                .orElse(false)
                ));
                if (saved) {
                    enriched++;
                }
            } catch (RuntimeException ex) {
                log.warn("GeoIP enrichment failed for endpoint {}: {}", endpoint.getId(), ex.getMessage());
            }
        }
        if (!endpoints.isEmpty()) {
            log.info("GeoIP enrichment: {} endpoints updated from {} candidates", enriched, endpoints.size());
        }
    }

    private boolean saveGeoIp(Long endpointId, GeoIpResult result) {
        return remoteEndpointRepository.findById(endpointId)
                .map(endpoint -> {
                    endpoint.setAsn(result.asn());
                    endpoint.setAsnOrg(result.asnOrg());
                    endpoint.setCountryIso(result.countryIso());
                    endpoint.setCountryName(result.countryName());
                    endpoint.setCity(result.city());
                    endpoint.setLatitude(result.latitude());
                    endpoint.setLongitude(result.longitude());
                    remoteEndpointRepository.save(endpoint);
                    return true;
                })
                .orElse(false);
    }

    private boolean saveReverseDns(Long endpointId, String hostname) {
        return remoteEndpointRepository.findById(endpointId)
                .map(endpoint -> {
                    endpoint.setReverseDns(hostname);
                    remoteEndpointRepository.save(endpoint);
                    return true;
                })
                .orElse(false);
    }

    private void enrichProcesses() {
        List<ProcessEntity> processes = processRepository.findTop25ByPathIsNullOrderByLastSeenDesc();
        int enriched = 0;
        for (ProcessEntity process : processes) {
            try {
                boolean saved = Boolean.TRUE.equals(transactionTemplate.execute(status ->
                        tasklistService.lookupDetailed(process.getPid())
                                .filter(processInfo -> processInfo.path() != null && !processInfo.path().isBlank())
                                .map(processInfo -> saveProcessDetails(process.getId(), processInfo))
                                .orElse(false)
                ));
                if (saved) {
                    enriched++;
                }
            } catch (RuntimeException ex) {
                log.warn("Process enrichment failed for process {} pid {}: {}", process.getId(), process.getPid(), ex.getMessage());
            }
        }
        if (!processes.isEmpty()) {
            log.info("Process enrichment: {} processes updated from {} candidates", enriched, processes.size());
        }
    }

    private boolean saveProcessDetails(Long processId, ProcessInfo processInfo) {
        SignatureResult signature = signatureService.check(processInfo.path());
        return processRepository.findById(processId)
                .map(process -> {
                    process.setPath(processInfo.path());
                    process.setSigned(signature.signed());
                    process.setSigner(signature.signer());
                    processRepository.save(process);
                    return true;
                })
                .orElse(false);
    }
}
