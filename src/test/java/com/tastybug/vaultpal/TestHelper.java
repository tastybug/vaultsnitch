package com.tastybug.vaultpal;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.sys.mounts.MountPayload;
import io.github.jopenlibs.vault.api.sys.mounts.MountType;
import io.github.jopenlibs.vault.api.sys.mounts.TimeToLive;
import io.github.jopenlibs.vault.response.LogicalResponse;
import io.github.jopenlibs.vault.response.MountResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus.SC_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;

public interface TestHelper {

    default String createStore(Vault vault) throws VaultException {
        String kvName = "kv2" + System.currentTimeMillis();

        // Create K/V v2 secret engine
        final MountPayload payload = new MountPayload()
                .defaultLeaseTtl(TimeToLive.of(86400, TimeUnit.SECONDS))
                .maxLeaseTtl(TimeToLive.of(86400, TimeUnit.SECONDS))
                .description("some description");

        final MountResponse response = vault.mounts().enable(kvName, MountType.KEY_VALUE_V2, payload);
        assertThat(response.getRestResponse().getStatus()).isEqualTo(SC_NO_CONTENT);

        return kvName;
    }

    default LogicalResponse createSecret(Vault vault, String kvName, String path, Map<String, Object> content) throws VaultException {

        Map<String, Object> secretData = new HashMap<>();
        secretData.put("username", "admin");
        secretData.put("password", "s3cr3t");
        LogicalResponse response = vault.logical().write(kvName + "/my-secret", secretData);
        assertThat(response.getRestResponse().getStatus()).isEqualTo(SC_NO_CONTENT);
        return response;

    }
}
