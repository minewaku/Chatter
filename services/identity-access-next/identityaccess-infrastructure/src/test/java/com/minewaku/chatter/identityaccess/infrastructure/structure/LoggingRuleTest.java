package com.minewaku.chatter.identityaccess.infrastructure.structure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Logging rule: no log statement in infrastructure may carry a password, hash, raw token, access credential or
 * private key. The scan reads the production sources; the allow-list is deliberately empty.
 */
class LoggingRuleTest {

    private static final Path SOURCES = Path.of("src/main/java");
    private static final Pattern LOG_CALL =
            Pattern.compile("\\b(?:log|logger|LOG|LOGGER)\\s*\\.\\s*(?:trace|debug|info|warn|error)\\s*\\(([^;]*);");
    private static final Pattern SENSITIVE =
            Pattern.compile("(?i)password|hash|rawToken|refreshToken|accessToken|privateKey|token|secret|credential");
    private static final List<String> ALLOW_LIST = List.of();

    @Test
    void noLogStatementMentionsSensitiveValues() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                Matcher call = LOG_CALL.matcher(source);
                while (call.find()) {
                    String arguments = call.group(1);
                    Matcher sensitive = SENSITIVE.matcher(arguments);
                    if (sensitive.find()
                            && !ALLOW_LIST.contains(file.getFileName().toString())) {
                        violations.add(file + ": log call mentions '" + sensitive.group() + "'");
                    }
                }
            }
        }
        assertThat(violations).isEmpty();
    }

    @Test
    void productionCodeNeverPrintsToTheConsole() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                if (source.contains("System.out")
                        || source.contains("System.err")
                        || source.contains("printStackTrace")) {
                    offenders.add(file.toString());
                }
            }
        }
        assertThat(offenders).isEmpty();
    }

    @Test
    void theScannerActuallyDetectsASensitiveLogCall() {
        String bad = "log.info(\"created session for token {}\", rawToken);";

        assertThat(LOG_CALL.matcher(bad).find()).isTrue();
        assertThat(SENSITIVE.matcher(bad).find()).isTrue();
    }
}
