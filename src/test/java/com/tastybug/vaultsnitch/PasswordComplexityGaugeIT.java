package com.tastybug.vaultsnitch;

import com.tastybug.vaultsnitch.collection.CollectStoreContents;
import com.tastybug.vaultsnitch.collection.CollectStores;
import com.tastybug.vaultsnitch.evaluation.Evaluator;
import com.tastybug.vaultsnitch.evaluation.PasswordComplexityGauge;
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

import static io.micrometer.prometheus.PrometheusConfig.DEFAULT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.utility.DockerImageName.parse;

@Testcontainers
public class PasswordComplexityGaugeIT implements TestHelper {

    private static final String VAULT_CONTAINER_NAME = "hashicorp/vault:1.19";
    private static final String TOKEN = "myroot";
    private static final String COMPLIANT_PASSWORD = "ComplexPassword12345678";
    private static final String NON_COMPLIANT_PASSWORD = "simple";

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
    void compliantPasswordEmitsViolationZeroWithCorrectTags() throws VaultException {
        createSecret(logical, kvName, "prod/db", Map.of("password", COMPLIANT_PASSWORD));

        PrometheusMeterRegistry prom = evaluate();

        assertThat(prom.scrape()).containsPattern("vaultsnitch_complexity_violation\\{[^}]+\\} 0\\.0");
        assertThat(prom.scrape()).contains("store=\"" + kvName + "\"");
        assertThat(prom.scrape()).contains("path=\"/prod/db\"");
        assertThat(prom.scrape()).contains("vault_url=");
        assertThat(prom.scrape()).contains("team=");
    }

    @Test
    void nonCompliantPasswordEmitsViolationOne() throws VaultException {
        createSecret(logical, kvName, "prod/db", Map.of("password", NON_COMPLIANT_PASSWORD));

        PrometheusMeterRegistry prom = evaluate();

        assertThat(prom.scrape()).containsPattern("vaultsnitch_complexity_violation\\{[^}]+\\} 1\\.0");
    }

    @Test
    void secretWithNoPasswordFieldProducesNoGaugeEntry() throws VaultException {
        createSecret(logical, kvName, "prod/db", Map.of("api_key", "somevalue"));

        PrometheusMeterRegistry prom = evaluate();

        assertThat(prom.scrape()).doesNotContain("vaultsnitch_complexity_violation");
    }

    @Test
    void gaugeIsAbsentWhenDisabled() throws VaultException {
        createSecret(logical, kvName, "prod/db", Map.of("password", NON_COMPLIANT_PASSWORD));

        PrometheusMeterRegistry prom = evaluateWithEnv(Map.of("PasswordComplexityGauge.Enabled", "false"));

        assertThat(prom.scrape()).doesNotContain("vaultsnitch_complexity_violation");
    }

    @Test
    void customRegexIsRespected() throws VaultException {
        createSecret(logical, kvName, "prod/db", Map.of("password", NON_COMPLIANT_PASSWORD));

        PrometheusMeterRegistry prom = evaluateWithEnv(Map.of("PasswordComplexityGauge.Regex", ".*"));

        assertThat(prom.scrape()).containsPattern("vaultsnitch_complexity_violation\\{[^}]+\\} 0\\.0");
    }

    private PrometheusMeterRegistry evaluate() {
        CollectStoreContents.Result result = new CollectStores()
                .andThen(new CollectStoreContents())
                .apply(vault);
        return new Evaluator().apply(result);
    }

    private PrometheusMeterRegistry evaluateWithEnv(Map<String, String> env) {
        CollectStoreContents.Result result = new CollectStores()
                .andThen(new CollectStoreContents())
                .apply(vault);
        PrometheusMeterRegistry prom = new PrometheusMeterRegistry(DEFAULT);
        new PasswordComplexityGauge(env).accept(prom, result);
        return prom;
    }
}
