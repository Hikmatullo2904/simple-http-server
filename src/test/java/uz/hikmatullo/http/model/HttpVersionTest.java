package uz.hikmatullo.http.model;

import org.junit.jupiter.api.Test;
import uz.hikmatullo.http.exception.BadHttpVersionException;

import static org.junit.jupiter.api.Assertions.*;

class HttpVersionTest {
    @Test
    void getBestCompatibleVersionExactMatch() {
        HttpVersion version = null;
        try {
            version = HttpVersion.getBestCompatibleVersion("HTTP/1.1");
        } catch (BadHttpVersionException e) {
            fail();
        }
        assertNotNull(version);
        assertEquals(HttpVersion.HTTP_1_1, version);
    }

    @Test
    void getBestCompatibleVersionBadFormat() {

        try {
            HttpVersion.getBestCompatibleVersion("http/1.1");
            fail();
        } catch (BadHttpVersionException e) {
            assertEquals(HttpStatusCode.HTTP_VERSION_NOT_SUPPORTED, e.getErrorCode());
        }
    }

    @Test
    void getBestCompatibleVersionHigherVersion() {
        HttpVersion version;
        try {
            version = HttpVersion.getBestCompatibleVersion("HTTP/1.2");
            assertNotNull(version);
            assertEquals(HttpVersion.HTTP_1_1, version);
        } catch (BadHttpVersionException e) {
            fail();
        }
    }

}