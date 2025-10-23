package uz.hikmatullo.httpserver.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.hikmatullo.httpserver.core.handler.RequestHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer extends Thread{

    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);
    private final ServerSocket serverSocket;
    private final RequestHandler requestHandler;
    public HttpServer(int port,  RequestHandler requestHandler) throws IOException {
        serverSocket = new ServerSocket(port);
        this.requestHandler = requestHandler;
    }

    @Override
    public void run() {
        try {
            while(serverSocket.isBound() && !serverSocket.isClosed()) {
                log.info("Waiting for connection...");
                Socket socket = serverSocket.accept();
                log.info("Client connected!");

                var workerThread = new HttpConnectionHandler(socket, requestHandler);
                new Thread(workerThread).start();
            }
        } catch (IOException e) {
            log.error("Error occurred when setting up socket", e);
            throw new RuntimeException(e);
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                }catch (IOException e) {
                    log.error("Could not close server socket. {}", e.getMessage());
                }
            }
        }
    }

}
