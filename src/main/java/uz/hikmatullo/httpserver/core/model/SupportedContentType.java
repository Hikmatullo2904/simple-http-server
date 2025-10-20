package uz.hikmatullo.httpserver.core.model;

import java.util.Locale;

public enum SupportedContentType {
    JSON("application/json"),
    XML("application/xml"),
    TEXT_XML("text/xml"),
    FORM_URL_ENCODED("application/x-www-form-urlencoded"),
    TEXT_PLAIN("text/plain"),
    MULTIPART_FORM_DATA("multipart/form-data");

    private final String mimeType;

    SupportedContentType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    /**
     * Checks if the given Content-Type string is supported.
     */
    public static boolean isSupported(String contentType) {
        if (contentType == null) return false;

        // Strip charset or boundary info
        int semicolonIndex = contentType.indexOf(';');
        if (semicolonIndex != -1) {
            contentType = contentType.substring(0, semicolonIndex).trim();
        }

        final String normalized = contentType.toLowerCase(Locale.ROOT);
        for (SupportedContentType type : values()) {
            if (normalized.equals(type.mimeType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the content type is multipart/form-data.
     */
    public static boolean isMultipart(String contentType) {
        if (contentType == null) return false;
        return contentType.toLowerCase(Locale.ROOT).startsWith(MULTIPART_FORM_DATA.mimeType);
    }
}
