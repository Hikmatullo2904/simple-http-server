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
public class HttpBodyTest {

    private HttpParser httpParser;

    @BeforeAll
    public void beforeClass() {
        httpParser = new HttpParser();
    }

    @Test
    public void testJsonBodyParsing() {
        HttpRequest request = httpParser.parse(generateValidTestCase_withJsonBody());
        assertEquals("{\"name\":\"Hikmatullo\",\"age\":22}", request.getBody());
    }

    @Test
    public void testFormBodyParsing() {
        HttpRequest request = httpParser.parse(generateValidTestCase_withFormBody());
        assertEquals("username=admin&password=1234", request.getBody());
    }

    @Test
    public void testEmptyBodyParsing() {
        HttpRequest request = httpParser.parse(generateValidTestCase_withEmptyBody());
        assertNull(request.getBody());
    }

    @Test
    public void testInvalidBody_WhenContentLengthIsLongerThanBody() {
        try {
            httpParser.parse(generateInValidTestCase_withShortBodyThanContentLength());
            fail();
        } catch (HttpParsingException e) {
            assertEquals(HttpStatusCode.BAD_REQUEST, e.getErrorCode());
        }
    }

    private InputStream generateInValidTestCase_withShortBodyThanContentLength() {
        String rawData = """
            POST /users HTTP/1.1\r
            Host: localhost:8080\r
            Content-Type: application/json\r
            Content-Length: 34\r
            \r
            {"name":"Hikmatullo"}
            """;

        return new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
    }



    private InputStream generateValidTestCase_withEmptyBody() {
        String rawData = """
            GET /users HTTP/1.1\r
            Host: localhost:8080\r
            Accept: */*\r
            Connection: keep-alive\r
            \r
            """;

        return new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
    }



    private InputStream generateValidTestCase_withFormBody() {
        String rawData = """
            POST /login HTTP/1.1\r
            Host: localhost:8080\r
            Content-Type: application/x-www-form-urlencoded\r
            Content-Length: 28\r
            \r
            username=admin&password=1234
            """;

        return new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
    }


    private InputStream generateValidTestCase_withJsonBody() {
        String rawData = """
            POST /users HTTP/1.1\r
            Host: localhost:8080\r
            Content-Type: application/json\r
            Content-Length: 30\r
            \r
            {"name":"Hikmatullo","age":22}
            """;

        return new ByteArrayInputStream(rawData.getBytes(StandardCharsets.US_ASCII));
    }





}
