package uz.hikmatullo.httpserver.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.hikmatullo.httpserver.core.model.HttpRequest;
import uz.hikmatullo.httpserver.core.model.HttpResponse;
import uz.hikmatullo.httpserver.core.model.HttpStatusCode;
import uz.hikmatullo.httpserver.util.FileUtils;

import java.io.IOException;
import java.io.InputStream;

public class TestController {

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

}
