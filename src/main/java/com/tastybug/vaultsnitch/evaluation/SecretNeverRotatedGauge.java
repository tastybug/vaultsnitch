package com.tastybug.vaultsnitch.evaluation;

import com.tastybug.vaultsnitch.Settings;
import com.tastybug.vaultsnitch.collection.CollectStoreContents;
import io.micrometer.core.instrument.Tag;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;
import java.util.function.BiConsumer;

public class SecretNeverRotatedGauge implements BiConsumer<PrometheusMeterRegistry, CollectStoreContents.Result> {

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
        Long version = dataAtPath.getDataMetadata().getVersion();
        if (version == null) return;

        Set<Tag> tags = Set.of(
                Tag.of("store", storeAndPath.getLeft()),
                Tag.of("path", storeAndPath.getRight()),
                Tag.of("vault_url", Settings.getVaultUrl()),
                Tag.of("team", dataAtPath.getTeam())
        );
        registry.gauge("vaultsnitch_secret_never_rotated", tags, version == 1 ? 1 : 0);
    }
}
