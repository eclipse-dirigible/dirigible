/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.http;

import java.io.IOException;
import org.eclipse.dirigible.components.api.http.HttpClientFacade;

/**
 * Synchronous outbound HTTP client for calling third-party APIs from controllers, jobs, and
 * listeners. Options (headers, query params, body, timeouts, basic-auth credentials) are passed as
 * a JSON document — the same shape the TS / JS surface accepts — so identical option objects can be
 * shared between Java and script-side callers.
 * <p>
 * The client is blocking; for non-trivial latency, wrap calls in a
 * {@link java.util.concurrent.CompletableFuture#supplyAsync(java.util.function.Supplier)} or batch
 * them through a small executor. For long-running streaming downloads, drop down to
 * {@code org.apache.hc.client5.http.impl.classic.HttpClients} directly.
 */
public final class HttpClient {

    private HttpClient() {}

    public static String get(String url) throws IOException {
        return HttpClientFacade.get(url, null);
    }

    public static String get(String url, String optionsJson) throws IOException {
        return HttpClientFacade.get(url, optionsJson);
    }

    public static String post(String url, String optionsJson) throws IOException {
        return HttpClientFacade.post(url, optionsJson);
    }

    public static String put(String url, String optionsJson) throws IOException {
        return HttpClientFacade.put(url, optionsJson);
    }

    public static String patch(String url, String optionsJson) throws IOException {
        return HttpClientFacade.patch(url, optionsJson);
    }

    public static String delete(String url, String optionsJson) throws IOException {
        return HttpClientFacade.delete(url, optionsJson);
    }

    public static String head(String url, String optionsJson) throws IOException {
        return HttpClientFacade.head(url, optionsJson);
    }
}
