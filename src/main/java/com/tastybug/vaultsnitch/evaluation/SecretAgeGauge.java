package com.tastybug.vaultsnitch.evaluation;

import com.tastybug.vaultsnitch.Settings;
import com.tastybug.vaultsnitch.collection.CollectStoreContents;
import io.micrometer.core.instrument.Tag;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.function.BiConsumer;

public class SecretAgeGauge implements BiConsumer<PrometheusMeterRegistry, CollectStoreContents.Result> {

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
        var meta = dataAtPath.getDataMetadata();
        Set<Tag> tags = Set.of(
                Tag.of("store", storeAndPath.getLeft()),
                Tag.of("path", storeAndPath.getRight()),
                Tag.of("vault_url", Settings.getVaultUrl())
        );

        String createdTime = meta.getMetadataMap().get("created_time");
        if (createdTime != null && !createdTime.isBlank()) {
            long ageDays = ChronoUnit.DAYS.between(Instant.parse(createdTime), Instant.now());
            registry.gauge("vaultsnitch_secret_age_days", tags, ageDays);
        }

        Long version = meta.getVersion();
        if (version != null) {
            registry.gauge("vaultsnitch_secret_version", tags, version);
        }
    }
}
