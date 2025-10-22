package uz.hikmatullo.httpserver.util;

import java.io.IOException;
import java.io.InputStream;

public class Util {

    public static String readRequest(InputStream inputStream) throws IOException {
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
}
