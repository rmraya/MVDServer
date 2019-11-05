package com.maxprograms.mvdserver;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RedirectHandler implements HttpHandler {

    private static Logger logger = System.getLogger(RedirectHandler.class.getName());
    private MVDServer parent;

    public RedirectHandler(MVDServer parent) throws IOException {
        this.parent = parent;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        
        String newLocation = "https://" + parent.getHostName() + uri.toString();
        
        logger.log(Level.INFO, "redirecting to " + newLocation);

        StringBuilder builder = new StringBuilder();
        builder.append("<html><head><meta http-equiv=\"refresh\" content=\"0; URL=");
        builder.append(newLocation);
        builder.append("\"/><title>&nbsp;</title></head>&nbsp;<body></body></html>");

        exchange.getResponseHeaders().add("content-type", "text/html");
        exchange.getResponseHeaders().add("X-FRAME-OPTIONS", "sameorigin");
        exchange.getResponseHeaders().add("X-XSS-Protection", "1; mode=block");
        exchange.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        exchange.getResponseHeaders().add("X-Permitted-Cross-Domain-Policies", "master-only");
        exchange.getResponseHeaders().add("Content-Security-Policy", "report-uri https://maxprograms.com");
        exchange.getResponseHeaders().add("Referrer-Policy", "no-referrer-when-downgrade");
        exchange.getResponseHeaders().add("Feature-Policy", "microphone 'none'; camera 'none'");

        byte[] array = builder.toString().getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(200, array.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(array, 0, array.length);
        }
    }

}
