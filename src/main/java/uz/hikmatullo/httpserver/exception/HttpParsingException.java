package uz.hikmatullo.httpserver.exception;

import uz.hikmatullo.httpserver.core.model.HttpStatusCode;

public class HttpParsingException extends RuntimeException{
    private final HttpStatusCode errorCode;
    private String message;


    public HttpParsingException(HttpStatusCode errorCode) {
        this.errorCode = errorCode;
    }

    public HttpParsingException(HttpStatusCode errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public HttpStatusCode getErrorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
