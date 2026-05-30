package com.gluna.watchtower.capture;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class NetstatParser {

    private static final Pattern SS_PID_PATTERN = Pattern.compile("pid=(\\d+)");

    public List<ConnectionSnapshot> parse(List<String> netstatLines, Instant observedAt) {
        if (netstatLines == null || netstatLines.isEmpty()) {
            return List.of();
        }

        List<ConnectionSnapshot> snapshots = new ArrayList<>();
        for (String rawLine : netstatLines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank() || isHeader(line)) {
                continue;
            }

            String[] columns = line.split("\\s+");
            String protocol = normalizeProtocol(columns[0]);
            if (!"TCP".equals(protocol) && !"UDP".equals(protocol)) {
                continue;
            }

            ConnectionSnapshot snapshot = parseColumns(protocol, columns, observedAt);
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        }
        return List.copyOf(snapshots);
    }

    public List<ConnectionSnapshot> parseSs(List<String> ssLines, Instant observedAt) {
        if (ssLines == null || ssLines.isEmpty()) {
            return List.of();
        }

        List<ConnectionSnapshot> snapshots = new ArrayList<>();
        for (String rawLine : ssLines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank() || isSsHeader(line)) {
                continue;
            }

            String[] columns = line.split("\\s+");
            if (columns.length < 6) {
                continue;
            }

            String protocol = normalizeProtocol(columns[0]);
            if (!"TCP".equals(protocol) && !"UDP".equals(protocol)) {
                continue;
            }

            ConnectionSnapshot snapshot = parseSsColumns(protocol, columns, observedAt);
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        }
        return List.copyOf(snapshots);
    }

    private boolean isHeader(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith("active connections")
                || lower.startsWith("proto ")
                || lower.startsWith("foreign address")
                || lower.startsWith("tcp connections")
                || lower.startsWith("udp connections");
    }

    private boolean isSsHeader(String line) {
        return line.toLowerCase().startsWith("netid ");
    }

    private String normalizeProtocol(String protocol) {
        String upper = protocol.toUpperCase();
        if ("TCPV6".equals(upper)) {
            return "TCP";
        }
        if ("UDPV6".equals(upper)) {
            return "UDP";
        }
        return upper;
    }

    private ConnectionSnapshot parseSsColumns(String protocol, String[] columns, Instant observedAt) {
        AddressPort local = parseAddressPort(columns[4]);
        AddressPort remote = parseAddressPort(columns[5]);
        if (local == null || local.port() == null) {
            return null;
        }

        return new ConnectionSnapshot(
                protocol,
                local.address(),
                local.port(),
                remote == null ? null : remote.address(),
                remote == null ? null : remote.port(),
                normalizeSsState(columns[1]),
                parseSsPid(columns),
                observedAt
        );
    }

    private ConnectionSnapshot parseColumns(String protocol, String[] columns, Instant observedAt) {
        if ("TCP".equals(protocol)) {
            if (columns.length < 5) {
                return null;
            }

            AddressPort local = parseAddressPort(columns[1]);
            AddressPort remote = parseAddressPort(columns[2]);
            Integer pid = parseInteger(columns[4]);
            if (local == null || local.port() == null || pid == null) {
                return null;
            }

            return new ConnectionSnapshot(
                    protocol,
                    local.address(),
                    local.port(),
                    remote == null ? null : remote.address(),
                    remote == null ? null : remote.port(),
                    columns[3],
                    pid,
                    observedAt
            );
        }

        if (columns.length < 4) {
            return null;
        }

        AddressPort local = parseAddressPort(columns[1]);
        AddressPort remote = parseAddressPort(columns[2]);
        Integer pid = parseInteger(columns[3]);
        if (local == null || local.port() == null || pid == null) {
            return null;
        }

        return new ConnectionSnapshot(
                protocol,
                local.address(),
                local.port(),
                remote == null ? null : remote.address(),
                remote == null ? null : remote.port(),
                null,
                pid,
                observedAt
        );
    }

    private String normalizeSsState(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }

        return switch (state.toUpperCase()) {
            case "ESTAB" -> "ESTABLISHED";
            case "LISTEN" -> "LISTENING";
            case "SYN-SENT" -> "SYN_SENT";
            case "SYN-RECV" -> "SYN_RECV";
            case "FIN-WAIT-1" -> "FIN_WAIT_1";
            case "FIN-WAIT-2" -> "FIN_WAIT_2";
            case "CLOSE-WAIT" -> "CLOSE_WAIT";
            case "LAST-ACK" -> "LAST_ACK";
            case "TIME-WAIT" -> "TIME_WAIT";
            default -> state.toUpperCase().replace('-', '_');
        };
    }

    private Integer parseSsPid(String[] columns) {
        for (int i = 6; i < columns.length; i++) {
            Matcher matcher = SS_PID_PATTERN.matcher(columns[i]);
            if (matcher.find()) {
                return parseInteger(matcher.group(1));
            }
        }
        return null;
    }

    private AddressPort parseAddressPort(String value) {
        if (value == null || value.isBlank() || "*:*".equals(value)) {
            return null;
        }

        int separator = value.lastIndexOf(':');
        if (separator < 0 || separator == value.length() - 1) {
            return null;
        }

        String rawAddress = value.substring(0, separator);
        String rawPort = value.substring(separator + 1);
        if ("*".equals(rawPort)) {
            return null;
        }
        Integer port = parseInteger(rawPort);
        if (port == null) {
            return null;
        }

        return new AddressPort(cleanAddress(rawAddress), port);
    }

    private String cleanAddress(String rawAddress) {
        String address = rawAddress;
        if (address.startsWith("[") && address.endsWith("]")) {
            address = address.substring(1, address.length() - 1);
        }

        int zoneIndex = address.indexOf('%');
        if (zoneIndex >= 0) {
            address = address.substring(0, zoneIndex);
        }
        return address;
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record AddressPort(String address, Integer port) {
    }
}
