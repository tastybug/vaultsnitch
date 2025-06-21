package com.tastybug.vaultpal.collection;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.sys.mounts.Mount;
import io.github.jopenlibs.vault.api.sys.mounts.MountType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CollectStores implements Function<Vault, CollectStores.Result> {

    private static final Logger logger = LoggerFactory.getLogger(CollectStoreContents.class);

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
        Map<String, Mount> mounts = vault.sys().mounts().list().getMounts();
        return mounts.entrySet().stream()
                .filter(e -> e.getValue().getType() == MountType.KEY_VALUE)
                .map(Map.Entry::getKey)
                .map(store -> store.replace("/", "")) // strip trailing slash
                .toList();
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
