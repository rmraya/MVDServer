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

import java.io.IOException;
import java.net.URI;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RedirectHandler implements HttpHandler {

    private MVDServer parent;

    public RedirectHandler(MVDServer parent) {
        this.parent = parent;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();

        String newLocation = "https://" + parent.getHostName() + uri.toString();
        exchange.getResponseHeaders().add("X-FRAME-OPTIONS", "sameorigin");
        exchange.getResponseHeaders().add("X-XSS-Protection", "1; mode=block");
        exchange.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        exchange.getResponseHeaders().add("X-Permitted-Cross-Domain-Policies", "master-only");
        exchange.getResponseHeaders().add("Content-Security-Policy", "report-uri https://maxprograms.com");
        exchange.getResponseHeaders().add("Referrer-Policy", "no-referrer-when-downgrade");
        exchange.getResponseHeaders().add("Feature-Policy", "microphone 'none'; camera 'none'");
        exchange.getResponseHeaders().add("Location", newLocation);
        exchange.sendResponseHeaders(301, -1);
    }

}
