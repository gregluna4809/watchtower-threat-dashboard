package com.gluna.watchtower.process;

import com.gluna.watchtower.capture.WindowsCommandRunner;
import com.gluna.watchtower.exception.CaptureException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SignatureService {

    private static final Logger log = LoggerFactory.getLogger(SignatureService.class);
    private static final Pattern CN_PATTERN = Pattern.compile("CN=(?:\"([^\"]+)\"|([^,\\r\\n]+))");

    private final WindowsCommandRunner commandRunner;
    private final Duration commandTimeout;

    public SignatureService(
            WindowsCommandRunner commandRunner,
            @Value("${watchtower.capture.command-timeout-ms:5000}") long commandTimeoutMs
    ) {
        this.commandRunner = commandRunner;
        this.commandTimeout = Duration.ofMillis(commandTimeoutMs);
    }

    public SignatureResult check(String path) {
        if (path == null || path.isBlank()) {
            return new SignatureResult(null, null);
        }

        String escapedPath = escapePowerShellSingleQuoted(path);
        String script = "Get-AuthenticodeSignature -FilePath '" + escapedPath
                + "' | Select-Object Status, SignerCertificate | Format-List";
        try {
            String output = commandRunner.run(
                    List.of("powershell.exe", "-NoProfile", "-Command", script),
                    commandTimeout
            );
            return new SignatureResult(parseSigned(output), parseSigner(output).orElse(null));
        } catch (CaptureException ex) {
            log.warn("Signature check failed for path {}: {}", path, ex.getMessage());
            return new SignatureResult(null, null);
        }
    }

    private Boolean parseSigned(String output) {
        String status = parseStatus(output).orElse(null);
        if (status == null) {
            return null;
        }
        if ("Valid".equalsIgnoreCase(status)) {
            return true;
        }
        if ("NotSigned".equalsIgnoreCase(status)) {
            return false;
        }
        return null;
    }

    private Optional<String> parseStatus(String output) {
        return output.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("Status"))
                .map(line -> line.substring(line.indexOf(':') + 1).trim())
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private Optional<String> parseSigner(String output) {
        Matcher matcher = CN_PATTERN.matcher(output);
        if (matcher.find()) {
            String quoted = matcher.group(1);
            String unquoted = matcher.group(2);
            String value = quoted == null ? unquoted : quoted;
            return Optional.of(value.trim());
        }
        return Optional.empty();
    }

    private String escapePowerShellSingleQuoted(String value) {
        return value.replace("'", "''");
    }
}
