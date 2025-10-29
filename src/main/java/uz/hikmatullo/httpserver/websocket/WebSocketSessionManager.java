package uz.hikmatullo.httpserver.websocket;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


/**
 * Lightweight manager to track active WebSocketSession instances.
 * - register when session opens
 * - unregister when session cleans up
 * - broadcast utility and graceful closeAll
 */
public class WebSocketSessionManager {
    private final Logger log = LoggerFactory.getLogger(WebSocketSessionManager.class);


    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();


    public void register(WebSocketSession session) {
        if (session == null) return;
        sessions.put(session.getId(), session);
        log.info("registered session={} remote={}", session.getId(), session.getRemoteAddress());
    }


    public void unregister(WebSocketSession session) {
        if (session == null) return;
        sessions.remove(session.getId());
        log.info("unregistered session={}", session.getId());
    }


    public Collection<WebSocketSession> getAll() {
        return sessions.values();
    }


    /**
     * Gracefully close all sessions. Best-effort: send close frame and wait briefly for shutdown.
     */
    public void closeAll() {
        log.info("Initiating graceful close of all WebSocket sessions...");
        for (WebSocketSession s : sessions.values()) {
            try {
                if (s.isOpen()) {
                    try {
                        s.sendClose(1001, "Server shutting down");
                    } catch (IOException e) {
                        // If we fail to send close, just attempt to close socket from session side
                        log.info("Failed to send close frame to {}: {}", s.getId(), e.getMessage());
                        try {
                            s.closeUnderlying();
                        } catch (Exception ex) {
                            log.info("Failed to force-close session {}: {}", s.getId(), ex.getMessage());
                        }
                    }
                }
            } catch (Throwable t) {
                log.info("Error while closing session {}: {}", s.getId(), t.getMessage());
            }
        }


        // Wait briefly for sessions to finish their cleanup loops
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }


        // Force clear registry
        sessions.clear();
        log.info("[SessionRegistry] closeAll completed.");
    }
}