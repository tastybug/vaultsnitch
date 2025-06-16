package com.tastybug.vaultpal;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.Logical;
import io.github.jopenlibs.vault.api.sys.mounts.Mount;
import io.github.jopenlibs.vault.api.sys.mounts.MountType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class FindKvStores implements Function<Vault, FindKvStores.Result> {

    @Override
    public Result apply(Vault vault) {
        try {
            List<String> kv2Mounts = getKv2Mounts(vault);
            return new Result(vault, kv2Mounts);
        } catch (Exception e) {
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
