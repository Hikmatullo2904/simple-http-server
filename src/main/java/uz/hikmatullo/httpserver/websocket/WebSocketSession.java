package uz.hikmatullo.httpserver.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.hikmatullo.httpserver.websocket.io.WebSocketFrameReader;
import uz.hikmatullo.httpserver.websocket.io.WebSocketFrameWriter;
import uz.hikmatullo.httpserver.websocket.model.WebSocketFrame;
import uz.hikmatullo.httpserver.websocket.model.WebSocketOpcode;
import uz.hikmatullo.httpserver.websocket.listener.WebSocketListener;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages a single WebSocket connection. Reads frames, dispatches to the listener,
 * and exposes send methods for the application.
 *
 * Usage:
 *   WebSocketSession session = new WebSocketSession(socket, listener);
 *   executor.submit(session); // or new Thread(session).start();
 */
public class WebSocketSession implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(WebSocketSession.class);

    private final Socket socket;
    private final WebSocketListener listener;
    private final WebSocketFrameReader reader;
    private final WebSocketFrameWriter writer;
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final String id;

    public WebSocketSession(Socket socket, WebSocketListener listener) throws IOException {
        this.socket = socket;
        this.listener = listener;
        this.reader = new WebSocketFrameReader();
        this.writer = new WebSocketFrameWriter();
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public void run() {
        try (InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            // Notify listener that the connection is open
            try {
                listener.onOpen(this);
            } catch (Throwable t) {
                // Listener bug shouldn't break the session initialization
                log.warn("Listener.onOpen threw", t);
            }

            while (open.get() && !socket.isClosed()) {
                WebSocketFrame frame;
                try {
                    frame = reader.read(in);
                    if (frame == null) {
                        log.debug("frame is null");
                        // Stream closed cleanly by remote
                        break;
                    }
                } catch (EOFException eof) {
                    // Remote closed connection abruptly
                    log.debug("EOF received, closing session {}", id);
                    break;
                }

                try {
                    handleFrame(frame, out);
                } catch (Throwable t) {
                    listener.onError(this, t);
                    // On protocol error, attempt to close with 1002
                    try {
                        sendClose(1002, "Protocol error");
                    } catch (IOException e) {
                        log.debug("Failed to send close frame", e);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            // network I/O broken
            listener.onError(this, e);
        } finally {
            // Ensure we inform listener and cleanup
            cleanup();
        }
    }

    private void handleFrame(WebSocketFrame frame, OutputStream out) throws IOException {
        WebSocketOpcode opcode = frame.getOpcode();

        switch (opcode) {
            case TEXT -> {
                String text = new String(frame.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
                try {
                    listener.onTextMessage(this, text);
                } catch (Throwable t) {
                    listener.onError(this, t);
                }
            }
            case BINARY -> {
                try {
                    listener.onBinaryMessage(this, frame.getPayload());
                } catch (Throwable t) {
                    listener.onError(this, t);
                }
            }
            case PING -> {
                // Automatically reply with PONG (same payload)
                try {
                    writer.writePong(out, frame.getPayload());
                } catch (IOException e) {
                    throw e;
                }
                try {
                    listener.onPing(this, frame.getPayload());
                } catch (Throwable t) {
                    listener.onError(this, t);
                }
            }
            case PONG -> {
                try {
                    listener.onPong(this, frame.getPayload());
                } catch (Throwable t) {
                    listener.onError(this, t);
                }
            }
            case CLOSE -> {
                // Parse optional close code and reason
                int code = -1;
                String reason = "";
                byte[] p = frame.getPayload();
                if (p != null && p.length >= 2) {
                    code = ((p[0] & 0xFF) << 8) | (p[1] & 0xFF);
                    if (p.length > 2) {
                        reason = new String(p, 2, p.length - 2, java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
                // Echo close if we are still open (per RFC)
                if (open.getAndSet(false)) {
                    try {
                        writer.writeClose(out, code, reason);
                    } catch (IOException ignored) {
                        // ignore; we're closing anyway
                    }
                }
                try {
                    listener.onClose(this, code, reason);
                } catch (Throwable t) {
                    listener.onError(this, t);
                }
                // After close handshake, close socket
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
            default -> {
                // Unknown/unsupported opcode -> protocol error
                throw new IOException("Unsupported opcode: " + opcode);
            }
        }
    }

    // -------------------
    // Public API methods
    // -------------------

    /**
     * Synchronously send a text message (UTF-8).
     */
    public void sendText(String text) throws IOException {
        ensureOpen();
        synchronized (writer) {
            try (OutputStream out = socket.getOutputStream()) {
                writer.writeText(out, text);
            }
        }
    }

    /**
     * Synchronously send binary data.
     */
    public void sendBinary(byte[] data) throws IOException {
        ensureOpen();
        synchronized (writer) {
            try (OutputStream out = socket.getOutputStream()) {
                writer.writeBinary(out, data);
            }
        }
    }

    /**
     * Send a ping frame.
     */
    public void sendPing(byte[] payload) throws IOException {
        ensureOpen();
        synchronized (writer) {
            try (OutputStream out = socket.getOutputStream()) {
                writer.writePing(out, payload);
            }
        }
    }

    /**
     * Send a close frame then close the socket.
     */
    public void sendClose(int statusCode, String reason) throws IOException {
        if (!open.getAndSet(false)) return;
        synchronized (writer) {
            try (OutputStream out = socket.getOutputStream()) {
                writer.writeClose(out, statusCode, reason);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.debug("Failed to close socket", e);
                }
            }
        }
    }

    // -------------------
    // Utilities
    // -------------------

    private void ensureOpen() throws IOException {
        if (!open.get()) throw new IOException("WebSocketSession is closed");
        if (socket.isClosed()) {
            open.set(false);
            throw new IOException("Underlying socket is closed");
        }
    }

    private void cleanup() {
        if (!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
        open.set(false);
    }

    public boolean isOpen() {
        return open.get() && !socket.isClosed();
    }

    public String getId() {
        return id;
    }

    public String getRemoteAddress() {
        return socket.getRemoteSocketAddress() == null ? "unknown" : socket.getRemoteSocketAddress().toString();
    }
}
