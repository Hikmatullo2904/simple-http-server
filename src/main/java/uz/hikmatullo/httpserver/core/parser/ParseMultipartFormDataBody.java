package uz.hikmatullo.httpserver.core.parser;

import uz.hikmatullo.httpserver.exception.HttpParsingException;
import uz.hikmatullo.httpserver.core.model.HttpStatusCode;
import uz.hikmatullo.httpserver.core.model.MultipartRawFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ParseMultipartFormDataBody {

    private static final int MAX_HEADER_LINE_LENGTH = 8192;

    /**
     * Parses multipart/form-data body into form fields and file parts.
     *
     * @param input    InputStream of HTTP body
     * @param boundary Multipart boundary from Content-Type
     * @return Parsed result containing fields and files
     * @throws IOException on read error
     */
    public static Result parse(InputStream input, String boundary, int contentLength) throws IOException {
        if (boundary == null || boundary.isEmpty()) {
            throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);
        }

        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.US_ASCII);
        byte[] endBoundaryBytes = ("--" + boundary + "--").getBytes(StandardCharsets.US_ASCII);

        byte[] bodyBytes = input.readNBytes(contentLength);

        Result result = new Result();
        int pos = 0;

        while (true) {
            // Find start boundary
            int boundaryIndex = indexOf(bodyBytes, boundaryBytes, pos);
            if (boundaryIndex < 0) break;
            pos = boundaryIndex + boundaryBytes.length;

            // Detect final boundary
            if (startsWith(bodyBytes, endBoundaryBytes, boundaryIndex)) {
                break;
            }

            // Skip CRLF if present
            if (pos + 2 <= bodyBytes.length && bodyBytes[pos] == '\r' && bodyBytes[pos + 1] == '\n') {
                pos += 2;
            }

            // Find header section end
            int headerEnd = indexOf(bodyBytes, "\r\n\r\n".getBytes(StandardCharsets.US_ASCII), pos);
            if (headerEnd < 0) throw new HttpParsingException(HttpStatusCode.BAD_REQUEST);

            String headersBlock = new String(bodyBytes, pos, headerEnd - pos, StandardCharsets.US_ASCII);
            pos = headerEnd + 4;

            Map<String, String> partHeaders = parseHeaders(headersBlock);

            // Find next boundary
            int nextBoundaryIndex = indexOf(bodyBytes, boundaryBytes, pos);
            if (nextBoundaryIndex < 0) break;

            byte[] partData = Arrays.copyOfRange(bodyBytes, pos, nextBoundaryIndex - 2); // exclude \r\n before boundary
            pos = nextBoundaryIndex;

            // Process part
            handlePart(result, partHeaders, partData);
        }

        return result;
    }

    private static void handlePart(Result result, Map<String, String> headers, byte[] data) {
        String disposition = headers.get("content-disposition");
        if (disposition == null) return;

        Map<String, String> dispParams = parseDisposition(disposition);
        String fieldName = dispParams.get("name");
        String filename = dispParams.get("filename");

        if (filename != null && !filename.isEmpty()) {
            // File upload
            MultipartRawFile file = new MultipartRawFile(
                    fieldName,
                    filename,
                    headers.getOrDefault("content-type", "application/octet-stream"),
                    data
            );
            result.files.add(file);
        } else if (fieldName != null) {
            // Text field
            String value = new String(data, StandardCharsets.UTF_8);
            result.fields.put(fieldName, value);
        }
    }

    private static Map<String, String> parseHeaders(String headersBlock) {
        Map<String, String> headers = new LinkedHashMap<>();
        String[] lines = headersBlock.split("\r\n");
        for (String line : lines) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                String key = line.substring(0, idx).trim().toLowerCase(Locale.ROOT);
                String value = line.substring(idx + 1).trim();
                headers.put(key, value);
            }
        }
        return headers;
    }

    private static Map<String, String> parseDisposition(String disposition) {
        Map<String, String> map = new LinkedHashMap<>();
        String[] parts = disposition.split(";");
        for (String p : parts) {
            String trimmed = p.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0) {
                String name = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                map.put(name, value);
            } else {
                map.put(trimmed, "");
            }
        }
        return map;
    }

    private static boolean startsWith(byte[] source, byte[] target, int offset) {
        if (offset + target.length > source.length) return false;
        for (int i = 0; i < target.length; i++) {
            if (source[offset + i] != target[i]) return false;
        }
        return true;
    }

    private static int indexOf(byte[] source, byte[] target, int fromIndex) {
        outer:
        for (int i = fromIndex; i <= source.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    // --- Result holder ---
    public static class Result {
        public final Map<String, String> fields = new LinkedHashMap<>();
        public final List<MultipartRawFile> files = new ArrayList<>();
    }
}
