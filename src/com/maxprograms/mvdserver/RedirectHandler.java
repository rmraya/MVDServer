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

        exchange.getResponseHeaders().add("Upgrade-Insecure-Requests", "1");
        exchange.getResponseHeaders().add("Location", newLocation);
        exchange.sendResponseHeaders(307, -1);
    }

}
