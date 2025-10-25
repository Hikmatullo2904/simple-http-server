package uz.hikmatullo.httpserver.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.hikmatullo.httpserver.core.HttpHeaderDefaults;
import uz.hikmatullo.httpserver.core.HttpKeepAliveManager;
import uz.hikmatullo.httpserver.core.handler.RequestHandler;
import uz.hikmatullo.httpserver.core.model.HttpRequest;
import uz.hikmatullo.httpserver.core.model.HttpResponse;
import uz.hikmatullo.httpserver.core.model.HttpStatusCode;
import uz.hikmatullo.httpserver.core.parser.HttpParser;
import uz.hikmatullo.httpserver.websocket.WebSocketSession;
import uz.hikmatullo.httpserver.websocket.WebSocketUtils;
import uz.hikmatullo.httpserver.websocket.listener.EchoWebSocketListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpConnectionHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(HttpConnectionHandler.class);

    private static final ExecutorService WS_EXECUTOR = Executors.newCachedThreadPool();
    private final Socket socket;
    private final RequestHandler requestHandler;
    public HttpConnectionHandler(Socket socket, RequestHandler requestHandler) {
        this.socket = socket;
        this.requestHandler = requestHandler;
    }
    @Override
    public void run() {
        boolean upgradedToWebSocket = false;

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {

            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            // Set a timeout so we don't hang forever waiting for data
            socket.setSoTimeout(5000);

            HttpKeepAliveManager keepAliveManager = new HttpKeepAliveManager();
            boolean keepAlive;

            do {
                HttpParser parser = new HttpParser();
                HttpRequest request = parser.parse(inputStream);
                if (request == null) {
                    log.debug("Request is null");
                    break;
                }

                // --- Detect WebSocket upgrade BEFORE normal HTTP handling ---
                if (WebSocketUtils.isWebSocketUpgrade(request)) {
                    // perform handshake and transfer ownership
                    handleWebSocketUpgrade(request, outputStream);

                    // create WebSocketSession and run it on executor
                    // you might create a listener based on request path or headers
                    WebSocketSession session = new WebSocketSession(socket, new EchoWebSocketListener());
                    WS_EXECUTOR.submit(session);

                    // IMPORTANT: after upgrade we must NOT close socket or streams here.
                    // Hand-off is complete; stop HTTP loop and return.
                    upgradedToWebSocket = true;
                    return;
                }

                // --- Handle request ---
                HttpResponse response = requestHandler.handle(request);

                // --- Handle Keep-Alive ---
                keepAlive = keepAliveManager.shouldKeepAlive(request);

                //Setting default headers and connection header based on keepAlive
                HttpHeaderDefaults.applyDefaultResponseHeaders(response, keepAlive);

                // --- Send Response ---
                response.write(outputStream);

                log.info("Request processed. keepAlive={}", keepAlive);

            } while (keepAlive && !socket.isClosed());

        } catch (SocketTimeoutException e) {
            log.debug("Keep-alive timeout reached, closing connection.");
        }catch (IOException e) {
            log.error("I/O error happened: {}", e.getMessage());
            sendErrorResponse(socket, HttpStatusCode.INTERNAL_SERVER_ERROR.statusCode, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            sendErrorResponse(socket, HttpStatusCode.INTERNAL_SERVER_ERROR.statusCode, e.getMessage());
        } finally {
            if (!upgradedToWebSocket)
                closeConnection(inputStream, outputStream);
        }
    }



    private void closeConnection(InputStream inputStream, OutputStream outputStream) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                log.error("Could not close socket. {}", e.getMessage());
            }
        }

        if(inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error("Could not close input stream. {}", e.getMessage());
            }
        }

        if(outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                log.error("Could not close output stream. {}", e.getMessage());
            }
        }
    }



    private void sendErrorResponse(Socket socket, int statusCode, String message) {
        try {
            String response = "HTTP/1.1 " + statusCode + " " + message + "\r\n" +
                    "Content-Length: 0\r\n" +
                    "Connection: close\r\n\r\n";

            OutputStream os = socket.getOutputStream();
            os.write(response.getBytes());
            os.flush();
            System.out.println("Error message is sent");
        } catch (IOException e) {
            log.error("Failed to send error response: {}", e.getMessage());
        }
    }


    private void handleWebSocketUpgrade(HttpRequest request, OutputStream outputStream) throws IOException {
        String clientKey = request.getHeader("Sec-WebSocket-Key");
        if (clientKey == null || clientKey.isEmpty()) {
            sendErrorResponse(socket, HttpStatusCode.BAD_REQUEST.statusCode, "Missing Sec-WebSocket-Key");
            return;
        }

        String accept = WebSocketUtils.computeAccept(clientKey);

        HttpResponse response = new HttpResponse(HttpStatusCode.SWITCHING_PROTOCOLS);
        response.addHeader("Upgrade", "websocket");
        response.addHeader("Connection", "Upgrade");
        response.addHeader("Sec-WebSocket-Accept", accept);

        response.write(outputStream);
        log.info("WebSocket handshake completed for remoteAddress={}", socket.getRemoteSocketAddress());
    }


}
