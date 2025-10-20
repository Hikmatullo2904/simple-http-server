package uz.hikmatullo.httpserver.core.model;

import uz.hikmatullo.httpserver.exception.HttpParsingException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;


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
    private final String contentType;
    private final String contentLength;
    private final List<MultipartRawFile> multipartRawFiles;
    private final Map<String, String> formFields;

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
        this.contentType = builder.contentType;
        this.contentLength = builder.contentLength;
        this.multipartRawFiles = builder.multipartRawFiles;
        this.formFields = Collections.unmodifiableMap(builder.formFields);
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
    public Map<String, String> getFormFields() { return formFields; }

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
    public String getContentType() {
        return contentType;
    }
    public String getContentLength() {
        return contentLength;
    }
    public List<MultipartRawFile> getMultipartRawFiles() {
        return multipartRawFiles;
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
        private String contentType;
        private String contentLength;
        private List<MultipartRawFile> multipartRawFiles  = new ArrayList<>();
        private Map<String, String> formFields = new HashMap<>();

        public void method(String methodName) {
            try {
                this.method = HttpMethod.valueOf(methodName);
            } catch (IllegalArgumentException e) {
                throw new HttpParsingException(HttpStatusCode.NOT_IMPLEMENTED);
            }
        }

        public void contentType(String contentType) {
            this.contentType = contentType;
        }

        public void contentLength(String contentLength) {
            this.contentLength = contentLength;
        }

        public void multipartRawFiles(List<MultipartRawFile> multipartRawFiles) {
            this.multipartRawFiles = multipartRawFiles;
        }

        public void formFields(Map<String, String> formFields) {
            this.formFields = formFields;
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


        public void addMultipartRawFile(MultipartRawFile file) {
            this.multipartRawFiles.add(file);
        }
        public void addFormFields(Map<String, String> formFields) {
            this.formFields.putAll(formFields);
        }

        public void body(String body) {
            this.body = body;
        }

        public Map<String, String> headers() {
            return headers;
        }

        public Map<String, String> cookies() {
            return cookies;
        }

        public Map<String, String> parameters() {
            return parameters;
        }

        public List<MultipartRawFile> multipartRawFiles() {
            return multipartRawFiles;
        }
        public Map<String, String> formFields() {
            return formFields;
        }

        public HttpVersion getHttpVersion() {
            return httpVersion;
        }

        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }


}
