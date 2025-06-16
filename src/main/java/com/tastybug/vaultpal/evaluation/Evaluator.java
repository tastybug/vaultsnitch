package com.tastybug.vaultpal.evaluation;

import com.tastybug.vaultpal.collection.CollectStoreContents;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.util.function.Function;

import static io.micrometer.prometheus.PrometheusConfig.DEFAULT;

public class Evaluator implements Function<CollectStoreContents.Result, PrometheusMeterRegistry> {

    @Override
    public PrometheusMeterRegistry apply(CollectStoreContents.Result input) {
        try {
            if (!input.isSuccess()) {
                throw input.getException();
            }
            PrometheusMeterRegistry prometheusMeterRegistry = new PrometheusMeterRegistry(DEFAULT);

            new AllSecretsGauge()
                    .andThen(new AllStoresGauge())
                    .accept(prometheusMeterRegistry, input);

            return prometheusMeterRegistry;
        } catch (Exception e) {
            return null;
        }
    }
}
