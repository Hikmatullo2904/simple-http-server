package uz.hikmatullo.httpserver.websocket.listener;


import uz.hikmatullo.httpserver.websocket.WebSocketSession;

import java.io.IOException;

// Create simple echo listener
public class EchoWebSocketListener implements WebSocketListener {
    @Override
    public void onOpen(WebSocketSession session) {
        System.out.println("WebSocket open: " + session.getId());
    }
    @Override
    public void onTextMessage(WebSocketSession session, String text) {
        System.out.println("Received: " + text);
        try { session.sendText("Echo: " + text); } catch (IOException ignored) {}
    }
    @Override public void onBinaryMessage(WebSocketSession s, byte[] d) {}
    @Override public void onPing(WebSocketSession s, byte[] p) {}
    @Override public void onPong(WebSocketSession s, byte[] p) {}
    @Override public void onClose(WebSocketSession s, int code, String reason) {
        System.out.println("Closed: " + code + " " + reason);
    }
    @Override public void onError(WebSocketSession s, Throwable error) {
        error.printStackTrace();
    }
}
