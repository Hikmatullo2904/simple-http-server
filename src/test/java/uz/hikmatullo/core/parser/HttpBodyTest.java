package uz.hikmatullo.core.parser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uz.hikmatullo.core.exception.HttpParsingException;
import uz.hikmatullo.core.model.HttpRequest;
import uz.hikmatullo.core.model.HttpStatusCode;

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

    @Test
    public void testValidChunkedBody() {
        HttpRequest request = httpParser.parse(generateValidChunkedTestCase());
        assertEquals("HelloWorld", request.getBody());
    }

    private InputStream generateValidChunkedTestCase() {
        String rawData = """
            POST /upload HTTP/1.1\r
            Host: localhost:8080\r
            Transfer-Encoding: chunked\r
            Content-Type: text/plain\r
            \r
            5\r
            Hello\r
            5\r
            World\r
            0\r
            \r
            """;

        return new ByteArrayInputStream(
                rawData.getBytes(StandardCharsets.US_ASCII)
        );
    }

    @Test
    public void testInvalidChunkedBody_MissingFinalZeroChunk() {
        try {
            httpParser.parse(generateInvalidChunkedTestCase_MissingFinalZeroChunk());
            fail();
        } catch (HttpParsingException e) {
            assertEquals(HttpStatusCode.BAD_REQUEST, e.getErrorCode());
        }
    }

    @Test
    public void testPlainTextBodyParsing() {
        HttpRequest request = httpParser.parse(generateValidTestCase_withPlainTextBody());
        assertEquals("Hello, this is plain text", request.getBody());
    }

    @Test
    public void testXmlBodyParsing() {
        HttpRequest request = httpParser.parse(generateValidTestCase_withXmlBody());
        assertEquals("<user><name>Hikmatullo</name></user>", request.getBody());
    }
    @Test
    public void testBodyWithoutContentType() {
        try{
            httpParser.parse(generateValidTestCase_withoutContentType());
            fail();
        }catch (HttpParsingException e) {
            assertEquals(HttpStatusCode.UNSUPPORTED_MEDIA_TYPE, e.getErrorCode());
        }

    }

    @Test
    public void testInvalidTransferEncoding() {
        try {
            httpParser.parse(generateInValidTestCase_withUnsupportedTransferEncoding());
            fail();
        } catch (HttpParsingException e) {
            assertEquals(HttpStatusCode.NOT_IMPLEMENTED, e.getErrorCode());
        }
    }


    @Test
    public void testInvalidChunkedBody_CRLF_Missing() {
        try {
            httpParser.parse(generateInValidChunkedTestCase_missingCRLF());
            fail();
        } catch (HttpParsingException e) {
            assertEquals(HttpStatusCode.BAD_REQUEST, e.getErrorCode());
        }
    }


    private InputStream generateInvalidChunkedTestCase_MissingFinalZeroChunk() {
        String rawData = """
            POST /upload HTTP/1.1\r
            Host: localhost:8080\r
            Transfer-Encoding: chunked\r
            Content-Type: text/plain\r
            \r
            5\r
            Hello\r
            5\r
            World\r
            """;

        return new ByteArrayInputStream(
                rawData.getBytes(StandardCharsets.US_ASCII)
        );
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



    private InputStream generateValidTestCase_withPlainTextBody() {
        String raw = """
            POST /submit HTTP/1.1\r
            Host: localhost:8080\r
            Content-Type: text/plain\r
            Content-Length: 25\r
            \r
            Hello, this is plain text
            """;
        return new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII));
    }

    private InputStream generateValidTestCase_withXmlBody() {
        String raw = """
            POST /api/xml HTTP/1.1\r
            Host: localhost:8080\r
            Content-Type: application/xml\r
            Content-Length: 36\r
            \r
            <user><name>Hikmatullo</name></user>
            """;
        return new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII));
    }

    private InputStream generateValidTestCase_withoutContentType() {
        String raw = """
            POST /data HTTP/1.1\r
            Host: localhost:8080\r
            Content-Length: 8\r
            \r
            raw-data
            """;
        return new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII));
    }

    private InputStream generateInValidTestCase_withUnsupportedTransferEncoding() {
        String raw = """
            POST /upload HTTP/1.1\r
            Host: localhost:8080\r
            Transfer-Encoding: gzip\r
            \r
            somebody
            """;
        return new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII));
    }

    private InputStream generateInValidChunkedTestCase_missingCRLF() {
        String raw = """
            POST /chunked HTTP/1.1\r
            Host: localhost:8080\r
            Transfer-Encoding: chunked\r
            \r
            5\r
            Hello
            5\r
            World0\r
            \r
            """;
        return new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII));
    }



}
