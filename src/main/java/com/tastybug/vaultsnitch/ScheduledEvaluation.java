package com.tastybug.vaultsnitch;

import com.tastybug.vaultsnitch.collection.CollectStoreContents;
import com.tastybug.vaultsnitch.collection.CollectStores;
import com.tastybug.vaultsnitch.evaluation.Evaluator;
import io.github.jopenlibs.vault.Vault;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ScheduledEvaluation {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledEvaluation.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicReference<String> mostRecentMetricsData = new AtomicReference<>();
    private final Supplier<Vault> vaultClientSupplier;

    public ScheduledEvaluation(Supplier<Vault> vaultClientSupplier) {
        this.vaultClientSupplier = vaultClientSupplier;
    }

    public void startScheduledTask(long period, TimeUnit unit) {
        logger.info("Evaluation loop runs every: {} {}", period, unit.toString());
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
            logger.debug("Updated metrics model.");
            return Optional.of(registry.scrape());
            // TODO collection von auswertung trennen, damit die error messages genauer sein koennen
        } catch (Throwable t) {
            logger.warn("Failed to prepare metrics data", t);
            return Optional.empty();
        }
    }
}