package com.tastybug.vaultpal;

import com.tastybug.vaultpal.collection.CollectStoreContents;
import com.tastybug.vaultpal.collection.CollectStores;
import com.tastybug.vaultpal.evaluation.Evaluator;
import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ScheduledEvaluation {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicReference<String> lastPayload = new AtomicReference<>();
    private final Vault vault;

    public ScheduledEvaluation() throws VaultException {
        final VaultConfig config = new VaultConfig()
                .address("http://127.0.0.1:8200")
                .token("myroot");

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        config.build();
        vault = Vault.create(config);
        lastPayload.set("");
    }

    public void startScheduledTask(long period, TimeUnit unit) {
        scheduler.scheduleAtFixedRate(() -> {
            String payload = runEvaluations();
            lastPayload.set(payload);
        }, 0, period, unit);
    }

    public String getLastPayload() {
        return lastPayload.get();
    }

    private String runEvaluations() {
        try {
            PrometheusMeterRegistry registry = new CollectStores()
                    .andThen(new CollectStoreContents())
                    .andThen(new Evaluator())
                    .apply(vault);
            return registry.scrape();

        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}