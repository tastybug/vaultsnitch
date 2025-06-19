package com.tastybug.vaultpal;

import com.tastybug.vaultpal.collection.CollectStoreContents;
import com.tastybug.vaultpal.collection.CollectStores;
import com.tastybug.vaultpal.evaluation.Evaluator;
import io.github.jopenlibs.vault.Vault;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScheduledEvaluation {

    private static final Logger LOGGER = Logger.getLogger(ScheduledEvaluation.class.getSimpleName());

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicReference<String> mostRecentMetricsData = new AtomicReference<>();
    private final Supplier<Vault> vaultClientSupplier;

    public ScheduledEvaluation(Supplier<Vault> vaultClientSupplier) {
        this.vaultClientSupplier = vaultClientSupplier;
    }

    public void startScheduledTask(long period, TimeUnit unit) {
        scheduler.scheduleAtFixedRate(this::run, 0, period, unit);
    }

    private void run() {
        runEvaluations()
                .ifPresent(mostRecentMetricsData::set);
    }

    public AtomicReference<String> getMostRecentMetricsData() {
        return mostRecentMetricsData;
    }

    private Optional<String> runEvaluations() {
        try {
            PrometheusMeterRegistry registry = new CollectStores()
                    .andThen(new CollectStoreContents())
                    .andThen(new Evaluator())
                    .apply(vaultClientSupplier.get());
            LOGGER.log(Level.FINE, "Updated metrics model.");
            return Optional.of(registry.scrape());
            // TODO collection von auswertung trennen, damit die error messages genauer sein koennen
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Failed to prepare metrics data", t);
            return Optional.empty();
        }
    }
}