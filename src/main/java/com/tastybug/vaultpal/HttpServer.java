package com.tastybug.vaultpal;

import fi.iki.elonen.NanoHTTPD;

import java.util.Optional;
import java.util.function.Supplier;

import static fi.iki.elonen.NanoHTTPD.Method.GET;
import static fi.iki.elonen.NanoHTTPD.Response.Status.*;
import static java.util.Optional.ofNullable;

public class HttpServer extends NanoHTTPD  {

    private final Supplier<String> promMetricsSupplier;

    public HttpServer(int port, Supplier<String> promMetricsSupplier) {
        super(port);
        this.promMetricsSupplier = promMetricsSupplier;
    }

    @Override
    public Response serve(IHTTPSession session) {

        String uri = session.getUri();
        if ("/liveness".equals(uri)) {
            return getDefaultLivenessResponse();
        } else if ("/readiness".equals(uri)) {
            String value = promMetricsSupplier.get();
            return getReadinessResponse(ofNullable(value));
        } else if (GET.name().equals(session.getMethod().toString()) && "/metrics".equals(uri)) {
            return newFixedLengthResponse(
                    OK,
                    "text/plain; version=0.0.4; charset=utf-8",
                    ofNullable(promMetricsSupplier.get()).orElse(""));
        } else {
            return newFixedLengthResponse(NOT_FOUND, MIME_PLAINTEXT, "Not Found");
        }
    }

    private static Response getDefaultLivenessResponse() {
        return newFixedLengthResponse(OK, "application/json", "{\"status\": \"UP\"}");
    }

    private static Response getReadinessResponse(Optional<String> lastPayload) {
        if (lastPayload.isEmpty()) {
            return newFixedLengthResponse(SERVICE_UNAVAILABLE, "application/json", "{\"status\": \"DOWN\"}");
        }
        return newFixedLengthResponse(OK, "application/json", "{\"status\": \"UP\"}");
    }
}
