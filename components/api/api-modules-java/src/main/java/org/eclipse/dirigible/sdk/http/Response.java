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

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.dirigible.components.api.http.HttpResponseFacade;

/**
 * Writes the outbound HTTP response bound to the calling thread — status, headers, cookies, body.
 * Useful from {@link Controller @Controller} methods that want to stream bytes, set explicit status
 * codes, or send {@code 30x} redirects.
 * <p>
 * Most controllers can rely on the dispatcher's automatic Jackson serialization of return values
 * and reach for this class only at the edges (file downloads, binary responses, manual error
 * shapes). {@link #getNative()} hands back the underlying {@link HttpServletResponse} when you need
 * a Servlet-only method.
 */
public final class Response {

    private Response() {}

    public static HttpServletResponse getNative() {
        return HttpResponseFacade.getResponse();
    }

    public static boolean isValid() {
        return HttpResponseFacade.isValid();
    }

    public static void print(String text) {
        HttpResponseFacade.print(text);
    }

    public static void print(Object o) {
        HttpResponseFacade.print(o);
    }

    public static void println(String text) {
        HttpResponseFacade.println(text);
    }

    public static void write(byte[] bytes) {
        HttpResponseFacade.write(bytes);
    }

    public static void write(String input) {
        HttpResponseFacade.write(input);
    }

    public static boolean isCommitted() {
        return HttpResponseFacade.isCommitted();
    }

    public static void setContentType(String contentType) {
        HttpResponseFacade.setContentType(contentType);
    }

    public static String getContentType() {
        return HttpResponseFacade.getContentType();
    }

    public static void flush() {
        HttpResponseFacade.flush();
    }

    public static void close() {
        HttpResponseFacade.close();
    }

    public static void addCookie(String cookieJson) {
        HttpResponseFacade.addCookie(cookieJson);
    }

    public static boolean containsHeader(String name) {
        return HttpResponseFacade.containsHeader(name);
    }

    public static String encodeURL(String url) {
        return HttpResponseFacade.encodeURL(url);
    }

    public static String encodeRedirectURL(String url) {
        return HttpResponseFacade.encodeRedirectURL(url);
    }

    public static void sendError(int code, String message) throws IOException {
        HttpResponseFacade.sendError(code, message);
    }

    public static void sendError(int code) throws IOException {
        HttpResponseFacade.sendError(code);
    }

    public static void sendRedirect(String location) throws IOException {
        HttpResponseFacade.sendRedirect(location);
    }

    public static void setCharacterEncoding(String charset) {
        HttpResponseFacade.setCharacterEncoding(charset);
    }

    public static String getCharacterEncoding() {
        return HttpResponseFacade.getCharacterEncoding();
    }

    public static void setContentLength(int length) {
        HttpResponseFacade.setContentLength(length);
    }

    public static void setHeader(String name, String value) {
        HttpResponseFacade.setHeader(name, value);
    }

    public static void addHeader(String name, String value) {
        HttpResponseFacade.addHeader(name, value);
    }

    public static String getHeader(String name) {
        return HttpResponseFacade.getHeader(name);
    }

    public static String getHeaders(String name) {
        return HttpResponseFacade.getHeaders(name);
    }

    public static String getHeaderNames() {
        return HttpResponseFacade.getHeaderNames();
    }

    public static void setStatus(int code) {
        HttpResponseFacade.setStatus(code);
    }

    public static void reset() {
        HttpResponseFacade.reset();
    }

    public static void setLocale(String language) {
        HttpResponseFacade.setLocale(language);
    }

    public static String getLocale() {
        return HttpResponseFacade.getLocale();
    }

    public static OutputStream getOutputStream() throws IOException {
        return HttpResponseFacade.getOutputStream();
    }
}
