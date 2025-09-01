package com.tastybug.vaultsnitch.evaluation;

import com.tastybug.vaultsnitch.collection.CollectStoreContents;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.util.function.BiConsumer;

public class AllSecretsGauge implements BiConsumer<PrometheusMeterRegistry, CollectStoreContents.Result> {

    @Override
    public void accept(PrometheusMeterRegistry prometheusMeterRegistry,
                       CollectStoreContents.Result input) {

        prometheusMeterRegistry.gauge("vaultpal_secrets_total", input.getPathsAndSecrets().size());
    }
}
