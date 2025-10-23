package uz.hikmatullo.httpserver.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer extends Thread{

    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);
    private int port;
    private String webroot;
    private final ServerSocket serverSocket;
    public HttpServer(int port, String webroot) throws IOException {
        this.port = port;
        this.webroot = webroot;
        serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        try {
            while(serverSocket.isBound() && !serverSocket.isClosed()) {
                log.info("Waiting for connection...");
                Socket socket = serverSocket.accept();
                log.info("Client connected!");

                var workerThread = new HttpConnectionHandler(socket);
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
