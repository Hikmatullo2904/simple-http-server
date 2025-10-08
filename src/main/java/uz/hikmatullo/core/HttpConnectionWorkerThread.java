package uz.hikmatullo.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class HttpConnectionWorkerThread extends Thread{

    private static final Logger log = LoggerFactory.getLogger(HttpConnectionWorkerThread.class);
    private Socket socket;
    public HttpConnectionWorkerThread(Socket socket) {
        this.socket = socket;
    }
    @Override
    public void run() {

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            final String CRLF = "\r\n";

            String html = getHtml();

            String response =
                    "HTTP/1.1 200 OK" + CRLF + //Status line
//                    "Content-Type: text/html" + CRLF + //Headers
                            "Content-Length: " + html.getBytes().length + CRLF +
                            CRLF +
                            html; //Body
            /*
            This doesn’t directly send data to the client (like the browser).
            Instead, it writes the bytes into an internal buffer — a memory area managed by the Java I/O stream.
            Java → copies data into a buffer.
            The buffer → goes down into the Socket Output Stream.
            Eventually → the OS kernel picks up that data and sends it out via TCP (when the buffer fills or is flushed). */
            outputStream.write(response.getBytes());
            //Hey OS, send everything in that buffer to the TCP layer right now, don’t wait
            outputStream.flush();

            inputStream.close();

            //This calls flush internally. that's why without calling flush explicitly, response is returning.
            outputStream.close();
            socket.close();

            log.info("Connection finished");
        }catch (IOException e) {
            log.error("Error happened {}", e.getMessage());
        }finally {
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

}
