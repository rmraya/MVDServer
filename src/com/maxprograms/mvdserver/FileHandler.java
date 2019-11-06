/*******************************************************************************
 * Copyright (c) 2019 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.mvdserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.util.List;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONObject;

public class FileHandler implements HttpHandler {

    private static Logger logger = System.getLogger(FileHandler.class.getName());

    private JSONObject contentTypes;
    private MVDServer parent;

    public FileHandler(MVDServer parent) throws IOException {
        this.parent = parent;
        loadContentTypes();
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            URI uri = exchange.getRequestURI();
            String url = uri.toString();

            if (url.isEmpty() || "/".equals(url)) {
                url = "/index.html";
            }
            if (url.startsWith("/stop?key=")) {
                logger.log(Level.INFO, "Stop requested");
                parent.stopServer(url.substring("/stop?key=".length()).trim());      
                return;          
            }

            File resource = new File(parent.getWebDir(), url);

            if (resource.exists()) {
                String contentType = "text/html";
                String name = resource.getName().toLowerCase();
                if (name.indexOf('.') != -1) {
                    String extension = name.substring(name.lastIndexOf('.'));
                    if (contentTypes.has(extension)) {
                        contentType = contentTypes.getString(extension);
                    } else {
                        logger.log(Level.INFO, () -> "Unknown extension: " + extension);
                    }
                }
                exchange.getResponseHeaders().add("ETag", "LM" + resource.lastModified() + "L" + resource.length());
                exchange.getResponseHeaders().add("content-type", contentType);
                exchange.getResponseHeaders().add("X-FRAME-OPTIONS", "sameorigin");
                exchange.getResponseHeaders().add("X-XSS-Protection", "1; mode=block");
                exchange.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
                exchange.getResponseHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                exchange.getResponseHeaders().add("X-Permitted-Cross-Domain-Policies", "master-only");
                exchange.getResponseHeaders().add("Content-Security-Policy", "report-uri https://maxprograms.com");
                exchange.getResponseHeaders().add("Referrer-Policy", "no-referrer-when-downgrade");
                exchange.getResponseHeaders().add("Feature-Policy", "microphone 'none'; camera 'none'");

                Headers headers = exchange.getRequestHeaders();
                if (headers.containsKey("If-None-Match")) {
                    List<String> value = headers.get("If-None-Match");
                    if (!value.isEmpty()
                            && ("LM" + resource.lastModified() + "L" + resource.length()).equals(value.get(0))) {
                        exchange.sendResponseHeaders(304, -1l);
                        return;
                    }
                }
                exchange.sendResponseHeaders(200, resource.length());
                try (FileInputStream stream = new FileInputStream(resource)) {
                    try (OutputStream os = exchange.getResponseBody()) {
                        byte[] array = new byte[2048];
                        int read;
                        while ((read = stream.read(array)) != -1) {
                            os.write(array, 0, read);
                        }
                    }
                }
            } else {
                logger.log(Level.WARNING, "Missing resource requested: " + uri.toString());
                exchange.getResponseHeaders().add("Upgrade-Insecure-Requests", "1");
                exchange.sendResponseHeaders(404, -1l);
            }
        } catch (IOException ioe) {
            logger.log(Level.ERROR, "Error processing file " + exchange.getRequestURI().toString(), ioe);
        }
    }

    private void loadContentTypes() throws IOException {
        StringBuilder builder = new StringBuilder();
        try (InputStream stream = FileHandler.class.getResourceAsStream("ContentTypes.json")) {
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                try (BufferedReader buffer = new BufferedReader(reader)) {
                    String line = buffer.readLine();
                    while (line != null) {
                        builder.append(line);
                        builder.append('\n');
                        line = buffer.readLine();
                    }
                }
            }
        }
        contentTypes = new JSONObject(builder.toString());
    }

}
