package uz.hikmatullo.httpserver.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.hikmatullo.httpserver.core.handler.RequestHandler;
import uz.hikmatullo.httpserver.websocket.WebSocketSessionManager;
import uz.hikmatullo.httpserver.websocket.listener.WebSocketListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.RejectedExecutionException;

public class HttpServer extends Thread{

    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);
    private final ServerSocket serverSocket;
    private final RequestHandler requestHandler;
    private final WebSocketListener webSocketListener;
    private final WebSocketSessionManager webSocketSessionManager = new WebSocketSessionManager();
    public HttpServer(int port, RequestHandler requestHandler, WebSocketListener webSocketListener) throws IOException {
        serverSocket = new ServerSocket(port);
        this.requestHandler = requestHandler;
        this.webSocketListener = webSocketListener;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received. Closing all WebSocket sessions...");
            try {
                close();
            } catch (Exception e) {
                log.error("Error during shutdown: {}", e.getMessage());
            }
        }));
    }

    @Override
    public void run() {
        try {
            while(serverSocket.isBound() && !serverSocket.isClosed()) {
                log.info("Waiting for connection...");
                Socket socket = serverSocket.accept();
                log.info("Client connected!");

                var workerThread = new HttpConnectionHandler(socket, requestHandler, webSocketListener, webSocketSessionManager);
                try {
                    ExecutorsHolder.VIRTUAL_EXECUTOR.execute(workerThread);
                } catch (RejectedExecutionException rex) {
                    log.warn("Server is overloaded - rejecting connection from {}", socket.getRemoteSocketAddress());
                    try {
                        socket.close();
                    } catch (IOException ignored) {}
                }
            }
        } catch (IOException e) {
            log.error("Error occurred when setting up socket", e);
            throw new RuntimeException(e);
        } finally {
            close();
        }
    }

    public void close() {
        try {
            webSocketSessionManager.closeAll();
        } catch (Throwable t) {
            log.error("Error during sessionManager.closeAll(): {}", t.getMessage());
        }

        if (serverSocket != null) {
            try {
                serverSocket.close();
            }catch (IOException e) {
                log.error("Could not close server socket. {}", e.getMessage());
            }
        }

        ExecutorsHolder.shutdownAll();
        log.debug("HttpServer stopped");
    }


}
