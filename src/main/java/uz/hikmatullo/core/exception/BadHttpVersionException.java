package uz.hikmatullo.core.exception;

import uz.hikmatullo.core.model.HttpStatusCode;

public class BadHttpVersionException extends RuntimeException {
    private final HttpStatusCode errorCode;

    public BadHttpVersionException() {
        this.errorCode = HttpStatusCode.HTTP_VERSION_NOT_SUPPORTED;
    }

    public HttpStatusCode getErrorCode() {
        return errorCode;
    }
}
