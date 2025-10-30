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
import uz.hikmatullo.httpserver.exception.HttpParsingException;
import uz.hikmatullo.httpserver.websocket.WebSocketSession;
import uz.hikmatullo.httpserver.websocket.WebSocketSessionManager;
import uz.hikmatullo.httpserver.websocket.WebSocketUtils;
import uz.hikmatullo.httpserver.websocket.listener.WebSocketListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpConnectionHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(HttpConnectionHandler.class);

    private static final ExecutorService WS_EXECUTOR = Executors.newCachedThreadPool();
    private final Socket socket;
    private final RequestHandler requestHandler;
    private final WebSocketListener webSocketListener;
    private final WebSocketSessionManager webSocketSessionManager;
    public HttpConnectionHandler(Socket socket, RequestHandler requestHandler, WebSocketListener webSocketListener, WebSocketSessionManager webSocketSessionManager) {
        this.socket = socket;
        this.requestHandler = requestHandler;
        this.webSocketListener = webSocketListener;
        this.webSocketSessionManager = webSocketSessionManager;
    }

    @Override
    public void run() {
        log.debug("Connection handler started");
        boolean upgradedToWebSocket = false;

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {

            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            // Set a timeout so we don't hang forever waiting for data
            // Set timeout to 5 seconds. this means that we wait for 5 seconds for data to be available in the input stream
            // Data will not come immediately when socket is accepted or created, so we need to wait for it.
            //Data may come in chunks, it means GET may come then other chunks and then other chunks for example. "GET / HT" first, then "TP/1.1\r\nHost: ..."
            // when we read data inputStream.read(), we may wait up to 5 seconds for data to be available in the input stream each time.
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

                    //if this is websocket connection, we do not put time out. because data may come any time.
                    socket.setSoTimeout(0);

                    // perform handshake and transfer ownership
                    handleWebSocketUpgrade(request, outputStream);


                    WebSocketSession session = new WebSocketSession(socket, webSocketListener, webSocketSessionManager);
                    ExecutorsHolder.VIRTUAL_EXECUTOR.submit(session);

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

                log.debug("Request processed. keepAlive={}", keepAlive);

            } while (keepAlive && !socket.isClosed());

        } catch (HttpParsingException e) {
            sendErrorResponse(e.getErrorCode(), e.getMessage(), outputStream);
        }
        catch (SocketTimeoutException e) {
            log.debug("Keep-alive timeout reached, closing connection.");
        }catch (IOException e) {
            log.error("I/O error happened: {}", e.getMessage());
            sendErrorResponse(HttpStatusCode.INTERNAL_SERVER_ERROR, e.getMessage(), outputStream);
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            sendErrorResponse(HttpStatusCode.INTERNAL_SERVER_ERROR, e.getMessage(), outputStream);
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

    private void sendErrorResponse(HttpStatusCode status, String message, OutputStream outputStream) {
        try {
            byte[] bodyBytes = getError(status, message);
            HttpResponse response = new HttpResponse(status);
            response.addHeader("Content-Type", "text/html; charset=utf-8");
            response.addHeader("Content-Length", String.valueOf(bodyBytes.length));
            response.addHeader("Connection", "close");
            HttpHeaderDefaults.applyServerInfoHeaders(response);
            response.setBody(bodyBytes);

            response.write(outputStream);
            System.out.println("Error message sent: " + status.getCode());
        } catch (IOException e) {
            log.error("Failed to send error response: {}", e.getMessage());
        }
    }

    private byte[] getError(HttpStatusCode status, String message) {
        String reason = status.getReasonPhrase(); // e.g. "Bad Request"
        String body = "<html><body><h2>" + status.getCode() + " " + reason + "</h2>" +
                "<p>" + message + "</p></body></html>";

        return body.getBytes(StandardCharsets.UTF_8);
    }



    private void handleWebSocketUpgrade(HttpRequest request, OutputStream outputStream) throws IOException {
        String clientKey = request.getHeader("Sec-WebSocket-Key");
        if (clientKey == null || clientKey.isEmpty()) {
            sendErrorResponse(HttpStatusCode.BAD_REQUEST, "Missing Sec-WebSocket-Key", outputStream);
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
