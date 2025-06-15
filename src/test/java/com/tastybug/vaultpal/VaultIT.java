package com.tastybug.vaultpal;

import io.github.jopenlibs.vault.SslConfig;
import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.Logical;
import io.github.jopenlibs.vault.response.LogicalResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.utility.DockerImageName.parse;

@Testcontainers
public class VaultIT implements TestHelper{

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
        kvName = createStore(vault);
        assertThat(kvName).isNotBlank();
    }

    @Test
    void name() throws VaultException {
        createSecret(logical, kvName, "test1", Map.of("username", "test1"));
        createSecret(logical, kvName, "test2", Map.of("username", "test2"));
        createSecret(logical, kvName, "subfolder/test3", Map.of("username", "test3"));
        createSecret(logical, kvName, "subfolder/subsubfolder/test4", Map.of("username", "test4"));

        LogicalResponse list = vault.logical().list(kvName);
        List<String> listData = list.getListData();


        TraverseStores.Result result = new FindKvStores()
                .andThen(new TraverseStores())
                .apply(vault);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).hasSize(4);
    }
}
