package com.tastybug.vaultsnitch.evaluation;

import com.tastybug.vaultsnitch.Settings;
import com.tastybug.vaultsnitch.collection.CollectStoreContents;
import io.micrometer.core.instrument.Tag;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.function.BiConsumer;

public class SecretExpiryGauge implements BiConsumer<PrometheusMeterRegistry, CollectStoreContents.Result> {

    private static final Logger logger = LoggerFactory.getLogger(SecretExpiryGauge.class);

    @Override
    public void accept(PrometheusMeterRegistry registry, CollectStoreContents.Result input) {
        input.getPathsAndSecrets().entrySet().forEach(entry ->
                consumeData(registry, getStoreAndPath(entry.getKey()), entry.getValue()));
    }

    private static Pair<String, String> getStoreAndPath(String path) {
        String[] parts = path.split("/", 2);
        return Pair.of(parts.length > 0 ? parts[0] : "", parts.length > 1 ? "/" + parts[1] : "");
    }

    private void consumeData(PrometheusMeterRegistry registry,
                             Pair<String, String> storeAndPath,
                             CollectStoreContents.Result.DataAtPath dataAtPath) {
        String expiresAt = dataAtPath.getExpiresAt();
        if (expiresAt == null) return;

        LocalDate expiryDate;
        try {
            expiryDate = LocalDate.parse(expiresAt);
        } catch (DateTimeParseException e) {
            logger.warn("Ignoring malformed expires_at_date '{}' for {}{}", expiresAt, storeAndPath.getLeft(), storeAndPath.getRight());
            return;
        }

        long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);

        Set<Tag> tags = Set.of(
                Tag.of("store", storeAndPath.getLeft()),
                Tag.of("path", storeAndPath.getRight()),
                Tag.of("vault_url", Settings.getVaultUrl()),
                Tag.of("team", dataAtPath.getTeam())
        );
        registry.gauge("vaultsnitch_secret_expires_in_days", tags, daysUntilExpiry);
    }
}
