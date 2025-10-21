package uz.hikmatullo.httpserver.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.hikmatullo.httpserver.core.model.HttpResponse;
import uz.hikmatullo.httpserver.exception.HttpParsingException;
import uz.hikmatullo.httpserver.core.model.HttpRequest;
import uz.hikmatullo.httpserver.core.model.HttpStatusCode;
import uz.hikmatullo.httpserver.core.parser.HttpParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class HttpConnectionHandler extends Thread{

    private static final Logger log = LoggerFactory.getLogger(HttpConnectionHandler.class);
    private final Socket socket;
    public HttpConnectionHandler(Socket socket) {
        this.socket = socket;
    }
    @Override
    public void run() {

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            if (inputStream.available() == 0) {
                log.warn("Empty connection received. Ignoring.");
                return;
            }

//            System.out.println(readRequest(inputStream));

            HttpParser parser = new HttpParser();
            HttpRequest parse = parser.parse(inputStream);

            String html = getHtml();
            HttpResponse response = new HttpResponse.Builder()
                    .status(HttpStatusCode.OK)
                    .protocol("HTTP/1.1")
                    .header("Content-Type", "text/html")
                    .body(html.getBytes())
                    .build();

            response.write(outputStream);


            /*
            This doesn’t directly send data to the client (like the browser).
            Instead, it writes the bytes into an internal buffer — a memory area managed by the Java I/O stream.
            Java → copies data into a buffer.
            The buffer → goes down into the Socket Output Stream.
            Eventually → the OS kernel picks up that data and sends it out via TCP (when the buffer fills or is flushed). */
//            outputStream.write(response.getBytes());
            //Hey OS, send everything in that buffer to the TCP layer right now, don’t wait
//            outputStream.flush();

            log.info("Connection finished");
        }catch (IOException e) {
            log.error("I/O error happened {}", e.getMessage());
            sendErrorResponse(socket, HttpStatusCode.INTERNAL_SERVER_ERROR.statusCode, e.getMessage());
        } catch (HttpParsingException e) {
            sendErrorResponse(socket, e.getErrorCode().statusCode, e.getMessage());
        }catch (Exception e) {
            log.error("Error happened {}", e.getMessage());
            sendErrorResponse(socket, HttpStatusCode.INTERNAL_SERVER_ERROR.statusCode, e.getMessage());
        }
        finally {
            closeConnections(inputStream, outputStream);
        }
    }

    private void closeConnections(InputStream inputStream, OutputStream outputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error("Could not close inputStream. {}", e.getMessage());
            }
        }

        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                log.error("Could not close outputStream. {}", e.getMessage());
            }
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                log.error("Could not close socket. {}", e.getMessage());
            }
        }
    }

    private static String getHtml() {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>My Server</title>
        </head>
        <body>
            <h1>This is my super simple http server created using Java</h1>
        </body>
        </html>
        """;
    }

    private String readRequest(InputStream inputStream) throws IOException {
        StringBuilder headersBuilder = new StringBuilder();
        int prev = 0, curr;
        // Read until \r\n\r\n (end of headers)
        while ((curr = inputStream.read()) != -1) {
            headersBuilder.append((char) curr);
            if (prev == '\r' && curr == '\n' && headersBuilder.toString().endsWith("\r\n\r\n")) {
                break;
            }
            prev = curr;
        }

        String headers = headersBuilder.toString();
        System.out.println("----- HEADERS START -----");
        System.out.println(headers);
        System.out.println("----- HEADERS END -----");

        // Extract content length
        int contentLength = 0;
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
        }

        // Now read body based on content length
        byte[] body = new byte[contentLength];
        int bytesRead = 0;
        while (bytesRead < contentLength) {
            int read = inputStream.read(body, bytesRead, contentLength - bytesRead);
            if (read == -1) break;
            bytesRead += read;
        }

        System.out.println("----- BODY START -----");
        System.out.println(new String(body));
        System.out.println("----- BODY END -----");

        return headers + "\r\n" + new String(body);
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
