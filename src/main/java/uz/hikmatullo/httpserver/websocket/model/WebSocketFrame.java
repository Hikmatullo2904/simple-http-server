package uz.hikmatullo.httpserver.websocket.model;

import java.util.Arrays;

/**
 * Represents a single WebSocket frame.
 */
public final class WebSocketFrame {
    private final boolean fin;
    private final WebSocketOpcode opcode;
    private final byte[] payload;
    private final boolean masked;
    private final byte[] maskingKey;

    public WebSocketFrame(boolean fin, WebSocketOpcode opcode, byte[] payload,
                          boolean masked, byte[] maskingKey) {
        this.fin = fin;
        this.opcode = opcode;
        this.payload = payload;
        this.masked = masked;
        this.maskingKey = maskingKey;
    }

    public boolean isFin() {
        return fin;
    }

    public WebSocketOpcode getOpcode() {
        return opcode;
    }

    public byte[] getPayload() {
        return payload;
    }

    public boolean isMasked() {
        return masked;
    }

    public byte[] getMaskingKey() {
        return maskingKey;
    }


    @Override
    public String toString() {
        return "WebSocketFrame{" +
                "fin=" + fin +
                ", opcode=" + opcode +
                ", payloadLength=" + (payload != null ? payload.length : 0) +
                ", masked=" + masked +
                ", maskingKey=" + Arrays.toString(maskingKey) +
                ", payload=" + Arrays.toString(payload) +
                '}';
    }

}
