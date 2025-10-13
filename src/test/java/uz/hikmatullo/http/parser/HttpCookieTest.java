package uz.hikmatullo.http.parser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uz.hikmatullo.http.exception.HttpParsingException;
import uz.hikmatullo.http.model.HttpRequest;
import uz.hikmatullo.http.model.HttpStatusCode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpCookieTest {

    private HttpParser httpParser;

    @BeforeAll
    public void beforeClass() {
        httpParser = new HttpParser();
    }

    @Test
    public void testValidCookie() {
        HttpRequest request = httpParser.parse(generateValidTestCase());
        assertEquals("safe-token", request.getCookie("token"));
    }

    @Test
    public void testCookiesSize() {
        HttpRequest request = httpParser.parse(generateValidTestCase());
        assertEquals(4, request.getCookies().size());
    }

    @Test
    public void testInvalidCookie_NameIsMissing() {
        try {
            httpParser.parse(generateInValidTestCase_NameIsMissing());
            fail();
        } catch (HttpParsingException e) {
            assertEquals(HttpStatusCode.BAD_REQUEST, e.getErrorCode());
        }
    }

    @Test
    public void testInvalidCookie_EqualSignIsMissing() {
        try {
            httpParser.parse(generateInValidTestCase_EqualSignIsMissing());
            fail();
        } catch (HttpParsingException e) {
            assertEquals(HttpStatusCode.BAD_REQUEST, e.getErrorCode());
        }
    }




    private InputStream generateValidTestCase() {
        String rawData = """
                GET / HTTP/1.1\r
                Origin: metrix.uz\r
                X-Code: something\r
                tenant: metrix\r
                User-Agent: PostmanRuntime/7.48.0\r
                Accept: */*\r
                Postman-Token: f75cb8ed-307e-4aac-b2c1-274b89d08b69\r
                Host: localhost:8080\r
                Accept-Encoding: gzip, deflate, br\r
                Connection: keep-alive\r
                Cookie: remember_web=remember_web; jenkins-timestamper-offset=-18000000; csrftoken=kYuOjwK4ByOFjA6cWWJUjqaepPS0ffTw; token=safe-token\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }

    private InputStream generateInValidTestCase_NameIsMissing() {
        String rawData = """
                GET / HTTP/1.1\r
                Origin: metrix.uz\r
                X-Code: something\r
                tenant: metrix\r
                User-Agent: PostmanRuntime/7.48.0\r
                Accept: */*\r
                Postman-Token: f75cb8ed-307e-4aac-b2c1-274b89d08b69\r
                Host: localhost:8080\r
                =abcd\r
                Accept-Encoding: gzip, deflate, br\r
                Connection: keep-alive\r
                Cookie: remember_web=remember_web; jenkins-timestamper-offset=-18000000; csrftoken=kYuOjwK4ByOFjA6cWWJUjqaepPS0ffTw; token=safe-token\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }

    private InputStream generateInValidTestCase_EqualSignIsMissing() {
        String rawData = """
                GET / HTTP/1.1\r
                Origin: metrix.uz\r
                X-Code: something\r
                tenant: metrix\r
                User-Agent: PostmanRuntime/7.48.0\r
                Accept: */*\r
                Postman-Token: f75cb8ed-307e-4aac-b2c1-274b89d08b69\r
                Host: localhost:8080\r
                session\r
                Accept-Encoding: gzip, deflate, br\r
                Connection: keep-alive\r
                Cookie: remember_web=remember_web; jenkins-timestamper-offset=-18000000; csrftoken=kYuOjwK4ByOFjA6cWWJUjqaepPS0ffTw; token=safe-token\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }


}
