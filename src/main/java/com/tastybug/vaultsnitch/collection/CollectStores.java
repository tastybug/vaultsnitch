package com.tastybug.vaultsnitch.collection;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.json.Json;
import io.github.jopenlibs.vault.json.JsonObject;
import io.github.jopenlibs.vault.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

public class CollectStores implements Function<Vault, CollectStores.Result> {

    private static final Logger logger = LoggerFactory.getLogger(CollectStores.class);

    @Override
    public Result apply(Vault vault) {
        try {
            List<String> kv2Mounts = getKv2Mounts(vault);
            return new Result(vault, kv2Mounts);
        } catch (Exception e) {
            logger.error("Error while enumerating stores.", e);
            return new Result(e);
        }
    }

    private List<String> getKv2Mounts(Vault vault) throws VaultException {
        byte[] body = vault.sys().mounts().list().getRestResponse().getBody();
        JsonObject allMounts = Json.parse(new String(body)).asObject();
        return allMounts.names().stream()
                .filter(name -> isKv2Mount(allMounts, name))
                .map(name -> name.replace("/", ""))
                .toList();
    }

    private static boolean isKv2Mount(JsonObject allMounts, String name) {
        try {
            JsonValue options = allMounts.get(name).asObject().get("options");
            if (options == null || options.isNull()) {
                return false;
            }
            return "2".equals(options.asObject().get("version").asString());
        } catch (Exception e) {
            return false;
        }
    }

    public static class Result {
        private Exception exception;
        private List<String> kv2Mounts;
        private Vault vault;

        Result(Exception e) {
            exception = e;
        }

        Result(Vault vault, List<String> kv2Mounts) {

            this.vault = vault;
            this.kv2Mounts = kv2Mounts;
        }

        public boolean isSuccess() {
            return exception == null;
        }

        public Vault getVault() {
            return vault;
        }

        public List<String> getKv2Mounts() {
            return kv2Mounts;
        }

        public Exception getException() {
            return exception;
        }
    }
}
