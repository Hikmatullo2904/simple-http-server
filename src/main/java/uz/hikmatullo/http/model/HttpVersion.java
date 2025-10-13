package uz.hikmatullo.http.model;

import uz.hikmatullo.http.exception.BadHttpVersionException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents supported HTTP protocol versions.
 * Strictly validates "HTTP/x.y" format and provides compatibility helpers.
 */
public enum HttpVersion {
    HTTP_1_0("HTTP/1.0", 1, 0),
    HTTP_1_1("HTTP/1.1", 1, 1),
    HTTP_2_0("HTTP/2.0", 2, 0); // reserved for future use

    public final String literal;
    public final int major;
    public final int minor;

    HttpVersion(String literal, int major, int minor) {
        this.literal = literal;
        this.major = major;
        this.minor = minor;
    }

    private static final Pattern HTTP_VERSION_PATTERN =
            Pattern.compile("^HTTP/(?<major>\\d+)\\.(?<minor>\\d+)$");

    /**
     * Returns the exact matching version or throws if invalid.
     */
    public static HttpVersion fromLiteral(String literal) throws BadHttpVersionException {
        if (literal == null) throw new BadHttpVersionException();

        for (HttpVersion version : values()) {
            if (version.literal.equalsIgnoreCase(literal)) {
                return version;
            }
        }
        throw new BadHttpVersionException();
    }

    /**
     * Returns the most compatible version supported by this server.
     * For example: "HTTP/1.2" will return HTTP/1.1 as the closest match.
     * Returns null if the version is totally unsupported.
     */
    public static HttpVersion getBestCompatibleVersion(String literal) throws BadHttpVersionException {
        if (literal == null) throw new BadHttpVersionException();

        Matcher matcher = HTTP_VERSION_PATTERN.matcher(literal.trim());
        if (!matcher.matches()) {
            throw new BadHttpVersionException();
        }

        int major = Integer.parseInt(matcher.group("major"));
        int minor = Integer.parseInt(matcher.group("minor"));

        HttpVersion best = null;
        for (HttpVersion version : values()) {
            if (version.major == major) {
                // exact match
                if (version.minor == minor) return version;
                // keep the closest lower compatible version
                if (version.minor < minor) best = version;
            }
        }
        return best;
    }

    public boolean isHttp11() {
        return this == HTTP_1_1;
    }

    @Override
    public String toString() {
        return literal;
    }
}
