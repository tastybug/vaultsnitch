package com.tastybug.vaultpal;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.Logical;
import io.github.jopenlibs.vault.api.sys.mounts.MountPayload;
import io.github.jopenlibs.vault.api.sys.mounts.MountType;
import io.github.jopenlibs.vault.api.sys.mounts.TimeToLive;
import io.github.jopenlibs.vault.response.LogicalResponse;
import io.github.jopenlibs.vault.response.MountResponse;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus.SC_NO_CONTENT;
import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;

public interface TestHelper {

    Set<String> kvNames = new HashSet<>();

    default String createKvStore(Vault vault) throws VaultException {
        String store = createStore(vault, MountType.KEY_VALUE_V2);
        kvNames.add(store);
        return store;
    }

    default String createStore(Vault vault, MountType mountType) throws VaultException {
        String kvName = "kv2" + System.currentTimeMillis();

        // Create K/V v2 secret engine
        final MountPayload payload = new MountPayload()
                .defaultLeaseTtl(TimeToLive.of(86400, TimeUnit.SECONDS))
                .maxLeaseTtl(TimeToLive.of(86400, TimeUnit.SECONDS))
                .description("some description");

        final MountResponse response = vault.mounts().enable(kvName, mountType, payload);
        assertThat(response.getRestResponse().getStatus()).isEqualTo(SC_NO_CONTENT);

        return kvName;
    }

    default void disableCachedKvStores(Vault vault) throws VaultException {
        for (String store : kvNames.stream().toList() /*stupid clone to avoid concurrent mod*/) {
            MountResponse response = vault.sys().mounts().disable(store);
            assertThat(response.getRestResponse().getStatus()).isEqualTo(SC_NO_CONTENT);
            kvNames.remove(store);
        }
    }

    default LogicalResponse createSecret(Logical logical, String kvName, String path, Map<String, Object> content)
            throws VaultException {

        LogicalResponse response = logical.write(kvName + "/" + path, content);
        assertThat(response.getRestResponse().getStatus()).isEqualTo(SC_OK);
        return response;
    }

}
