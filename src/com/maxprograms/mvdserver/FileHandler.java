/*******************************************************************************
 * Copyright (c) 2019-2020 Maxprograms.
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
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONObject;

public class FileHandler implements HttpHandler {

    private static Logger logger = System.getLogger(FileHandler.class.getName());

    private JSONObject contentTypes;
    private JSONObject cacheTimes;
    private JSONObject permanentRedirects;
    private MVDServer parent;

    public FileHandler(MVDServer parent) throws IOException {
        this.parent = parent;
        loadContentTypes();
        loadCacheTimes();
        loadRedirects();
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
                parent.stopServer(url.substring("/stop?key=".length()).trim());
                return;
            }
            if ("/redirects.json".equals(url)) {
                logger.log(Level.WARNING, () -> "Rejected access to " + uri.toString());
                exchange.sendResponseHeaders(403, -1l);
                return;
            }
            if (url.indexOf('?') != -1) {
                url = url.substring(0, url.indexOf('?'));
            }

            File resource = new File(parent.getWebDir(), url);
            if (resource.isDirectory()) {
                resource = new File(resource, "index.html");
            }

            if (resource.exists()) {
                String etag = "W/" + resource.lastModified() + "L" + resource.length();
                String contentType = "text/html";
                String cacheTime = "";
                String name = resource.getName().toLowerCase();
                if (name.indexOf('.') != -1) {
                    String extension = name.substring(name.lastIndexOf('.'));
                    if (contentTypes.has(extension)) {
                        contentType = contentTypes.getString(extension);
                    } else {
                        logger.log(Level.INFO, () -> "Unknown extension: " + extension);
                    }
                    if (cacheTimes.has(extension)) {
                        cacheTime = "public, max-age=" + cacheTimes.getInt(extension);
                    }
                }

                Headers headers = exchange.getRequestHeaders();

                String pragma = headers.getFirst("Pragma");
                String cacheControl = headers.getFirst("Cache-Control");
                String etagMatch = headers.getFirst("If-None-Match");

                if (!("no-cache".equalsIgnoreCase(pragma) || "no-cache".equalsIgnoreCase(cacheControl)
                        || "max-age=0".equalsIgnoreCase(cacheControl) || !etag.equalsIgnoreCase(etagMatch))) {
                    exchange.sendResponseHeaders(304, -1l);
                    return;
                }

                exchange.getResponseHeaders().add("ETag", etag);
                exchange.getResponseHeaders().add("content-type", contentType);
                exchange.getResponseHeaders().add("X-FRAME-OPTIONS", "sameorigin");
                exchange.getResponseHeaders().add("X-XSS-Protection", "1; mode=block");
                exchange.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
                exchange.getResponseHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                exchange.getResponseHeaders().add("X-Permitted-Cross-Domain-Policies", "master-only");
                exchange.getResponseHeaders().add("Content-Security-Policy", "report-uri https://maxprograms.com");
                exchange.getResponseHeaders().add("Referrer-Policy", "no-referrer-when-downgrade");
                exchange.getResponseHeaders().add("Permissions-Policy", "microphone=(), camera=()");
                if (!cacheTime.isEmpty()) {
                    exchange.getResponseHeaders().add("Cache-Control", cacheTime);
                }

                exchange.sendResponseHeaders(200, resource.length());

                if ("GET".equals(exchange.getRequestMethod())) {
                    try (FileInputStream stream = new FileInputStream(resource)) {
                        try (OutputStream os = exchange.getResponseBody()) {
                            byte[] array = new byte[2048];
                            int read;
                            while ((read = stream.read(array)) != -1) {
                                os.write(array, 0, read);
                            }
                        }
                    }
                }
            } else {
                if (permanentRedirects.has(url)) {
                    String newLocation = permanentRedirects.getString(url);
                    exchange.getResponseHeaders().add("Location", newLocation);
                    exchange.sendResponseHeaders(301, -1);
                    logger.log(Level.INFO, () -> "Redirected " + uri.toString() + " to " + newLocation);
                } else {
                    logger.log(Level.WARNING, () -> "Missing resource requested: " + uri.toString());
                    exchange.getResponseHeaders().add("Upgrade-Insecure-Requests", "1");
                    exchange.sendResponseHeaders(404, -1l);
                }
            }
        } catch (IOException ioe) {
            logger.log(Level.ERROR,
                    () -> "Received `" + ioe.getMessage() + "` processing " + exchange.getRequestURI().toString());
        }
    }

    private void loadContentTypes() throws IOException {
        StringBuilder builder = new StringBuilder();
        try (InputStream stream = FileHandler.class.getResourceAsStream("ContentTypes.json")) {
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                try (BufferedReader buffer = new BufferedReader(reader)) {
                    String line = "";
                    while ((line = buffer.readLine()) != null) {
                        if (!builder.isEmpty()) {
                            builder.append('\n');
                        }
                        builder.append(line);
                    }
                }
            }
        }
        contentTypes = new JSONObject(builder.toString());
    }

    private void loadCacheTimes() throws IOException {
        StringBuilder builder = new StringBuilder();
        try (InputStream stream = FileHandler.class.getResourceAsStream("CacheTimes.json")) {
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                try (BufferedReader buffer = new BufferedReader(reader)) {
                    String line = "";
                    while ((line = buffer.readLine()) != null) {
                        if (!builder.isEmpty()) {
                            builder.append('\n');
                        }
                        builder.append(line);
                    }
                }
            }
        }
        cacheTimes = new JSONObject(builder.toString());
    }

    private void loadRedirects() throws IOException {
        StringBuilder builder = new StringBuilder();
        File redirectsFile = new File(parent.getWebDir(), "redirects.json");
        if (!redirectsFile.exists()) {
            permanentRedirects = new JSONObject();
            return;
        }
        try (InputStream stream = new FileInputStream(redirectsFile)) {
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                try (BufferedReader buffer = new BufferedReader(reader)) {
                    String line = "";
                    while ((line = buffer.readLine()) != null) {
                        if (!builder.isEmpty()) {
                            builder.append('\n');
                        }
                        builder.append(line);
                    }
                }
            }
        }
        permanentRedirects = new JSONObject(builder.toString());
    }
}
