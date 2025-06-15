package com.tastybug.vaultpal;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.sys.mounts.MountPayload;
import io.github.jopenlibs.vault.api.sys.mounts.MountType;
import io.github.jopenlibs.vault.api.sys.mounts.Mounts;
import io.github.jopenlibs.vault.api.sys.mounts.TimeToLive;
import io.github.jopenlibs.vault.response.LogicalResponse;
import io.github.jopenlibs.vault.response.MountResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ScheduledTask implements Runnable {

    final VaultConfig config = new VaultConfig()
            .address("http://127.0.0.1:8200")
            .token("myroot");

    @Override
    public void run() {
        try {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
            config.build();
            final Vault vault = Vault.create(config);

            LogicalResponse secretReadResult = createStoreAndSecretInIt(vault);

            System.out.println("username=" + secretReadResult.getData().get("username"));
            System.out.println("password=" + secretReadResult.getData().get("password"));

            Mounts mounts = vault.sys().mounts();
            MountResponse list = mounts.list();
            System.out.println("Found the following mounts: " + list.getData().keySet().stream().toList());

        } catch (VaultException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    private static LogicalResponse createStoreAndSecretInIt(Vault vault) throws VaultException {
        String kvName = "kv2" + System.currentTimeMillis();

        // Create K/V v2 secret engine
        final MountPayload payload = new MountPayload()
                .defaultLeaseTtl(TimeToLive.of(86400, TimeUnit.SECONDS))
                .maxLeaseTtl(TimeToLive.of(86400, TimeUnit.SECONDS))
                .description("some description");

        final MountResponse response = vault.mounts().enable(kvName, MountType.KEY_VALUE_V2, payload);

        // Add a secret to the K/V v2 engine
        Map<String, Object> secretData = new HashMap<>();
        secretData.put("username", "admin");
        secretData.put("password", "s3cr3t");
        LogicalResponse writeSecret = vault.logical().write(kvName + "/my-secret", secretData);

        LogicalResponse secretReadResult = vault.withRetries(3, 1000)
                .logical()
                .read(kvName + "/my-secret"); // enginename/path
        return secretReadResult;
    }
}