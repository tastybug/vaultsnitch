package com.tastybug.vaultsnitch.evaluation;

import com.tastybug.vaultsnitch.collection.CollectStoreContents;
import io.micrometer.core.instrument.Tag;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordLengthGauge implements BiConsumer<PrometheusMeterRegistry, CollectStoreContents.Result> {

    private static final String ENABLED = "PasswordLengthGauge.Enabled";
    private static final String REGEX = "PasswordLengthGauge.Regex";
    private static final String DEFAULT_PATTERN_RAW = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z]).{22,}$";


    private final boolean IS_ENABLED;
    private final Pattern PATTERN;

    public PasswordLengthGauge(Map<String, String> envProperties) {
        IS_ENABLED = Boolean.parseBoolean(envProperties.getOrDefault(ENABLED, "true"));
        String patternRaw = envProperties.getOrDefault(REGEX, DEFAULT_PATTERN_RAW);
        PATTERN = Pattern.compile(patternRaw);
    }

    @Override
    public void accept(PrometheusMeterRegistry promReg,
                       CollectStoreContents.Result input) {
        if (!IS_ENABLED) {
            return;
        }
        input.getPathsAndSecrets().entrySet().forEach(
                dataAtPath -> consumeData(
                        promReg,
                        getTeamAndPath(dataAtPath.getKey()),
                        dataAtPath.getValue()));
    }

    // this takes "JokerTeam/prod/oracle" and returns Pair.of("TeamName", "/prod/oracle")
    private static Pair<String,String> getTeamAndPath(String path) {
        String[] parts = path.split("/", 2);
        return Pair.of(parts.length > 0 ? parts[0] : "", parts.length > 1 ? "/" + parts[1] : "");
    }

    private void consumeData(PrometheusMeterRegistry registry,
                             Pair<String, String> teamNameAndPath,
                             CollectStoreContents.Result.DataAtPath dataAtPath) {
        // we only look at field `password` by convention
        Optional.ofNullable(dataAtPath.getSecretData().get("password"))
                .map(PATTERN::matcher)
                .map(Matcher::matches)
                .ifPresent(
                        matchesPattern -> {
                            registry.gauge("vaultsnitch_complexity_violation",
                                            Set.of(
                                                Tag.of("team", teamNameAndPath.getLeft()),
                                                Tag.of("path", teamNameAndPath.getRight())
                                            ),
                                            matchesPattern ? 0 : 1);
                        }
                );
    }
}
