package uz.hikmatullo.httpserver.websocket;

import uz.hikmatullo.httpserver.core.model.HttpRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class WebSocketUtils {
    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private WebSocketUtils() {}

    public static boolean isWebSocketUpgrade(HttpRequest request) {
        if (request == null) return false;
        String upgrade = request.getHeader("Upgrade");
        String connection = request.getHeader("Connection");
        String version = request.getHeader("Sec-WebSocket-Version");
        return upgrade != null
                && "websocket".equalsIgnoreCase(upgrade.trim())
                && connection != null
                && connection.toLowerCase().contains("upgrade")
                && "13".equals(version == null ? null : version.trim());
    }

    public static String computeAccept(String secWebSocketKey) {
        if (secWebSocketKey == null) throw new IllegalArgumentException("Sec-WebSocket-Key is null");
        try {
            String combined = secWebSocketKey.trim() + WS_GUID;
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }
}
