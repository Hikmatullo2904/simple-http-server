package uz.hikmatullo.httpserver.websocket.model;

/**
 * WebSocket frame opcodes as defined by RFC 6455 §5.2.
 */
public enum WebSocketOpcode {
    CONTINUATION(0x0),
    TEXT(0x1),
    BINARY(0x2),
    CLOSE(0x8),
    PING(0x9),
    PONG(0xA);

    private final int code;

    WebSocketOpcode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static WebSocketOpcode fromCode(int code) {
        for (WebSocketOpcode op : values()) {
            if (op.code == code) return op;
        }
        throw new IllegalArgumentException("Unknown opcode: " + code);
    }
}
