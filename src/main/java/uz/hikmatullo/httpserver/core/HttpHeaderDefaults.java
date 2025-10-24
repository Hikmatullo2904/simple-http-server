package uz.hikmatullo.httpserver.core;

import uz.hikmatullo.httpserver.core.model.HttpResponse;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class HttpHeaderDefaults {
    public static void applyDefaultResponseHeaders(HttpResponse response, boolean keepAlive) {
        if (response.getHeader("Date") != null) {
            response.addHeader("Date", DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
        }
        if (response.getHeader("Server") != null) {
            response.addHeader("Server", "HA_HTTP/1.1");
        }
        if (keepAlive) {
            response.addHeader("Connection", "keep-alive");
            response.addHeader("Keep-Alive", "timeout=5, max=100");
        } else {
            response.addHeader("Connection", "close");
        }
    }
}
