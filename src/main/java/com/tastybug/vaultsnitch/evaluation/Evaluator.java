package com.tastybug.vaultsnitch.evaluation;

import com.tastybug.vaultsnitch.collection.CollectStoreContents;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.util.function.Function;

import static io.micrometer.prometheus.PrometheusConfig.DEFAULT;

public class Evaluator implements Function<CollectStoreContents.Result, PrometheusMeterRegistry> {

    @Override
    public PrometheusMeterRegistry apply(CollectStoreContents.Result input) {
        if (!input.isSuccess()) {
            throw new RuntimeException("Collection failed", input.getException());
        }
        PrometheusMeterRegistry prometheusMeterRegistry = new PrometheusMeterRegistry(DEFAULT);

        new AllSecretsGauge()
                .andThen(new AllStoresGauge())
                .andThen(new PasswordComplexityGauge(System.getenv()))
                .andThen(new SecretAgeGauge())
                .andThen(new SecretNeverRotatedGauge())
                .andThen(new SecretExpiryGauge())
                .accept(prometheusMeterRegistry, input);

        return prometheusMeterRegistry;
    }
}
