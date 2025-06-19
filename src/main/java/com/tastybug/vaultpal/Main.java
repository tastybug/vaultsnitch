package com.tastybug.vaultpal;

import fi.iki.elonen.NanoHTTPD;
import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Main extends NanoHTTPD {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getSimpleName());

    public static void main(String[] args) throws IOException, VaultException {
        new Main();
    }

    public Main() throws IOException, VaultException {
        super(Settings.getServerPort());

        ScheduledEvaluation evaluation = new ScheduledEvaluation(this::createVaultClient);
        evaluation.startScheduledTask(Settings.getPollIntervalSeconds(), SECONDS);

        HttpServer httpServer = new HttpServer(8080, ()->evaluation.getMostRecentMetricsData().get());
        httpServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    private Vault createVaultClient() {
        try {
            final VaultConfig config = new VaultConfig()
                    .address(Settings.getVaultUrl())
                    .token(Settings.getVaultToken())
                    .engineVersion(2);
            config.build();
            return Vault.create(config);
        } catch(VaultException ve) {
            LOGGER.log(Level.SEVERE, "Error creating vault client.", ve);
            throw new RuntimeException("", ve);
        }
    }
}