package uz.hikmatullo.httpserver.websocket.listener;

import uz.hikmatullo.httpserver.websocket.WebSocketSession;

/**
 * Application-level callback interface for WebSocket events.
 */
public interface WebSocketListener {
    /**
     * Called when the session is established and open.
     */
    void onOpen(WebSocketSession session);

    /**
     * Called when a text message (UTF-8) is received.
     */
    void onTextMessage(WebSocketSession session, String text);

    /**
     * Called when binary data is received.
     */
    void onBinaryMessage(WebSocketSession session, byte[] data);

    /**
     * Called when a Ping control frame is received.
     * The session will automatically reply with Pong; this is for notification.
     */
    void onPing(WebSocketSession session, byte[] payload);

    /**
     * Called when a Pong control frame is received.
     */
    void onPong(WebSocketSession session, byte[] payload);

    /**
     * Called when the session is closed (remote or local).
     *
     * @param statusCode close code if present (-1 otherwise)
     * @param reason human-readable reason (may be empty)
     */
    void onClose(WebSocketSession session, int statusCode, String reason);

    /**
     * Called when an error occurs in the session.
     */
    void onError(WebSocketSession session, Throwable error);
}
