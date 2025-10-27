package uz.hikmatullo.httpserver.websocket.io;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uz.hikmatullo.httpserver.websocket.model.WebSocketFrame;
import uz.hikmatullo.httpserver.websocket.model.WebSocketOpcode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebSocketFrameReaderTest {

    private WebSocketFrameReader reader;

    @BeforeAll
    void setUp() {
        reader = new WebSocketFrameReader();
    }

    // ----------------------
    // 1. TEXT FRAME TEST
    // ----------------------
    @Test
    void testReadTextFrame() {
        try {
            WebSocketFrame frame = reader.read(generateValidRawTextFrame());
            assertNotNull(frame);
            assertEquals(WebSocketOpcode.TEXT, frame.getOpcode());
            assertEquals("Bu gala dashli gala", new String(frame.getPayload(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            fail(e);
        }
    }

    // ----------------------
    // 2. BINARY FRAME TEST (masked as client->server must be)
    // ----------------------
    @Test
    void testReadBinaryFrame() {
        try {
            WebSocketFrame frame = reader.read(generateMaskedBinaryFrame());
            assertNotNull(frame);
            assertEquals(WebSocketOpcode.BINARY, frame.getOpcode());
            assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, frame.getPayload());
        } catch (IOException e) {
            fail(e);
        }
    }

    // ----------------------
    // 3. PING FRAME TEST (masked)
    // ----------------------
    @Test
    void testReadPingFrame() {
        try {
            WebSocketFrame frame = reader.read(generateMaskedPingFrame());
            assertNotNull(frame);
            assertEquals(WebSocketOpcode.PING, frame.getOpcode());
            assertEquals("ping", new String(frame.getPayload(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            fail(e);
        }
    }

    // ----------------------
    // 4. PONG FRAME TEST (masked)
    // ----------------------
    @Test
    void testReadPongFrame() {
        try {
            WebSocketFrame frame = reader.read(generateMaskedPongFrame());
            assertNotNull(frame);
            assertEquals(WebSocketOpcode.PONG, frame.getOpcode());
            assertEquals("pong", new String(frame.getPayload(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            fail(e);
        }
    }

    // ----------------------
    // 5. CLOSE FRAME TEST (masked)
    // ----------------------
    @Test
    void testReadCloseFrame() {
        try {
            WebSocketFrame frame = reader.read(generateMaskedCloseFrame());
            assertNotNull(frame);
            assertEquals(WebSocketOpcode.CLOSE, frame.getOpcode());
            byte[] payload = frame.getPayload();
            assertTrue(payload.length >= 2);
            int code = ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF);
            assertEquals(1000, code); // normal closure
            String reason = new String(payload, 2, payload.length - 2, StandardCharsets.UTF_8);
            assertEquals("Normal closure", reason);
        } catch (IOException e) {
            fail(e);
        }
    }

    // ----------------------
    // 6. INVALID OPCODE TEST
    // ----------------------
    @Test
    void testInvalidOpcode() {
        // First byte has invalid opcode 0xF (0x8F means FIN + opcode 0xF)
        byte[] data = new byte[]{
                (byte) 0x8F,
                (byte) 0x00
        };
        InputStream in = new ByteArrayInputStream(data);
        assertThrows(IllegalArgumentException.class, () -> reader.read(in));
    }

    // ----------------------
    // FRAME GENERATORS (masked as client frames)
    // ----------------------

    private InputStream generateValidRawTextFrame() {
        // Raw masked text frame for "Bu gala dashli gala"
        String rawData = "81 93 72 A0 25 1E 30 D5 05 79 13 CC 44 3E 16 C1 56 76 1E C9 05 79 13 CC 44";
        byte[] bytes = hexStringToByteArray(rawData);
        return new ByteArrayInputStream(bytes);
    }

    private InputStream generateMaskedBinaryFrame() {
        // FIN + BINARY (0x82), masked, len=5
        byte[] payload = new byte[]{1, 2, 3, 4, 5};
        byte[] mask = new byte[]{(byte)0xAA, (byte)0xBB, (byte)0xCC, (byte)0xDD}; // deterministic mask
        return new ByteArrayInputStream(buildMaskedFrame((byte)0x2, payload, mask));
    }

    private InputStream generateMaskedPingFrame() {
        byte[] payload = "ping".getBytes(StandardCharsets.UTF_8);
        byte[] mask = new byte[]{0x01, 0x02, 0x03, 0x04};
        return new ByteArrayInputStream(buildMaskedFrame((byte)0x9, payload, mask));
    }

    private InputStream generateMaskedPongFrame() {
        byte[] payload = "pong".getBytes(StandardCharsets.UTF_8);
        byte[] mask = new byte[]{0x05, 0x06, 0x07, 0x08};
        return new ByteArrayInputStream(buildMaskedFrame((byte)0xA, payload, mask));
    }

    private InputStream generateMaskedCloseFrame() {
        byte[] reason = "Normal closure".getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[2 + reason.length];
        payload[0] = (byte) ((1000 >> 8) & 0xFF);
        payload[1] = (byte) (1000 & 0xFF);
        System.arraycopy(reason, 0, payload, 2, reason.length);
        byte[] mask = new byte[]{0x11, 0x22, 0x33, 0x44};
        return new ByteArrayInputStream(buildMaskedFrame((byte)0x8, payload, mask));
    }

    // Helper: build masked client->server frame
    private byte[] buildMaskedFrame(byte opcode, byte[] payload, byte[] maskKey) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // b1: FIN=1, opcode
            out.write((byte)(0x80 | (opcode & 0x0F)));
            int len = payload == null ? 0 : payload.length;
            // b2: MASK bit set + payload length (we only use short lengths in tests)
            if (len <= 125) {
                out.write((byte)(0x80 | (len & 0x7F)));
            } else if (len <= 0xFFFF) {
                out.write((byte)(0x80 | 126));
                out.write((len >> 8) & 0xFF);
                out.write(len & 0xFF);
            } else {
                out.write((byte)(0x80 | 127));
                // write 8 bytes length (we don't use >2^32 sizes here)
                long longLen = len;
                for (int i = 7; i >= 0; i--) {
                    out.write((int)((longLen >> (8 * i)) & 0xFF));
                }
            }
            // write mask key
            out.write(maskKey);
            // write masked payload
            if (len > 0) {
                byte[] masked = new byte[len];
                for (int i = 0; i < len; i++) {
                    masked[i] = (byte) (payload[i] ^ maskKey[i % 4]);
                }
                out.write(masked);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e); // ByteArrayOutputStream won't actually throw here
        }
    }

    // Utility: convert space-separated hex to bytes
    private byte[] hexStringToByteArray(String hex) {
        hex = hex.replaceAll("\\s+", "");
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
