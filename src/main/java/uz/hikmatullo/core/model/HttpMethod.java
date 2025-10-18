package uz.hikmatullo.core.model;

public enum HttpMethod {
    GET, POST, PUT, DELETE, PATCH, CONNECT, OPTION, HEAD;

    public static final int MAX_LENGTH;

    static {
        int tempMaxLength = -1;
        for (HttpMethod method : values()) {
            if (method.name().length() > tempMaxLength) {
                tempMaxLength = method.name().length();
            }
        }
        MAX_LENGTH = tempMaxLength;
    }
}
