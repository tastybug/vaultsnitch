package com.tastybug.vaultpal.evaluation;

import com.tastybug.vaultpal.collection.CollectStoreContents;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.util.Random;
import java.util.function.BiConsumer;

public class AllSecretsGauge implements BiConsumer<PrometheusMeterRegistry, CollectStoreContents.Result> {
    @Override
    public void accept(PrometheusMeterRegistry prometheusMeterRegistry,
                       CollectStoreContents.Result input) {

        prometheusMeterRegistry.gauge("vaultpal_secrets_total", new Random().nextInt(1000));
    }
}
