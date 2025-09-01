package com.tastybug.vaultsnitch.collection;

import com.tastybug.vaultsnitch.collection.CollectStoreContents.Result.DataAtPath;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.api.Logical;
import io.github.jopenlibs.vault.response.DataMetadata;
import io.github.jopenlibs.vault.response.LogicalResponse;
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
            Map<String, DataAtPath> pathsAndData = traverseSecretPaths(vaultClient, kv2Mounts);
            return new Result(kv2Mounts, pathsAndData);
        } catch (Exception e) {
            logger.error("Error while collecting secrets.", e);
            return new Result(e);
        }
    }

    private Map<String, DataAtPath> traverseSecretPaths(Logical logical, List<String> kv2Mounts) throws VaultException {
        Map<String, DataAtPath> results = new HashMap<>();
        for (String mount : kv2Mounts) {
            traverseSecretPaths(logical, mount.endsWith("/") ? mount : mount + "/", results);
        }
        return results;
    }


    private void traverseSecretPaths(Logical logical, String path, Map<String, DataAtPath> secretMap) throws VaultException {
        List<String> children = logical.list(path).getListData();
        if (children == null) {
            return;
        }
        for (String child : children) {
            if (child.endsWith("/")) {
                traverseSecretPaths(logical, path+child, secretMap);
            } else {
                LogicalResponse read = logical.read(path + child);
                Map<String, String> data = read.getData();
                DataMetadata dataMetadata = read.getDataMetadata();
                secretMap.put(path+child, new DataAtPath(data, dataMetadata));
            }
        }
    }

    public static class Result {
        Exception exception;
        List<String> stores;
        Map<String, DataAtPath> result;

        Result(Exception e) {
            exception = e;
        }

        Result(List<String> stores, Map<String, DataAtPath> result) {

            this.stores = stores;
            this.result = result;
        }

        public boolean isSuccess() {
            return exception == null;
        }

        public Map<String, DataAtPath> getPathsAndSecrets() {
            return result;
        }

        public List<String> getStores() {
            return stores;
        }

        public Exception getException() {
            return exception;
        }

        public static class DataAtPath {
            private final Map<String, String> secretData;
            private final DataMetadata dataMetadata;

            public DataAtPath(Map<String, String> secretData, DataMetadata dataMetadata) {
                this.secretData = secretData;
                this.dataMetadata = dataMetadata;
            }

            public Map<String, String> getSecretData() {
                return secretData;
            }

            public DataMetadata getDataMetadata() {
                return dataMetadata;
            }
        }
    }
}
