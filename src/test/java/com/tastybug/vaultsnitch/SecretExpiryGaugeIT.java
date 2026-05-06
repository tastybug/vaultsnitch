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

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.utility.DockerImageName.parse;

@Testcontainers
public class SecretExpiryGaugeIT implements TestHelper {

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
                .address("http://" + vaultInstance.getHost() + ":" + vaultInstance.getMappedPort(8200))
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
    void futureExpiryDateYieldsPositiveValue() throws Exception {
        createSecret(logical, kvName, "prod/db", Map.of("password", "s3cr3t"));
        String futureDate = LocalDate.now().plusDays(42).toString();
        setCustomMetadata(vaultAddress(), TOKEN, kvName, "prod/db", Map.of("expires_at_date", futureDate));

        String scrape = evaluate().scrape();

        assertThat(scrape).containsPattern("vaultsnitch_secret_expires_in_days\\{[^}]+\\} 4[0-2]\\.0");
    }

    @Test
    void pastExpiryDateYieldsNegativeValue() throws Exception {
        createSecret(logical, kvName, "prod/db", Map.of("password", "s3cr3t"));
        String pastDate = LocalDate.now().minusDays(10).toString();
        setCustomMetadata(vaultAddress(), TOKEN, kvName, "prod/db", Map.of("expires_at_date", pastDate));

        String scrape = evaluate().scrape();

        assertThat(scrape).containsPattern("vaultsnitch_secret_expires_in_days\\{[^}]+\\} -1[0-9]\\.0");
    }

    @Test
    void noExpiryDateMeansNoMetric() throws VaultException {
        createSecret(logical, kvName, "prod/db", Map.of("password", "s3cr3t"));

        String scrape = evaluate().scrape();

        assertThat(scrape).doesNotContain("vaultsnitch_secret_expires_in_days");
    }

    @Test
    void malformedExpiryDateMeansNoMetric() throws Exception {
        createSecret(logical, kvName, "prod/db", Map.of("password", "s3cr3t"));
        setCustomMetadata(vaultAddress(), TOKEN, kvName, "prod/db", Map.of("expires_at_date", "not-a-date"));

        String scrape = evaluate().scrape();

        assertThat(scrape).doesNotContain("vaultsnitch_secret_expires_in_days");
    }

    @Test
    void expiryGaugeCarriesCorrectTags() throws Exception {
        createSecret(logical, kvName, "prod/db", Map.of("password", "s3cr3t"));
        String futureDate = LocalDate.now().plusDays(30).toString();
        setCustomMetadata(vaultAddress(), TOKEN, kvName, "prod/db",
                Map.of("expires_at_date", futureDate, "team", "payments"));

        String scrape = evaluate().scrape();

        assertThat(scrape).contains("vaultsnitch_secret_expires_in_days");
        assertThat(scrape).contains("store=\"" + kvName + "\"");
        assertThat(scrape).contains("path=\"/prod/db\"");
        assertThat(scrape).contains("team=\"payments\"");
    }

    private String vaultAddress() {
        return "http://" + vaultInstance.getHost() + ":" + vaultInstance.getMappedPort(8200);
    }

    private PrometheusMeterRegistry evaluate() {
        CollectStoreContents.Result result = new CollectStores()
                .andThen(new CollectStoreContents())
                .apply(vault);
        return new Evaluator().apply(result);
    }
}
