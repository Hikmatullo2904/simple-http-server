package uz.hikmatullo.httpserver.websocket.io;

import uz.hikmatullo.httpserver.websocket.model.WebSocketFrame;
import uz.hikmatullo.httpserver.websocket.model.WebSocketOpcode;

import java.io.IOException;
import java.io.InputStream;

public class WebSocketFrameReader {
    private static final int MAX_ALLOWED_SIZE = 16 * 1024 * 1024; // 16 MB

    /**
     * Read a WebSocket frame from the input stream.
     * Raw data example: '81 84 C1 33 CD D9 89 5C A1 B8'. that is raw from of text "Hola" send by frontend
     * @param in this is input stream from client
     * @return WebSocketFrame object
     * @throws IOException if an I/O error occurs
     */
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
            int hi = in.read();
            int lo = in.read();
            if (hi == -1 || lo == -1)
                throw new IOException("Unexpected EOF reading 16-bit payload length");
            payloadLen = ((hi & 0xFF) << 8) | (lo & 0xFF);
        } else if (payloadLen == 127) {
            payloadLen = 0;
            for (int i = 0; i < 8; i++) {
                int b = in.read();
                if (b == -1)
                    throw new IOException("Unexpected EOF reading 64-bit payload length");
                payloadLen = (payloadLen << 8) | (b & 0xFF);
            }
        }

        if (opcode.isControl() && (!fin || payloadLen > 125))
            throw new IOException("Invalid control frame");

        if (payloadLen > MAX_ALLOWED_SIZE)
            throw new IOException("Frame exceeds server limit");

        if (!masked)
            throw new IOException("Client frame not masked (protocol violation)");

        byte[] maskingKey = new byte[4];
        if (in.read(maskingKey) != 4)
            throw new IOException("Unexpected EOF reading masking key");

        byte[] payload = new byte[(int) payloadLen];
        int read = 0;
        while (read < payloadLen) {
            int r = in.read(payload, read, (int) payloadLen - read);
            if (r == -1) throw new IOException("Unexpected EOF in payload");
            read += r;
        }

        // unmask
        for (int i = 0; i < payload.length; i++) {
            payload[i] ^= maskingKey[i % 4];
        }

        return new WebSocketFrame(fin, opcode, payload, masked, maskingKey);
    }

}
