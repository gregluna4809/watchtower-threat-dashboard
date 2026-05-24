package com.gluna.watchtower.enrichment;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeoIpService {

    private static final Logger log = LoggerFactory.getLogger(GeoIpService.class);
    private static final String DOWNLOAD_URL = "https://dev.maxmind.com/geoip/geolite2-free-geolocation-data";

    private final DatabaseReader cityReader;
    private final DatabaseReader asnReader;

    public GeoIpService(
            @Value("${watchtower.geoip.city-db-path}") String cityDbPath,
            @Value("${watchtower.geoip.asn-db-path}") String asnDbPath
    ) {
        this.cityReader = openReader(cityDbPath, "GeoLite2 City").orElse(null);
        this.asnReader = openReader(asnDbPath, "GeoLite2 ASN").orElse(null);
    }

    public Optional<GeoIpResult> lookup(String ip) {
        if (cityReader == null || asnReader == null || !isPublicAddress(ip)) {
            return Optional.empty();
        }

        try {
            InetAddress address = InetAddress.getByName(ip);
            CityResponse cityResponse = cityReader.city(address);
            AsnResponse asnResponse = asnReader.asn(address);
            return Optional.of(new GeoIpResult(
                    longToInteger(asnResponse.getAutonomousSystemNumber()),
                    blankToNull(asnResponse.getAutonomousSystemOrganization()),
                    blankToNull(cityResponse.getCountry().getIsoCode()),
                    blankToNull(cityResponse.getCountry().getName()),
                    blankToNull(cityResponse.getCity().getName())
            ));
        } catch (AddressNotFoundException ex) {
            return Optional.empty();
        } catch (GeoIp2Exception ex) {
            log.warn("GeoIP lookup failed for IP {}: {}", ip, ex.getMessage());
            return Optional.empty();
        } catch (IOException ex) {
            log.warn("GeoIP lookup failed for IP {}: {}", ip, ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean isPublicAddress(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            byte[] bytes = address.getAddress();
            return !address.isAnyLocalAddress()
                    && !address.isLoopbackAddress()
                    && !address.isLinkLocalAddress()
                    && !address.isSiteLocalAddress()
                    && !address.isMulticastAddress()
                    && !isCarrierGradeNat(bytes);
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    @PreDestroy
    public void close() {
        closeReader(cityReader);
        closeReader(asnReader);
    }

    private Optional<DatabaseReader> openReader(String dbPath, String label) {
        Path path = Path.of(dbPath);
        if (!Files.isRegularFile(path)) {
            log.warn("{} database not found at {}. Download GeoLite2 databases from {} and place them there. GeoIP enrichment is disabled for this database.", label, path, DOWNLOAD_URL);
            return Optional.empty();
        }

        try {
            return Optional.of(new DatabaseReader.Builder(path.toFile()).build());
        } catch (IOException ex) {
            log.warn("{} database could not be opened at {}: {}", label, path, ex.getMessage());
            return Optional.empty();
        }
    }

    private boolean isCarrierGradeNat(byte[] bytes) {
        if (bytes.length != 4) {
            return false;
        }
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        return first == 100 && second >= 64 && second <= 127;
    }

    private Integer longToInteger(Long value) {
        if (value == null || value > Integer.MAX_VALUE) {
            return null;
        }
        return value.intValue();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void closeReader(DatabaseReader reader) {
        if (reader == null) {
            return;
        }
        try {
            reader.close();
        } catch (IOException ex) {
            log.warn("Failed to close GeoIP database reader: {}", ex.getMessage());
        }
    }
}
