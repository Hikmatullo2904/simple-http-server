package uz.hikmatullo.httpserver.core.model;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse {

    private String protocol;
    private int statusCode;
    private String reasonPhrase;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private byte[] body = new byte[0];

    public String getProtocol() {
        return protocol;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

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

        if (protocol == null) {
            protocol = "HTTP/1.1";
        }

        // 1 Status line
        responseBuilder.append(protocol).append(" ")
                       .append(statusCode).append(" ")
                       .append(reasonPhrase).append(CRLF);

        // 2 Headers
        for (var entry : headers.entrySet()) {
            responseBuilder.append(entry.getKey())
                           .append(": ")
                           .append(entry.getValue())
                           .append(CRLF);
        }

        // 3 End of headers
        responseBuilder.append(CRLF);

        // 4 Write headers + body
        outputStream.write(responseBuilder.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.write(body);
        outputStream.flush();
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public static class Builder {
        private HttpStatusCode status;
        private String protocol;
        private final Map<String, String> headers = new HashMap<>();
        private byte[] body = new byte[0];

        public Builder status(HttpStatusCode status) {
            this.status = status;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder body(byte[] body) {
            this.body = body;
            return this;
        }

        public HttpResponse build() {
            HttpResponse response = new HttpResponse(status);
            if (protocol == null) {
                protocol = "HTTP/1.1";
            }
            response.protocol = protocol;
            response.headers.putAll(headers);
            response.body = body;
            return response;
        }
    }

}
