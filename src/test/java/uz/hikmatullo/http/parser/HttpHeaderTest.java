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
public class HttpHeaderTest {

    private HttpParser httpParser;

    @BeforeAll
    public void beforeClass() {
        httpParser = new HttpParser();
    }

    @Test
    public void testEmptyValue() {
        HttpRequest request = httpParser.parse(generateValidTestCase());
        assertEquals("", request.getHeader("EmptyKey"));
    }

    @Test
    public void testValidHeaderWithDifferentCases() {
        HttpRequest request = httpParser.parse(generateValidTestCase());
        assertNotNull(request.getHeader("origin"));
        assertEquals("metrix.uz", request.getHeader("origin"));
        assertEquals("something", request.getHeader("X-CODE"));
        assertEquals("metrix", request.getHeader("Tenant"));
    }

    @Test
    public void testValidHeader() {
        HttpRequest request = httpParser.parse(generateValidTestCase());
        assertNotNull(request.getHeader("Origin"));
        assertEquals("metrix.uz", request.getHeader("Origin"));
        assertEquals("something", request.getHeader("X-Code"));
        assertEquals("metrix", request.getHeader("tenant"));
    }

    @Test
    public void testNotExistHeader() {
        HttpRequest request = httpParser.parse(generateValidTestCase());
        assertNull(request.getHeader("origi"));
    }

    @Test
    public void testHostHeader() {
        HttpRequest request = httpParser.parse(generateValidTestCase());
        assertEquals("localhost:8080", request.getHeader("Host"));
    }

    @Test
    public void testInvalidHeader_WhereColonIsMissing() {
        try {
            httpParser.parse(generateInValidTestCase_whereColonIsMissing());
            fail();
        } catch (HttpParsingException e) {
            assertEquals(HttpStatusCode.BAD_REQUEST, e.getErrorCode());
        }
    }

    @Test
    public void testInvalidHeader_WhereKeyDoesNotExists() {
        try {
            httpParser.parse(generateInValidTestCase_whereKeyDoesNotExists());
            fail();
        } catch (HttpParsingException e) {
            assertEquals(HttpStatusCode.BAD_REQUEST, e.getErrorCode());
        }
    }



    private InputStream generateInValidTestCase_whereColonIsMissing() {
        String rawData = """
                GET / HTTP/1.1\r
                Origin: metrix.uz\r
                X-Code: something\r
                tenant: metrix\r
                User-Agent: PostmanRuntime/7.48.0\r
                Accept: */*\r
                Invalid\r
                Postman-Token: f75cb8ed-307e-4aac-b2c1-274b89d08b69\r
                Host: localhost:8080\r
                Accept-Encoding: gzip, deflate, br\r
                Connection: keep-alive\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }

    private InputStream generateInValidTestCase_whereKeyDoesNotExists() {
        String rawData = """
                GET / HTTP/1.1\r
                Origin: metrix.uz\r
                X-Code: something\r
                tenant: metrix\r
                User-Agent: PostmanRuntime/7.48.0\r
                Accept: */*\r
                : Invalid\r
                Postman-Token: f75cb8ed-307e-4aac-b2c1-274b89d08b69\r
                Host: localhost:8080\r
                Accept-Encoding: gzip, deflate, br\r
                Connection: keep-alive\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
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
                EmptyKey:\r
                Accept-Encoding: gzip, deflate, br\r
                Connection: keep-alive\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }

}
