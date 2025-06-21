package com.tastybug.vaultpal.collection;

import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.Logical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CollectStoreContents implements Function<CollectStores.Result, CollectStoreContents.Result> {

    private static final Logger logger = LoggerFactory.getLogger(CollectStoreContents.class);

    @Override
    public Result apply(CollectStores.Result input) {
        try {
            if(!input.isSuccess()) {
                throw input.getException();
            }
            List<String> kv2Mounts = input.getKv2Mounts();
            Logical vaultClient = input.getVault().logical();
            Map<String, Map<String, String>> pathsAndData = traverseSecretPaths(vaultClient, kv2Mounts);
            return new Result(kv2Mounts, pathsAndData);
        } catch (Exception e) {
            logger.error("Error while collecting secrets.", e);
            return new Result(e);
        }
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
        List<String> stores;
        Map<String, Map<String, String>> result;

        Result(Exception e) {
            exception = e;
        }

        Result(List<String> stores, Map<String, Map<String, String>> result) {

            this.stores = stores;
            this.result = result;
        }

        public boolean isSuccess() {
            return exception == null;
        }

        public Map<String, Map<String, String>> getPathsAndSecrets() {
            return result;
        }

        public List<String> getStores() {
            return stores;
        }

        public Exception getException() {
            return exception;
        }
    }
}
