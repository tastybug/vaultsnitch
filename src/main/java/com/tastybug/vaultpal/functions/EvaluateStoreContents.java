package com.tastybug.vaultpal.functions;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class EvaluateStoreContents implements Function<CollectStoreContents.Result, EvaluateStoreContents.Result> {

    @Override
    public Result apply(CollectStoreContents.Result input) {
        try {
            if (!input.isSuccess()) {
                throw input.getException();
            }

            List<Report> reports = input.getResult().entrySet()
                    .stream()
                    .map(this::evaluatePath)
                    .toList();

            return new Result(reports);
        } catch (Exception e) {
            return new Result(e);
        }
    }

    private Report evaluatePath(Map.Entry<String, Map<String, String>> stringMapEntry) {
        return new Report(stringMapEntry.getKey());
    }

    public static class Result {
        Exception exception;
        Collection<Report> result;

        Result(Exception e) {
            exception = e;
        }

        Result(Collection<Report> reports) {
            this.result = result;
        }

        public boolean isSuccess() {
            return exception == null;
        }

        public Collection<Report> getResult() {
            return result;
        }
    }

    public static class Report {
        String path;

        public Report(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }
}
