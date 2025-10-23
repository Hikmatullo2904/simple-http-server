package uz.hikmatullo.httpserver.core.handler;

import uz.hikmatullo.httpserver.core.model.HttpRequest;
import uz.hikmatullo.httpserver.core.model.HttpResponse;

public interface RequestHandler {
    HttpResponse handle(HttpRequest httpRequest);
}
