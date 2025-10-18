package uz.hikmatullo.core.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.hikmatullo.core.exception.HttpParsingException;
import uz.hikmatullo.core.model.HttpRequest;
import uz.hikmatullo.core.model.HttpStatusCode;
import uz.hikmatullo.core.model.HttpVersion;
import uz.hikmatullo.core.model.SupportedContentType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Strict HTTP/1.x request parser.
 *
 * Key decisions:
 * - Uses raw InputStream so we can enforce strict CRLF sequences.
 * - Uses ISO-8859-1 (latin1) for request-line and header bytes per RFC7230.
 * - Body decoding (to String) uses UTF-8 by default; you may inspect Content-Type charset to change this.
 *
 * Note: this parser is intentionally strict — it rejects many malformed cases that lenient servers might accept.
 */
public class HttpParser {

    private static final Logger log = LoggerFactory.getLogger(HttpParser.class);

    // ASCII control bytes
    private static final int CR = 0x0D; // '\r'
    private static final int LF = 0x0A; // '\n'

    // Limits (tunable)
    private static final int MAX_REQUEST_LINE_LENGTH = 8192; // bytes
    private static final int MAX_HEADER_LINE_LENGTH = 8192; // bytes
    private static final int MAX_HEADERS = 200; // header count
    private static final int MAX_HEADER_TOTAL_SIZE = 65536; // bytes combined

    /**
     * Parse an HTTP request from the input stream.
     *
     * @param input Input stream from socket (must be blocking and contiguous for a single request)
     * @return fully built HttpRequest
     */
    public HttpRequest parse(InputStream input) {
        Objects.requireNonNull(input, "input stream required");

        HttpRequest.Builder builder = new HttpRequest.Builder();

        try {
            // Request line
            String requestLine = readLineStrict(input, MAX_REQUEST_LINE_LENGTH);
            parseRequestLine(requestLine, builder);

            // Headers
            Map<String, String> headers = parseHeaders(input);
            builder.headers(headers);

            // Cookie parsing
            Map<String, String> cookies = parseCookiesFromHeaders(headers);
            if (!cookies.isEmpty()) builder.cookies(cookies);

            // Validate Host for HTTP/1.1
            HttpVersion version = builder.getHttpVersion();
            if (version == null) {
                // fallback: attempt to compute from header 'Host' or previously set - but ideally builder.httpVersion() already set by request line.
                throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
            }
            if (version.isHttp11() && !headers.containsKey("host")) {
                throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
            }

            // Body
            parseBody(input, builder, headers);

            return builder.build();

        } catch (HttpParsingException e) {

            throw e;
        } catch (IOException e) {
            log.error("I/O while parsing HTTP request", e);
            throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
        }
    }

    // ---------------------------
// Request-line parsing (strict per RFC 9112)
// ---------------------------
    private void parseRequestLine(String requestLine, HttpRequest.Builder builder) {
        if (requestLine == null || requestLine.isEmpty()) {
            throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
        }

        // Split into 3 exact parts: METHOD SP REQUEST_TARGET SP HTTP_VERSION CRLF
        String[] parts = requestLine.split("\\s+");
        if (parts.length != 3) {
            throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
        }

        String methodStr = parts[0];
        String target = parts[1];
        String versionLiteral = parts[2];

        // --- Method ---
        try {
            builder.method(methodStr);
        } catch (HttpParsingException ex) {
            throw new HttpParsingException(HttpStatusCode.NOT_IMPLEMENTED_METHOD);
        }

        // --- Target ---
        if (target == null || target.isEmpty()) {
            throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
        }

        // Handle different request target forms
        if (target.startsWith("http://") || target.startsWith("https://")) {
            // Absolute-form (proxies)
            try {
                URI uri = URI.create(target);
                builder.path(uri.getPath() != null ? uri.getPath() : "/");
                builder.rawQuery(uri.getQuery());
                builder.target(target);
            } catch (IllegalArgumentException e) {
                throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
            }
        } else if (target.equals("*")) {
            // Asterisk-form (e.g., OPTIONS * HTTP/1.1)
            builder.path("*");
            builder.rawQuery(null);
            builder.target("*");
        } else {
            // Origin-form (normal)
            int qIndex = target.indexOf('?');
            if (qIndex >= 0) {
                builder.path(target.substring(0, qIndex));
                builder.rawQuery(target.substring(qIndex + 1));
            } else {
                builder.path(target);
                builder.rawQuery(null);
            }
            builder.target(target);
        }

        // --- HTTP Version ---
        HttpVersion best = HttpVersion.getBestCompatibleVersion(versionLiteral);
        if (best == null) {
            throw new HttpParsingException(HttpStatusCode.HTTP_VERSION_NOT_SUPPORTED);
        }
        builder.httpVersion(versionLiteral);
    }


    // ---------------------------
    // Headers parsing
    // ---------------------------
    private Map<String, String> parseHeaders(InputStream input) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        int headerCount = 0;
        int totalSize = 0;

