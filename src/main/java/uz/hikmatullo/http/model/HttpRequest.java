package uz.hikmatullo.http.model;

import uz.hikmatullo.http.exception.HttpParsingException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;


public class HttpRequest {

    private final HttpMethod method;
    private final String path;
    private final String originalHttpVersion; // literal from the request
    private final HttpVersion httpVersion;
    private final Map<String, String> headers;
    private final String body;
    private final Map<String, String> cookies;
    private final Map<String, String> parameters;
    private final String rawQuery;
    private final String target;

    private HttpRequest(Builder builder) {
        this.method = builder.method;
        this.path = builder.path;
        this.httpVersion = builder.httpVersion;
        this.originalHttpVersion = builder.originalHttpVersion;
        this.headers = Collections.unmodifiableMap(builder.headers);
        this.body = builder.body;
        this.cookies = Collections.unmodifiableMap(builder.cookies);
        this.parameters = Collections.unmodifiableMap(builder.parameters);
        this.rawQuery = builder.rawQuery;
        this.target = builder.target;
    }

    // ---- Getters ----
    public HttpMethod getMethod() { return method; }
    public String getPath() { return path; }
    public HttpVersion getHttpVersion() { return httpVersion; }
    public String getOriginalHttpVersion() { return originalHttpVersion; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }
    public Map<String, String> getCookies() { return cookies; }
    public Map<String, String> getParameters() { return parameters; }

    // Convenience helper (case-insensitive header lookup)
    public String getHeader(String name) {
        return headers.get(name.toLowerCase(Locale.ROOT));
    }
    public String getCookie(String name) {
        return cookies.get(name.toLowerCase(Locale.ROOT));
    }
    public String getParameter(String name) {
        return parameters.get(name);
    }

    public String getRawQuery() {
        return rawQuery;
    }

    public String getTarget() {
        return target;
    }

    // ---- Builder Pattern ----
    public static class Builder {
        private HttpMethod method;
        private String path;
        private HttpVersion httpVersion;
        private String originalHttpVersion;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private String body;
        private final Map<String, String> cookies = new LinkedHashMap<>();
        private final Map<String, String> parameters = new LinkedHashMap<>();
        private String rawQuery;
        private String target;

        public void method(String methodName) {
            try {
                this.method = HttpMethod.valueOf(methodName);
            } catch (IllegalArgumentException e) {
                throw new HttpParsingException(HttpStatusCode.NOT_IMPLEMENTED_METHOD);
            }
        }



        public Builder path(String path) {
            if (path == null || path.isEmpty()) {
                throw new HttpParsingException(HttpStatusCode.INTERNAL_SERVER_ERROR);
            }
            this.path = path;
            return this;
        }

        public void httpVersion(String versionString) {
            HttpVersion version = HttpVersion.getBestCompatibleVersion(versionString);
            if (version == null) {
                throw new HttpParsingException(HttpStatusCode.HTTP_VERSION_NOT_SUPPORTED);
            }
            this.httpVersion = version;
            this.originalHttpVersion = versionString;
        }

        public void rawQuery(String query) {
            this.rawQuery = query;
            if (query != null && !query.isEmpty()) {
                setParameters(query);
            }
        }

        private void setParameters(String query) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf('=');
                if (idx >= 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    this.parameters.put(key, value);
                } else {
                    // key with no value
                    String key = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                    this.parameters.put(key, "");
                }
            }
        }

        public void target(String target) {
            this.target = target;
        }

        public void headers(Map<String, String> headers) {
            this.headers.putAll(headers);
        }

        public void cookies(Map<String, String> cookies) {
            this.cookies.putAll(cookies);
        }

        public void parameters(Map<String, String> parameters) {
            this.parameters.putAll(parameters);
        }

        public void addHeader(String key, String value) {
            headers.put(key.toLowerCase(Locale.ROOT), value);
        }

        public void addCookie(String key, String value) {
            cookies.put(key, value);
        }

        public void addParameter(String key, String value) {
            parameters.put(key, value);
        }

        public void body(String body) {
            this.body = body;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public Map<String, String> getCookies() {
            return cookies;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public HttpVersion getHttpVersion() {
            return httpVersion;
        }

        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }


}
