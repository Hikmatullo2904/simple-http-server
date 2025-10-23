package uz.hikmatullo.httpserver.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.hikmatullo.httpserver.controller.TestController;
import uz.hikmatullo.httpserver.core.HttpKeepAliveManager;
import uz.hikmatullo.httpserver.core.model.HttpResponse;
import uz.hikmatullo.httpserver.core.model.HttpRequest;
import uz.hikmatullo.httpserver.core.model.HttpStatusCode;
import uz.hikmatullo.httpserver.core.parser.HttpParser;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class HttpConnectionHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(HttpConnectionHandler.class);
    private final Socket socket;
    public HttpConnectionHandler(Socket socket) {
        this.socket = socket;
    }
    @Override
    public void run() {
        try {
            // Set a timeout so we don't hang forever waiting for data
            socket.setSoTimeout(5000);

            try (InputStream inputStream = socket.getInputStream();
                 OutputStream outputStream = socket.getOutputStream()) {


                HttpKeepAliveManager keepAliveManager = new HttpKeepAliveManager();
                boolean keepAlive;

                do {
                    HttpParser parser = new HttpParser();
                    HttpRequest request = parser.parse(inputStream);
                    if (request == null) {
                        log.debug("Request is null");
                        break;
                    }

                    // --- Handle request ---
                    TestController controller = new TestController();
                    HttpResponse response = controller.sendPage(request);

                    // --- Handle Keep-Alive ---
                    keepAlive = keepAliveManager.shouldKeepAlive(request);

                    if (keepAlive) {
                        response.addHeader("Connection", "keep-alive");
                        response.addHeader("Keep-Alive", "timeout=5, max=100");
                    } else {
                        response.addHeader("Connection", "close");
                    }

                    // --- Send Response ---
                    response.write(outputStream);

                    log.info("Request processed. keepAlive={}", keepAlive);

                } while (keepAlive && !socket.isClosed());

            }

        } catch (SocketTimeoutException e) {
            log.debug("Keep-alive timeout reached, closing connection.");
        }catch (IOException e) {
            log.error("I/O error happened: {}", e.getMessage());
            sendErrorResponse(socket, HttpStatusCode.INTERNAL_SERVER_ERROR.statusCode, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            sendErrorResponse(socket, HttpStatusCode.INTERNAL_SERVER_ERROR.statusCode, e.getMessage());
        } finally {
            closeConnection();
        }
    }



    private void closeConnection() {


        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                log.error("Could not close socket. {}", e.getMessage());
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




}
