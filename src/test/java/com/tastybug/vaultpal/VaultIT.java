package com.tastybug.vaultpal;

import io.github.jopenlibs.vault.SslConfig;
import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.utility.DockerImageName.parse;

@Testcontainers
public class VaultIT implements TestHelper{

    private static final String VAULT_CONTAINER_NAME = "hashicorp/vault:1.19";

    @Container
    private static final GenericContainer<?> vaultInstance = new GenericContainer<>(parse(VAULT_CONTAINER_NAME))
            .withExposedPorts(8200)
            .withEnv("VAULT_DEV_ROOT_TOKEN_ID", "myroot");

    private static VaultConfig vaultConfig;
    private static Vault vault;

    private String kvName;

    @BeforeAll
    static void beforeAll() throws VaultException {
        vaultConfig = new VaultConfig()
                .address("http://127.0.0.1:" + vaultInstance.getMappedPort(8200))
                .sslConfig(new SslConfig().build())
                .token("myroot");
        vault = Vault.create(vaultConfig);
    }

    @BeforeEach
    void setUp() throws VaultException {
        kvName = createStore(vault);
        assertThat(kvName).isNotBlank();
    }

    @Test
    void name() {
        assertThat(vaultInstance.isRunning()).isTrue();
    }

}
