package com.url_detector;

import com.url_detector.web.WebServer;

public class Main {
    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                // Keep default when an invalid port is provided.
            }
        }

        try {
            WebServer server = new WebServer(port);
            server.start();
            System.out.println("URL Detector web app running at http://localhost:" + port);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to start web server", ex);
        }
    }
}