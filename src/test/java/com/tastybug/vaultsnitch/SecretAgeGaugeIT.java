package com.tastybug.vaultsnitch;

import com.tastybug.vaultsnitch.collection.CollectStoreContents;
import com.tastybug.vaultsnitch.collection.CollectStores;
import com.tastybug.vaultsnitch.evaluation.Evaluator;
import io.github.jopenlibs.vault.SslConfig;
import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.Logical;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.utility.DockerImageName.parse;

@Testcontainers
public class SecretAgeGaugeIT implements TestHelper {

    private static final String VAULT_CONTAINER_NAME = "hashicorp/vault:1.19";
    private static final String TOKEN = "myroot";

    @Container
    private static final GenericContainer<?> vaultInstance = new GenericContainer<>(parse(VAULT_CONTAINER_NAME))
            .withExposedPorts(8200)
            .withEnv("VAULT_DEV_ROOT_TOKEN_ID", TOKEN);

    private static Vault vault;
    private Logical logical;
    private String kvName;

    @BeforeAll
    static void beforeAll() throws VaultException {
        VaultConfig vaultConfig = new VaultConfig()
                .address("http://127.0.0.1:" + vaultInstance.getMappedPort(8200))
                .sslConfig(new SslConfig().build())
                .token(TOKEN);
        vault = Vault.create(vaultConfig);
    }

    @BeforeEach
    void setUp() throws VaultException {
        logical = vault.logical();
        kvName = createKvStore(vault);
    }

    @AfterEach
    void tearDown() throws VaultException {
        disableCachedKvStores(vault);
    }

    @Test
    void secretAgeIsExposedWithCorrectTags() throws VaultException {
        createSecret(logical, kvName, "prod/db", Map.of("password", "s3cr3t"));

        PrometheusMeterRegistry prom = evaluate();

        assertThat(prom.scrape()).contains("vaultsnitch_secret_age_days");
        assertThat(prom.scrape()).contains("store=\"" + kvName + "\"");
        assertThat(prom.scrape()).contains("path=\"/prod/db\"");
        assertThat(prom.scrape()).contains("vault_url=");
    }

    @Test
    void secretAgeIsNonNegative() throws VaultException {
        createSecret(logical, kvName, "dev/api-key", Map.of("key", "abc"));

        PrometheusMeterRegistry prom = evaluate();

        assertThat(prom.scrape()).containsPattern("vaultsnitch_secret_age_days\\{[^}]+\\} \\d+\\.0");
    }

    @Test
    void secretVersionIsExposed() throws VaultException {
        createSecret(logical, kvName, "prod/db", Map.of("password", "s3cr3t"));

        PrometheusMeterRegistry prom = evaluate();

        assertThat(prom.scrape()).contains("vaultsnitch_secret_version");
        assertThat(prom.scrape()).containsPattern("vaultsnitch_secret_version\\{[^}]+\\} 1\\.0");
    }

    private PrometheusMeterRegistry evaluate() {
        CollectStoreContents.Result result = new CollectStores()
                .andThen(new CollectStoreContents())
                .apply(vault);
        return new Evaluator().apply(result);
    }
}