        while (true) {
            String line = readLineStrict(input, MAX_HEADER_LINE_LENGTH);
            // line == "" => end of headers
            if (line.isEmpty()) break;

            headerCount++;
            totalSize += line.length();
            if (headerCount > MAX_HEADERS || totalSize > MAX_HEADER_TOTAL_SIZE) {
                throw new HttpParsingException(HttpStatusCode.REQUEST_HEADER_FIELDS_TOO_LARGE);
            }

            // per RFC7230 obs-fold is now invalid; reject lines starting with SP or HTAB
            char firstChar = line.charAt(0);
            if (firstChar == ' ' || firstChar == '\t') {
                throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
            }

            int colonIndex = line.indexOf(':');
            if (colonIndex <= 0) {
                throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
            }

            String name = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();

            // Normalize header name case-insensitively. We'll store canonical form as lower-case.
            String nameLower = name.toLowerCase(Locale.ROOT);

            // Combine repeated headers per RFC (concatenate with ", ").
            if (headers.containsKey(nameLower)) {
                String prev = headers.get(nameLower);
                headers.put(nameLower, prev + ", " + value);
            } else {
                headers.put(nameLower, value);
            }
        }

        return headers;
    }

    // ---------------------------
    // Body parsing (Content-Length and chunked)
    // ---------------------------
    private void parseBody(InputStream input, HttpRequest.Builder builder, Map<String, String> headers) throws IOException {
        builder.contentLength(getHeaderIgnoreCase(headers, "content-length"));
        builder.contentType(getHeaderIgnoreCase(headers, "content-type"));

        String transferEncoding = getHeaderIgnoreCase(headers, "transfer-encoding");
        String contentType = getHeaderIgnoreCase(headers, "content-type");
        String contentLengthValue = getHeaderIgnoreCase(headers, "content-length");

        // 1️⃣ Multipart (form-data)
        if (SupportedContentType.isMultipart(contentType)) {
            String boundary = extractBoundary(contentType);
            ParseMultipartFormDataBody.Result multi = ParseMultipartFormDataBody.parse(input, boundary);
            builder.multipartRawFiles(multi.files);
            builder.formFields(multi.fields);
            return;
        }

        // 2️⃣ Chunked transfer
        if (transferEncoding != null) {
            String lower = transferEncoding.toLowerCase(Locale.ROOT);
            if (!lower.contains("chunked")) {
                throw new HttpParsingException(HttpStatusCode.NOT_IMPLEMENTED_METHOD);
            }

            byte[] bodyBytes = readChunkedBody(input, builder, headers);
            String bodyString = bytesToStringWithCharset(bodyBytes, headers);
            builder.body(bodyString);
            return;
        }

        // 3️⃣ Fixed-length body
        if (contentLengthValue != null) {
            int length;
            try {
                length = Integer.parseInt(contentLengthValue.trim());
            } catch (NumberFormatException e) {
                throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
            }

            if (length < 0) throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);

            // Only allow known types
            if (!SupportedContentType.isSupported(contentType)) {
                log.error("Unsupported media type: {}", contentType);
                throw new HttpParsingException(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE);
            }

            byte[] bodyBytes = readFixedLength(input, length);
            String bodyString = bytesToStringWithCharset(bodyBytes, headers);
            builder.body(bodyString);
            return;
        }

        // 4️⃣ No body
        builder.body(null);
    }


    private byte[] readFixedLength(InputStream input, int length) throws IOException {
        byte[] buffer = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = input.read(buffer, offset, length - offset);
            if (read == -1) {
                log.error("Body size is not equal to content-length");
                throw new HttpParsingException(HttpStatusCode.BAD_REQUEST, "Body size is not equal to content-length");
            }
            offset += read;
        }
        return buffer;
    }

    /**
     * Read and decode chunked transfer-encoding body.
     * Implements RFC7230 chunked transfer decoding:
     * <chunk-size> [; extensions] CRLF
     * <chunk-data> CRLF
     * ...
     * 0 CRLF
     * [trailer headers] CRLF
     */
    private byte[] readChunkedBody(InputStream input, HttpRequest.Builder builder, Map<String, String> requestHeaders) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (true) {
            // Read chunk-size line strictly
            String sizeLine = readLineStrict(input, MAX_HEADER_LINE_LENGTH);
            // strip chunk extensions (after ';')
            int semi = sizeLine.indexOf(';');
            String sizeToken = semi >= 0 ? sizeLine.substring(0, semi).trim() : sizeLine.trim();
            if (sizeToken.isEmpty()) throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);

            int chunkSize;
            try {
                chunkSize = Integer.parseInt(sizeToken, 16);
            } catch (NumberFormatException ex) {
                throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
            }

            if (chunkSize < 0) throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);

            if (chunkSize == 0) {
                // final chunk; consume trailer headers (if any) until CRLF CRLF
                Map<String, String> trailers = new LinkedHashMap<>();
                while (true) {
                    String trailerLine = readLineStrict(input, MAX_HEADER_LINE_LENGTH);
                    if (trailerLine.isEmpty()) break;
                    int colon = trailerLine.indexOf(':');
                    if (colon <= 0) throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
                    String name = trailerLine.substring(0, colon).trim();
                    String value = trailerLine.substring(colon + 1).trim();
                    String key = name.toLowerCase(Locale.ROOT);
                    if (trailers.containsKey(key)) {
                        trailers.put(key, trailers.get(key) + ", " + value);
                    } else {
                        trailers.put(key, value);
                    }
                }
                // We ignore trailers for now, but they exist if needed.
                return out.toByteArray();
            }

            // Read chunk data exactly chunkSize bytes
            byte[] chunk = readFixedLength(input, chunkSize);
            out.write(chunk);

            // After chunk data must be CRLF
            int c1 = input.read();
            int c2 = input.read();
            if (c1 != CR || c2 != LF) {
                throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
            }
        }
    }

    // ---------------------------
    // Utility - strict line reader (CRLF only)
    // ---------------------------
    private String readLineStrict(InputStream input, int maxLen) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(128);

        int total = 0;

        while (true) {
            int b = input.read();
            if (b == -1) {
                // EOF while reading a line -> protocol error
                throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
            }
            total++;
            if (total > maxLen) {
                throw new HttpParsingException(HttpStatusCode.REQUEST_HEADER_FIELDS_TOO_LARGE);
            }

            if (b == CR) {
                int next = input.read();
                if (next == -1) throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
                if (next != LF) {
                    // CR not followed by LF: invalid per strict HTTP parsing
                    throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
                }
                // line ended. convert bytes to String using ISO-8859-1 as RFC suggests for header bytes
                return buf.toString(StandardCharsets.ISO_8859_1);
            }

            if (b == LF) {
                // LF without preceding CR -> invalid
                throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
            }

            buf.write(b);
        }
    }

    // ---------------------------
    // Cookies parsing
    // ---------------------------
    private Map<String, String> parseCookiesFromHeaders(Map<String, String> headers) {
        Map<String, String> cookies = new LinkedHashMap<>();
        // header names stored lower-case
        // Cookies may appear in "cookie" header; multiple cookie headers may be present combined with ', '
        String cookieHeader = headers.get("cookie");
        if (cookieHeader == null) return cookies;

        // Cookie header format: "name=value; name2=value2; ..."
        String[] pairs = cookieHeader.split(";");
        for (String pair : pairs) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) continue;
            String[] kv = trimmed.split("=", 2);
            if (kv.length == 2) {
                cookies.put(kv[0].trim(), kv[1].trim());
            }
        }
        return cookies;
    }

    // ---------------------------
    // Form parameters parsing
    // ---------------------------
    private void parseFormParametersIfNeeded(HttpRequest.Builder builder, Map<String, String> headers, String body) {
        if (body == null || body.isEmpty()) return;

        String contentType = getHeaderIgnoreCase(headers, "content-type");
        if (contentType != null) {
            String lower = contentType.toLowerCase(Locale.ROOT);
            if (lower.contains("application/x-www-form-urlencoded")) {
                Map<String, String> params = parseUrlEncoded(body);
                if (!params.isEmpty()) builder.parameters(params);
            }
        }
    }

    private Map<String, String> parseUrlEncoded(String body) {
        Map<String, String> params = new LinkedHashMap<>();
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            String[] kv = pair.split("=", 2);
            try {
                String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String v = kv.length == 2 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                params.put(k, v);
            } catch (Exception ex) {
                // leave raw if decoding fails
                params.put(kv[0], kv.length == 2 ? kv[1] : "");
            }
        }
        return params;
    }

    // ---------------------------
    // Helpers
    // ---------------------------
    private static String getHeaderIgnoreCase(Map<String, String> headers, String key) {
        if (key == null) return null;
        return headers.get(key.toLowerCase(Locale.ROOT));
    }

    private static String bytesToStringWithCharset(byte[] bytes, Map<String, String> headers) {
        // If Content-Type has charset parameter, use it; otherwise fall back to UTF-8
        String contentType = getHeaderIgnoreCase(headers, "content-type");
        if (contentType != null) {
            // naive parse of charset
            String[] parts = contentType.split(";");
            for (String p : parts) {
                String trimmed = p.trim();
                if (trimmed.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                    String cs = trimmed.substring(8).trim();
                    try {
                        return new String(bytes, cs);
                    } catch (Exception ignored) {
                        // fallthrough to utf-8
                    }
                }
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }


    private String extractBoundary(String contentType) {
        for (String param : contentType.split(";")) {
            param = param.trim();
            if (param.startsWith("boundary=")) {
                String val = param.substring("boundary=".length());
                // boundary might be quoted
                if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                    val = val.substring(1, val.length() - 1);
                }
                return val;
            }
        }
        return null;
    }
}
