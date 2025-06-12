package com.tastybug;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        // Initialize Jetty server on port 8080
        Server server = new Server(8080);

        // Set up servlet handler
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        // Add servlets for /health and /metrics endpoints
        handler.addServletWithMapping(HealthServlet.class, "/health");
        handler.addServletWithMapping(MetricsServlet.class, "/metrics");

        // Start the scheduled task
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new ScheduledTask(), 0, 10, TimeUnit.SECONDS);

        // Start the server
        server.start();
        server.join();
    }

    // Servlet for /health endpoint
    public static class HealthServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"status\": \"UP\"}");
        }
    }

    // Servlet for /metrics endpoint
    public static class MetricsServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain");
            // Simple Prometheus-compatible metrics format
            resp.getWriter().write("# HELP app_up Indicates if the application is up\n" +
                    "# TYPE app_up gauge\n" +
                    "app_up 1\n");
        }
    }
}