package uz.hikmatullo.http.exception;

import uz.hikmatullo.http.model.HttpStatusCode;

public class HttpParsingException extends RuntimeException{
    private final HttpStatusCode errorCode;


    public HttpParsingException(HttpStatusCode errorCode) {
        this.errorCode = errorCode;
    }

    public HttpStatusCode getErrorCode() {
        return errorCode;
    }
}
