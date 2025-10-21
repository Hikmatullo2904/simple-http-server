package uz.hikmatullo.httpserver.core;



import uz.hikmatullo.httpserver.core.model.HttpRequest;
import uz.hikmatullo.httpserver.core.model.HttpResponse;
import uz.hikmatullo.httpserver.core.model.HttpVersion;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Handles HTTP keep-alive (persistent connection) logic.
 * Decides whether the connection should stay open or close after response.
 */
public class HttpKeepAliveManager {

    private static final int DEFAULT_TIMEOUT = 5000; // 5 seconds
    private static final int DEFAULT_MAX_REQUESTS = 100;

    /**
     * Determines if this connection should be kept alive.
     */
    public static boolean shouldKeepAlive(HttpRequest request) {
        String connectionHeader = request.getHeader("Connection");

        if (request.getHttpVersion() == HttpVersion.HTTP_1_1) {
            // HTTP/1.1 keeps alive by default unless explicitly closed
            return !"close".equalsIgnoreCase(connectionHeader);
        } else {
            // HTTP/1.0 requires explicit keep-alive
            return "keep-alive".equalsIgnoreCase(connectionHeader);
        }
    }

    /**
     * Adds keep-alive headers to the response if applicable.
     */
    public static void applyHeaders(HttpResponse response, boolean keepAlive) {
        if (keepAlive) {
            response.addHeader("Connection", "keep-alive");
            response.addHeader("Keep-Alive",
                    String.format("timeout=%d, max=%d", DEFAULT_TIMEOUT / 1000, DEFAULT_MAX_REQUESTS));
        } else {
            response.addHeader("Connection", "close");
        }
    }

    /**
     * Writes response and closes or keeps socket open depending on connection state.
     */
    public static boolean sendResponse(Socket socket, OutputStream outputStream,
                                       HttpResponse response, boolean keepAlive) throws IOException {

        // Write response to client
        response.write(outputStream);
        outputStream.flush();

        if (!keepAlive) {
            socket.close();
            return false;
        }

        // If we’re keeping the socket alive, set timeout to avoid hanging
        socket.setSoTimeout(DEFAULT_TIMEOUT);
        return true;
    }
}
