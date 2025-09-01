package com.tastybug.vaultsnitch;

import fi.iki.elonen.NanoHTTPD;
import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        new Main();
    }

    public Main() throws IOException {
        ScheduledEvaluation eval = new ScheduledEvaluation(this::createVaultClient);
        eval.startScheduledTask(Settings.getPollIntervalSeconds(), SECONDS);

        HttpServer httpServer = new HttpServer(Settings.getServerPort(), ()->eval.getMostRecentMetricsData().get());
        httpServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    private Vault createVaultClient() {
        try {
            String vaultUrl = Settings.getVaultUrl();
            String vaultToken = Settings.getVaultToken();
            logger.info("Creating vault client: {}", vaultUrl);
            final VaultConfig config = new VaultConfig()
                    .address(vaultUrl)
                    .token(vaultToken)
                    .engineVersion(2);
            config.build();
            return Vault.create(config);
        } catch(VaultException ve) {
            logger.error("Error creating vault client.", ve);
            throw new RuntimeException("", ve);
        }
    }
}