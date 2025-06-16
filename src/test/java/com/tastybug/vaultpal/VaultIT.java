package com.tastybug.vaultpal;

import io.github.jopenlibs.vault.SslConfig;
import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.Logical;
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
public class VaultIT implements TestHelper {

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
        assertThat(vaultInstance.isRunning()).isTrue();
        kvName = createKvStore(vault);
        assertThat(kvName).isNotBlank();
    }

    @AfterEach
    void tearDown() throws VaultException {
        disableCachedKvStores(vault);
    }

    @Test
    void canEnumerateAllSecrets() throws VaultException {
        String kv2Name = createKvStore(vault);
        createSecret(logical, kvName, "test1", Map.of("username", "test1"));
        createSecret(logical, kvName, "test2", Map.of("username", "test2"));
        createSecret(logical, kvName, "subfolder/test3", Map.of("username", "test3"));
        createSecret(logical, kvName, "subfolder/subsubfolder/test4", Map.of("username", "test4"));
        createSecret(logical, kv2Name, "folder/test5", Map.of("username", "test5"));

        TraverseStores.Result result = new FindKvStores()
                .andThen(new TraverseStores())
                .apply(vault);

        assertThat(result.getResult().keySet()).containsExactlyInAnyOrder(
                kvName + "/test1",
                kvName + "/test2",
                kvName + "/subfolder/test3",
                kvName + "/subfolder/subsubfolder/test4",
                kv2Name + "/folder/test5"
        );
    }

    @Test
    void canEnumerateKvStores() throws VaultException {
        String kvName2 = createKvStore(vault);

        FindKvStores.Result result = new FindKvStores().apply(vault);

        assertThat(result.getKv2Mounts()).containsExactlyInAnyOrder("secret", kvName, kvName2);
    }

    @Test
    void noStoresNoProblems() throws VaultException {
        disableCachedKvStores(vault);

        TraverseStores.Result result = new FindKvStores()
                .andThen(new TraverseStores())
                .apply(vault);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isEmpty();
    }
}
