package com.tastybug;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends NanoHTTPD {
    public Main() throws IOException {
        super(8080);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    public static void main(String[] args) throws IOException {
        // Start the NanoHTTPD server
        new Main();

        // Start the scheduled task
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new ScheduledTask(), 0, 10, TimeUnit.SECONDS);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if ("/health".equals(uri)) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\": \"UP\"}");
        } else if ("/metrics".equals(uri)) {
            String metrics = "# HELP app_up Indicates if the application is up\n" +
                    "# TYPE app_up gauge\n" +
                    "app_up 1\n";
            return newFixedLengthResponse(Response.Status.OK, "text/plain", metrics);
        } else {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
        }
    }
}