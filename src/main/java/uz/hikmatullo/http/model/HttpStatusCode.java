package uz.hikmatullo.http.model;

public enum HttpStatusCode {
    BAD_REQUEST(400, "Bad Request"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    URI_TOO_LONG(414, "URI Too Long"),
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header field is too large"),

    //Server errors
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED_METHOD(501, "Not Implemented Method"),
    HTTP_VERSION_NOT_SUPPORTED(505, "Http version not supported");



    public final int statusCode;
    public final String errorMessage;

    HttpStatusCode(int statusCode, String errorMessage) {
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
    }
}
