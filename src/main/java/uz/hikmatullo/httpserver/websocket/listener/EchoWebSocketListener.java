package uz.hikmatullo.httpserver.websocket.listener;


import uz.hikmatullo.httpserver.websocket.WebSocketSession;

import java.io.IOException;

// Create simple echo listener
public class EchoWebSocketListener implements WebSocketListener {
    @Override
    public void onOpen(WebSocketSession session) {
        System.out.println("WebSocketListener onOpen method is called");
        System.out.println("WebSocket open: " + session.getId());
    }
    @Override
    public void onTextMessage(WebSocketSession session, String text) {
        System.out.println("WebSocketListener onTextMessage method is called");
        System.out.println("Received: " + text);
        try { session.sendText("Echo: " + text); } catch (IOException ignored) {}
    }
    @Override
    public void onBinaryMessage(WebSocketSession session, byte[] d) {
        System.out.println("WebSocketListener onBinaryMessage method is called");
        try {
            session.sendBinary(d);
        } catch (IOException ignored) {
        }
    }
    @Override
    public void onPing(WebSocketSession session, byte[] p) {
        System.out.println("WebSocketListener onPing method is called");
    }
    @Override
    public void onPong(WebSocketSession session, byte[] p) {
        System.out.println("WebSocketListener onPong method is called");
    }
    @Override
    public void onClose(WebSocketSession session, int code, String reason) {
        System.out.println("WebSocketListener onClose method is called");
        System.out.println("Closed: " + code + " " + reason);
    }
    @Override
    public void onError(WebSocketSession session, Throwable error) {
        System.out.println("WebSocketListener onError is called");
        error.printStackTrace();
    }
}
