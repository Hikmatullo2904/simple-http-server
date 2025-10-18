package uz.hikmatullo.core.parser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uz.hikmatullo.core.exception.HttpParsingException;
import uz.hikmatullo.core.model.HttpMethod;
import uz.hikmatullo.core.model.HttpRequest;
import uz.hikmatullo.core.model.HttpStatusCode;
import uz.hikmatullo.core.model.HttpVersion;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is general test class for HttpParser
 * We test method, params and path.
* */

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpParserTest {
    private HttpParser httpParser;

    @BeforeAll
    public void beforeClass() {
        httpParser = new HttpParser();
    }

    @Test
    void parseGetHttpRequest() {
        HttpRequest request = null;
        try {
            request = httpParser.parse(
                    generateValidGETTestCase()
            );
        } catch (HttpParsingException e) {
            fail(e);
        }

        assertNotNull(request);
        assertEquals(HttpMethod.GET, request.getMethod());
        assertEquals("/", request.getPath());
        assertEquals("HTTP/1.1", request.getOriginalHttpVersion());
        assertEquals(HttpVersion.HTTP_1_1, request.getHttpVersion());
    }

    @Test
    void parseHttpRequestBadMethod1() {
        try {
             httpParser.parse(
                    generateBadTestCaseMethodName1()
            );

            fail();
        } catch (HttpParsingException e) {
            assertEquals(HttpStatusCode.NOT_IMPLEMENTED_METHOD, e.getErrorCode());
        }
    }

    @Test
    void testPath() {
         HttpRequest request = httpParser.parse(generateValidTestCaseWithParamAndPath());
         assertEquals("/users/12/info", request.getPath());
    }

    @Test
    void testParams() {
         HttpRequest request = httpParser.parse(generateValidTestCaseWithParamAndPath());
         assertEquals("Andijon", request.getParameter("region"));
    }

    @Test
    void testInvalidParams() {
        HttpRequest request = httpParser.parse(generateValidTestCaseWithParamAndPath());
        assertNull(request.getParameter("Region"));
    }

    @Test
    void testRawQuery() {
        HttpRequest request = httpParser.parse(generateValidTestCaseWithParamAndPath());
        assertEquals("region=Andijon&district=Shahrihon&text=salom%20dunyo", request.getRawQuery());
    }

    @Test
    void testTarget() {
        HttpRequest request = httpParser.parse(generateValidTestCaseWithParamAndPath());
        assertEquals("/users/12/info?region=Andijon&district=Shahrihon&text=salom%20dunyo", request.getTarget());
    }



    @Test
    void parseHttpRequestBadMethod2() {
        try {
            httpParser.parse(
                    generateBadTestCaseMethodName2()
            );
            fail();
        } catch (HttpParsingException e) {
            assertEquals(HttpStatusCode.NOT_IMPLEMENTED_METHOD, e.getErrorCode());
        }
    }

    @Test
    void testValidPostMethod() {

        HttpRequest request = httpParser.parse(
                generateValidPostMethodTestCase()
        );

        assertEquals(HttpMethod.POST, request.getMethod());

    }

    @Test
    void testValidPutMethod() {
        HttpRequest request = httpParser.parse(
                generateValidPutMethodTestCase()
        );

        assertEquals(HttpMethod.PUT, request.getMethod());
    }

    @Test
    void testValidDeleteMethod() {
        HttpRequest request = httpParser.parse(
                generateValidDeleteMethodTestCase()
        );

        assertEquals(HttpMethod.DELETE, request.getMethod());
    }

    @Test
    void testValidPatchMethod() {
        HttpRequest request = httpParser.parse(
                generateValidPatchMethodTestCase()
        );

        assertEquals(HttpMethod.PATCH, request.getMethod());
    }

    @Test
    void parseHttpRequestInvNumItems1() {
        try {
            httpParser.parse(
                    generateBadTestCaseRequestLineInvNumItems1()
            );
            fail();
        } catch (HttpParsingException e) {
            assertEquals(HttpStatusCode.BAD_REQUEST, e.getErrorCode());
        }
    }

    @Test
    void parseHttpEmptyRequestLine() {
        try {
            httpParser.parse(
                    generateBadTestCaseEmptyRequestLine()
            );
            fail();
        } catch (HttpParsingException e) {
            assertEquals(HttpStatusCode.BAD_REQUEST, e.getErrorCode());
        }
    }

    @Test
    void parseHttpRequestLineCRnoLF() {
        try {
             httpParser.parse(
                    generateBadTestCaseRequestLineOnlyCRnoLF()
            );
            fail();
        } catch (HttpParsingException e) {
            assertEquals(HttpStatusCode.BAD_REQUEST, e.getErrorCode());
        }
    }


    @Test
    void parseHttpRequestUnsupportedHttpVersion() {
        try {
            HttpRequest request = httpParser.parse(
                    generateUnsupportedHttpVersionTestCase()
            );
            System.out.println(request);
            fail();
        } catch (HttpParsingException e) {
            assertEquals(HttpStatusCode.HTTP_VERSION_NOT_SUPPORTED, e.getErrorCode());
        }
    }

    @Test
    void parseHttpRequestSupportedHttpVersion1() {
        try {
            HttpRequest request = httpParser.parse(
                    generateSupportedHttpVersion1()
            );
            assertNotNull(request);
            assertEquals(HttpVersion.HTTP_1_1, request.getHttpVersion());
            assertEquals("HTTP/1.2", request.getOriginalHttpVersion());
        } catch (HttpParsingException e) {
            fail();
        }
    }

    private InputStream generateValidGETTestCase() {
        String rawData = """
                GET / HTTP/1.1\r
                Host: localhost:8080\r
                Connection: keep-alive\r
                Upgrade-Insecure-Requests: 1\r
                User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36\r
                Sec-Fetch-User: ?1\r
                Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3\r
                Sec-Fetch-Site: none\r
                Sec-Fetch-Mode: navigate\r
                Accept-Encoding: gzip, deflate, br\r
                Accept-Language: en-US,en;q=0.9,es;q=0.8,pt;q=0.7,de-DE;q=0.6,de;q=0.5,la;q=0.4\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }

    private InputStream generateBadTestCaseMethodName1() {
        String rawData = """
                GeT / HTTP/1.1\r
                Host: localhost:8080\r
                Accept-Language: en-US,en;q=0.9,es;q=0.8,pt;q=0.7,de-DE;q=0.6,de;q=0.5,la;q=0.4\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }

    private InputStream generateBadTestCaseMethodName2() {
        String rawData = """
                GETTTT / HTTP/1.1\r
                Host: localhost:8080\r
                Accept-Language: en-US,en;q=0.9,es;q=0.8,pt;q=0.7,de-DE;q=0.6,de;q=0.5,la;q=0.4\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }

    private InputStream generateBadTestCaseRequestLineInvNumItems1() {
        String rawData = "GET / AAAAAA HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Accept-Language: en-US,en;q=0.9,es;q=0.8,pt;q=0.7,de-DE;q=0.6,de;q=0.5,la;q=0.4\r\n" +
                "\r\n";

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }

    private InputStream generateBadTestCaseEmptyRequestLine() {
        String rawData = """
                \r
                Host: localhost:8080\r
                Accept-Language: en-US,en;q=0.9,es;q=0.8,pt;q=0.7,de-DE;q=0.6,de;q=0.5,la;q=0.4\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }

    private InputStream generateBadTestCaseRequestLineOnlyCRnoLF() {
        String rawData = "GET / HTTP/1.1\r" + // <----- no LF
                "Host: localhost:8080\r\n" +
                "Accept-Language: en-US,en;q=0.9,es;q=0.8,pt;q=0.7,de-DE;q=0.6,de;q=0.5,la;q=0.4\r\n" +
                "\r\n";

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }


    private InputStream generateUnsupportedHttpVersionTestCase() {
        String rawData = """
                GET / HTTP/3.0\r
                Host: localhost:8080\r
                Connection: keep-alive\r
                Upgrade-Insecure-Requests: 1\r
                User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36\r
                Sec-Fetch-User: ?1\r
                Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3\r
                Sec-Fetch-Site: none\r
                Sec-Fetch-Mode: navigate\r
                Accept-Encoding: gzip, deflate, br\r
                Accept-Language: en-US,en;q=0.9,es;q=0.8,pt;q=0.7,de-DE;q=0.6,de;q=0.5,la;q=0.4\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }

    private InputStream generateSupportedHttpVersion1() {
        String rawData = """
                GET / HTTP/1.2\r
                Host: localhost:8080\r
                Connection: keep-alive\r
                Upgrade-Insecure-Requests: 1\r
                User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36\r
                Sec-Fetch-User: ?1\r
                Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3\r
                Sec-Fetch-Site: none\r
                Sec-Fetch-Mode: navigate\r
                Accept-Encoding: gzip, deflate, br\r
                Accept-Language: en-US,en;q=0.9,es;q=0.8,pt;q=0.7,de-DE;q=0.6,de;q=0.5,la;q=0.4\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }

    private InputStream generateValidPostMethodTestCase() {
        String rawData = """
                POST /users/12 HTTP/2.0\r
                Origin: metrix.uz\r
                host: localhost:8080\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }

    private InputStream generateValidPutMethodTestCase() {
        String rawData = """
                PUT /users/12 HTTP/1.1\r
                Origin: metrix.uz\r
                host: localhost:8080\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }

    private InputStream generateValidDeleteMethodTestCase() {
        String rawData = """
                DELETE /users/12 HTTP/1.1\r
                Origin: metrix.uz\r
                host: localhost:8080\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }

    private InputStream generateValidPatchMethodTestCase() {
        String rawData = """
                PATCH /users/12 HTTP/2.0\r
                Origin: metrix.uz\r
                host: localhost:8080\r
                \r
                """;

        return new ByteArrayInputStream(
                rawData.getBytes(
                        StandardCharsets.US_ASCII
                )
        );
    }



    private InputStream generateValidTestCaseWithParamAndPath() {
        String rawData = """
                POST /users/12/info?region=Andijon&district=Shahrihon&text=salom%20dunyo HTTP/1.1\r
                Origin: metrix.uz\r
                X-Code: something\r
                tenant: metrix\r
                User-Agent: PostmanRuntime/7.48.0\r
                Accept: */*\r
                Postman-Token: 5fed51c5-a60a-41db-8ce1-5f7789d8b96e\r
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
}