package uz.hikmatullo.core.model;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse {

    private String protocol;
    private int statusCode;
    private String reasonPhrase;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private byte[] body = new byte[0];

    public HttpResponse(HttpStatusCode status) {
        this.statusCode = status.statusCode;
        this.reasonPhrase = status.reasonPhrase;
    }

    public void setStatus(HttpStatusCode status) {
        this.statusCode = status.statusCode;
        this.reasonPhrase = status.name();
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public void setBody(byte[] body) {
        this.body = (body != null) ? body : new byte[0];
        headers.put("Content-Length", String.valueOf(this.body.length));
    }

    public void write(OutputStream outputStream) throws IOException {
        final String CRLF = "\r\n";
        StringBuilder responseBuilder = new StringBuilder();

        // 1️⃣ Status line
        responseBuilder.append(protocol).append(" ")
                       .append(statusCode).append(" ")
                       .append(reasonPhrase).append(CRLF);

        // 2️⃣ Headers
        for (var entry : headers.entrySet()) {
            responseBuilder.append(entry.getKey())
                           .append(": ")
                           .append(entry.getValue())
                           .append(CRLF);
        }

        // 3️⃣ End of headers
        responseBuilder.append(CRLF);

        // 4️⃣ Write headers + body
        outputStream.write(responseBuilder.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.write(body);
        outputStream.flush();
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
