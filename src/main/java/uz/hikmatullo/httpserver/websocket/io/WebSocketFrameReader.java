package uz.hikmatullo.httpserver.websocket.io;

import uz.hikmatullo.httpserver.websocket.model.WebSocketFrame;
import uz.hikmatullo.httpserver.websocket.model.WebSocketOpcode;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads WebSocket frames from an InputStream.
 * Handles unmasking and variable payload lengths.
 */
public class WebSocketFrameReader {

    public WebSocketFrame read(InputStream in) throws IOException {
        int b1 = in.read();
        if (b1 == -1) return null; // end of stream

        boolean fin = (b1 & 0x80) != 0;
        int opcodeVal = b1 & 0x0F;
        WebSocketOpcode opcode = WebSocketOpcode.fromCode(opcodeVal);

        int b2 = in.read();
        if (b2 == -1) throw new IOException("Unexpected EOF after first byte");

        boolean masked = (b2 & 0x80) != 0;
        long payloadLen = b2 & 0x7F;

        if (payloadLen == 126) {
            payloadLen = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
        } else if (payloadLen == 127) {
            payloadLen = 0;
            for (int i = 0; i < 8; i++) {
                payloadLen = (payloadLen << 8) | (in.read() & 0xFF);
            }
        }

        byte[] maskingKey = null;
        if (masked) {
            maskingKey = new byte[4];
            if (in.read(maskingKey) != 4)
                throw new IOException("Unexpected EOF reading masking key");
        }

        byte[] payload = new byte[(int) payloadLen];
        int read = 0;
        while (read < payloadLen) {
            int r = in.read(payload, read, (int) payloadLen - read);
            if (r == -1) throw new IOException("Unexpected EOF in payload");
            read += r;
        }

        if (masked && maskingKey != null) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (payload[i] ^ maskingKey[i % 4]);
            }
        }

        return new WebSocketFrame(fin, opcode, payload, masked, maskingKey);
    }
}
