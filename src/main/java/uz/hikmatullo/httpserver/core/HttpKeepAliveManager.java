package uz.hikmatullo.httpserver.core;

import uz.hikmatullo.httpserver.core.model.HttpRequest;
import uz.hikmatullo.httpserver.core.model.HttpVersion;

public class HttpKeepAliveManager {

    private static final int KEEP_ALIVE_TIMEOUT = 5000; // ms
    private static final int MAX_REQUESTS = 100;

    private int requestCount = 0;
    private long lastRequestTime = System.currentTimeMillis();


    public boolean shouldKeepAlive(HttpRequest request) {
        requestCount++;

        // If client explicitly asks to close
        String connectionHeader = request.getHeader("Connection");
        if (connectionHeader != null && connectionHeader.equalsIgnoreCase("close")) {
            return false;
        }

        // Timeout or max request limit reached
        if (System.currentTimeMillis() - lastRequestTime > KEEP_ALIVE_TIMEOUT) {
            return false;
        }

        if (requestCount >= MAX_REQUESTS) {
            return false;
        }

        // Default HTTP/1.1 behavior: keep-alive ON unless told otherwise
        boolean keepAlive = request.getHttpVersion().equals(HttpVersion.HTTP_1_1);
        lastRequestTime = System.currentTimeMillis();
        return keepAlive;
    }
}
