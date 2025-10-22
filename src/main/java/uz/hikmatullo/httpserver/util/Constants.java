package uz.hikmatullo.httpserver.util;

public class Constants {
    /**
     * Path to the resource folder relative to this exact class.
     */
    public static final String FILES_DIR = "/static/";

    public static final String FILES_CANONICAL_PATH = Constants.class.getResource("").getFile() + FILES_DIR;
}
