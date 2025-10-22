package uz.hikmatullo.httpserver.util;


import java.io.*;
import java.util.Base64;

/**
 * Simple utility class for reading and writing files from/to the resource folder.
 */
public class FileUtils {
    public static boolean exist(String fileName) {
        return Constants.class.getResource(Constants.FILES_DIR + fileName) != null;
    }

    public static String probeContentType(String fileName) {
        final String[] tokens = fileName.split("\\.");
        final String extension = tokens[tokens.length - 1];

        return switch (extension) {
            case "html" -> "text/html";
            case "css" -> "text/css";
            case "gif" -> "image/gif";
            case "jpg", "jpeg" -> "image/jpeg";
            case "js" -> "text/javascript";
            case "json" -> "application/json";
            case "mp4" -> "video/mp4";
            case "png" -> "image/png";
            default -> "text/plain";
        };

    }

    public static InputStream getInputStream(String fileName) {
        return Constants.class.getResourceAsStream(Constants.FILES_DIR + fileName);
    }

    public static void saveFile(String fileName, String base64) {
        final String filePath = Constants.FILES_CANONICAL_PATH + "uploaded/" + fileName;
        final File physicalFile = new File(filePath);

        try {
            physicalFile.getParentFile().mkdirs();
            physicalFile.createNewFile();
            try (OutputStream stream = new FileOutputStream(physicalFile)) {
                if (base64.contains(",")) {
                    base64 = base64.split(",")[1];
                }

                stream.write(Base64.getMimeDecoder().decode(base64));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
