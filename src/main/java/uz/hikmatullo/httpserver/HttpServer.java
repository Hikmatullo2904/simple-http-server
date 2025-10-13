package uz.hikmatullo.httpserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.hikmatullo.httpserver.config.ConfigManager;
import uz.hikmatullo.httpserver.config.Configuration;
import uz.hikmatullo.httpserver.core.ServerListenerThread;

import java.io.IOException;


public class HttpServer {

    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);

    public static void main(String[] args) {
        System.out.println("Server started..");

        String filePath = "src/main/resources/http.json";
        ConfigManager.getInstance().loadConfiguration(filePath);
        Configuration currentConfiguration = ConfigManager.getInstance().getCurrentConfiguration();
        log.info("Application initialized in port: {}", currentConfiguration.getPort());
        log.info("Application web root is: {}", currentConfiguration.getWebroot());

        try {
            var serverListenerThread = new ServerListenerThread(currentConfiguration.getPort(),
                    currentConfiguration.getWebroot());
            serverListenerThread.start();
        } catch (IOException e) {
            log.error(e.getMessage());
        }

    }
}
