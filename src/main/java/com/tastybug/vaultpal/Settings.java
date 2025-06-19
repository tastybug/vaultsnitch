package com.tastybug.vaultpal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

public class Settings {

    private static final Logger LOGGER = Logger.getLogger(Settings.class.getSimpleName());

    private static final String DEFAULT_VAULT_URL = "http://127.0.0.1:8200";
    private static final String VAULT_URL_ENV = System.getProperty("VAULT_URL", "http://127.0.0.1:8200");
    private static final String VAULT_TOKEN_FILE = System.getenv("VAULT_TOKEN_FILE");
    private static final String VAULT_TOKEN = System.getenv("VAULT_TOKEN");
    public static final String POLL_INTERVAL_SECS = System.getProperty("POLL_INTERVAL_SECS", "60");
    public static final String SERVER_PORT = System.getProperty("SERVER_PORT", "8080");

    public static int getServerPort() {
        return Integer.parseInt(SERVER_PORT);
    }

    public static int getPollIntervalSeconds() {
        return Integer.parseInt(POLL_INTERVAL_SECS);
    }

    public static String getVaultUrl() {
        if (Objects.nonNull(VAULT_URL_ENV)) {
            return VAULT_URL_ENV;
        } else {
            LOGGER.info("Using default Vault address: " + DEFAULT_VAULT_URL);
            return DEFAULT_VAULT_URL;
        }
    }

    public static String getVaultToken() {
        if (Objects.nonNull(VAULT_TOKEN_FILE)) {
            try {
                LOGGER.info("Reading secret from:" + VAULT_TOKEN_FILE);
                Path path = Path.of(VAULT_TOKEN_FILE);
                String value = Files.readString(path);
                return value.replaceAll("\n", "");
            } catch (IOException e) {
                LOGGER.severe("Error reading file: " + VAULT_TOKEN_FILE + " - " + e.getMessage());
                throw new RuntimeException("Failed to read file: " + VAULT_TOKEN_FILE, e);
            }
        } else {
            requireNonNull(VAULT_TOKEN, "No Vault token provided.");
            return VAULT_TOKEN;
        }
    }

}
