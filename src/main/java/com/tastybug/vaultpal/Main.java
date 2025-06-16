package com.tastybug.vaultpal;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static fi.iki.elonen.NanoHTTPD.Response.Status.OK;

public class Main extends NanoHTTPD {

    public static void main(String[] args) throws IOException {
        new Main();
    }

    private ScheduledEvaluation evaluation;

    public Main() throws IOException {
        super(8080);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

        // Start the scheduled task
        evaluation = new ScheduledEvaluation();
        evaluation.startScheduledTask(10, TimeUnit.SECONDS);
    }

    @Override
    public Response serve(IHTTPSession session) {

        String uri = session.getUri();
        if ("/health".equals(uri)) {
            return newFixedLengthResponse(OK, "application/json", "{\"status\": \"UP\"}");
        } else if ("GET".equals(session.getMethod().toString()) && "/metrics".equals(uri)) {
            return newFixedLengthResponse(
                    OK, "text/plain; version=0.0.4; charset=utf-8", evaluation.getLastPayload());
        } else {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
        }
    }

}