package com.url_detector;

import com.url_detector.desktop.DesktopApp;
import com.url_detector.web.WebServer;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0) {
            String first = args[0] == null ? "" : args[0].trim().toLowerCase();
            if ("--help".equals(first) || "-h".equals(first)) {
                printUsage();
                return;
            }

            if ("--desktop".equals(first) || "desktop".equals(first)) {
                DesktopApp.main(new String[0]);
                return;
            }

            if ("--web".equals(first) || "web".equals(first)) {
                startWebMode(shiftArgs(args));
                return;
            }
        }

        // Default mode keeps existing behavior: web server on port 8080 unless overridden.
        startWebMode(args);
    }

    private static void startWebMode(String[] args) {
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

    private static String[] shiftArgs(String[] args) {
        if (args == null || args.length <= 1) {
            return new String[0];
        }

        String[] shifted = new String[args.length - 1];
        System.arraycopy(args, 1, shifted, 0, shifted.length);
        return shifted;
    }

    private static void printUsage() {
        System.out.println("URL Detector launcher");
        System.out.println("Usage:");
        System.out.println("  java -jar url_detector.jar               # start web mode on port 8080");
        System.out.println("  java -jar url_detector.jar 9090          # start web mode on custom port");
        System.out.println("  java -jar url_detector.jar --web 9090    # explicit web mode");
        System.out.println("  java -jar url_detector.jar --desktop     # desktop JavaFX mode");
    }
}