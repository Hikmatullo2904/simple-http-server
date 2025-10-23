package uz.hikmatullo.httpserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.hikmatullo.httpserver.config.ConfigManager;
import uz.hikmatullo.httpserver.config.Configuration;
import uz.hikmatullo.httpserver.core.handler.RequestHandler;
import uz.hikmatullo.httpserver.core.model.HttpRequest;
import uz.hikmatullo.httpserver.core.model.HttpResponse;
import uz.hikmatullo.httpserver.core.model.HttpStatusCode;
import uz.hikmatullo.httpserver.runtime.HttpServer;

import java.io.IOException;
import java.io.InputStream;


public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        System.out.println("Server started..");

        String filePath = "src/main/resources/http.json";
        ConfigManager.getInstance().loadConfiguration(filePath);
        Configuration currentConfiguration = ConfigManager.getInstance().getCurrentConfiguration();
        log.info("Application initialized in port: {}", currentConfiguration.getPort());
        log.info("Application web root is: {}", currentConfiguration.getWebroot());

        try {
            var serverListenerThread = new HttpServer(currentConfiguration.getPort(), new TestController());
            serverListenerThread.start();
        } catch (IOException e) {
            log.error(e.getMessage());
        }

    }



    static class TestController implements RequestHandler {

        private static final Logger log = LoggerFactory.getLogger(TestController.class);


        public HttpResponse sendPage(HttpRequest request) {
            String path = request.getPath();
            String fileName;
            switch (path) {
                case "/" -> fileName = "index.html";
                case "/products" -> fileName = "product.html";
                case "/contact" -> fileName = "contact.html";
                case "/blog" -> fileName = "blog.html";
                case "/about" -> fileName = "about.html";
                default -> fileName = "404.html";
            }

            System.out.println("Path: " + path);

            if (!FileUtils.exist(fileName)) {
                log.error("File not exist");
                throw new RuntimeException("File not exist");
            }

            try {
                HttpResponse httpResponse = new HttpResponse(HttpStatusCode.OK);
                httpResponse.setProtocol("HTTP/1.1");
                InputStream inputStream = FileUtils.getInputStream(fileName);

                String contentType = FileUtils.probeContentType(fileName);
                httpResponse.addHeader("Content-Type", contentType);
                httpResponse.addHeader("Content-Length", String.valueOf(inputStream.available()));

                httpResponse.setBody(inputStream.readAllBytes());
                return httpResponse;
            }catch (IOException e) {
                log.error("I/O error happened {}", e.getMessage());
                throw new RuntimeException("I/O error happened");
            }


        }

        @Override
        public HttpResponse handle(HttpRequest httpRequest) {
            return sendPage(httpRequest);
        }
    }


    public static final String FILES_DIR = "/static/";
    static class FileUtils {
        public static boolean exist(String fileName) {
            return Main.class.getResource(FILES_DIR + fileName) != null;
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
            return Main.class.getResourceAsStream(FILES_DIR + fileName);
        }
    }
}
