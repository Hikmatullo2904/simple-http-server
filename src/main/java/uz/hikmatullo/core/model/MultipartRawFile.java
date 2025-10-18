package uz.hikmatullo.core.model;



public class MultipartRawFile {
    private final String fieldName; // e.g., "profilePic"
    private final String fileName;  // e.g., "photo.jpg"
    private final String contentType; // e.g., "image/jpeg"
    private final byte[] data;

    public MultipartRawFile(String fieldName, String fileName, String contentType, byte[] data) {
        this.fieldName = fieldName;
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }
}
