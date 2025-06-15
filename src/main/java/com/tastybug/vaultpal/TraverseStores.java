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

public class TraverseStores implements Function<FindKvStores.Result, TraverseStores.Result> {

    @Override
    public Result apply(FindKvStores.Result storesDiscoveryResult) {
        try {
            if(!storesDiscoveryResult.isSuccess()) {
                throw storesDiscoveryResult.getException();
            }
            return new Result(traverseSecretPaths(
                    storesDiscoveryResult.getVault().logical(),
                    storesDiscoveryResult.getKv2Mounts()));
        } catch (Exception e) {
            return new Result(e);
        }
    }

    private List<String> getKv2Mounts(Vault vault) throws VaultException {
        Map<String, Mount> mounts = vault.sys().mounts().list().getMounts();
        return mounts.entrySet().stream()
                .filter(e -> e.getValue().getType() == MountType.KEY_VALUE)
                .map(Map.Entry::getKey)
                .toList();
    }


    private Map<String, Map<String, String>> traverseSecretPaths(Logical logical, List<String> kv2Mounts) throws VaultException {
        Map<String, Map<String, String>> results = new HashMap<>();
        for (String mount : kv2Mounts) {
            traverseSecretPaths(logical, mount.endsWith("/") ? mount : mount + "/", results);
        }
        return results;
    }


    private void traverseSecretPaths(Logical logical, String path, Map<String, Map<String, String>> secretMap) throws VaultException {
        List<String> children = logical.list(path).getListData();
        if (children == null) {
            return;
        }
        for (String child : children) {
            if (child.endsWith("/")) {
                traverseSecretPaths(logical, path+child, secretMap);
            } else {
                Map<String, String> data = logical.read(path+child).getData();
                secretMap.put(path+child, data);
            }
        }
    }

    public static class Result {
        Exception exception;
        Map<String, Map<String, String>> result;

        Result(Exception e) {
            exception = e;
        }

        Result(Map<String, Map<String, String>> result) {
            this.result = result;
        }

        public boolean isSuccess() {
            return exception == null;
        }

        public Map<String, Map<String, String>> getResult() {
            return result;
        }

        public Exception getException() {
            return exception;
        }
    }
}
