package uz.hikmatullo;

import uz.hikmatullo.config.ConfigManager;
import uz.hikmatullo.config.Configuration;

public class HttpServer {
    public static void main(String[] args) {
        System.out.println("Server started..");

        String filePath = "src/main/resources/http.json";
        ConfigManager.getInstance().loadConfiguration(filePath);
        Configuration currentConfiguration = ConfigManager.getInstance().getCurrentConfiguration();
        System.out.println("Application initialized in port: " + currentConfiguration.getPort());
        System.out.println("Application web root is: " + currentConfiguration.getWebroot());
    }
}
