package uz.hikmatullo.httpserver.websocket.io;

import uz.hikmatullo.httpserver.websocket.model.WebSocketOpcode;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Writes WebSocket frames to an OutputStream.
 * Server frames must NOT be masked.
 */
public class WebSocketFrameWriter {
    private static final int MAX_ALLOWED_SIZE = 16 * 1024 * 1024; // 16 MB

    public synchronized void writeText(OutputStream out, String message) throws IOException {
        byte[] data = message == null ? new byte[0] : message.getBytes(StandardCharsets.UTF_8);
        writeFrame(out, WebSocketOpcode.TEXT, data);
    }

    public synchronized void writeBinary(OutputStream out, byte[] data) throws IOException {
        writeFrame(out, WebSocketOpcode.BINARY, data);
    }

    public synchronized void writePing(OutputStream out, byte[] payload) throws IOException {
        writeFrame(out, WebSocketOpcode.PING, payload);
    }

    public synchronized void writePong(OutputStream out, byte[] payload) throws IOException {
        writeFrame(out, WebSocketOpcode.PONG, payload);
    }

    public synchronized void writeClose(OutputStream out, int code, String reason) throws IOException {
        byte[] reasonBytes = reason == null ? new byte[0] : reason.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[2 + reasonBytes.length];
        payload[0] = (byte) ((code >> 8) & 0xFF);
        payload[1] = (byte) (code & 0xFF);
        System.arraycopy(reasonBytes, 0, payload, 2, reasonBytes.length);
        writeFrame(out, WebSocketOpcode.CLOSE, payload);
    }

    // -------------------------------
    // internal low-level frame writer
    // -------------------------------
    private void writeFrame(OutputStream out, WebSocketOpcode opcode, byte[] payload) throws IOException {
        if (payload == null) payload = new byte[0];
        int len = payload.length;

        if (len > MAX_ALLOWED_SIZE)
            throw new IOException("Frame payload exceeds server limit");

        if (opcode.isControl() && len > 125)
            throw new IOException("Control frame payload too large");

        int b1 = 0x80 | opcode.code(); // FIN + opcode
        out.write(b1);

        // server frame -> no masking bit set
        if (len <= 125) {
            out.write(len);
        } else if (len <= 0xFFFF) {
            out.write(126);
            out.write((len >> 8) & 0xFF);
            out.write(len & 0xFF);
        } else {
            out.write(127);
            long length = len & 0xFFFFFFFFL;
            for (int i = 7; i >= 0; i--) {
                out.write((int) ((length >> (8 * i)) & 0xFF));
            }
        }

        if (len > 0) {
            out.write(payload);
        }

        out.flush();
    }
}
