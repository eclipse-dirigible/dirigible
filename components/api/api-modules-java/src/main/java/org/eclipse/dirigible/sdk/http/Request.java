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

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.eclipse.dirigible.components.api.http.HttpRequestFacade;

/**
 * Inspects the inbound HTTP request bound to the calling thread — method, headers, cookies, query
 * parameters, body. Use this from a {@link Controller @Controller} method that needs more than the
 * parameter-binding annotations (e.g. raw header inspection, parameter iteration, streaming the
 * body).
 * <p>
 * {@link #getNative()} hands back the underlying {@link HttpServletRequest} when you need to call
 * into Servlet APIs that aren't exposed through the static helpers; the helpers themselves are
 * sufficient for the vast majority of use cases and keep call sites free of Servlet imports.
 */
public final class Request {

    private Request() {}

    public static HttpServletRequest getNative() {
        return HttpRequestFacade.getRequest();
    }

    public static boolean isValid() {
        return HttpRequestFacade.isValid();
    }

    public static String getMethod() {
        return HttpRequestFacade.getMethod();
    }

    public static String getRemoteUser() {
        return HttpRequestFacade.getRemoteUser();
    }

    public static String getPathInfo() {
        return HttpRequestFacade.getPathInfo();
    }

    public static String getHeader(String name) {
        return HttpRequestFacade.getHeader(name);
    }

    public static String getHeaderNames() {
        return HttpRequestFacade.getHeaderNames();
    }

    public static String getHeaders(String name) {
        return HttpRequestFacade.getHeaders(name);
    }

    public static String getCookies() {
        return HttpRequestFacade.getCookies();
    }

    public static String getAttribute(String name) {
        return HttpRequestFacade.getAttribute(name);
    }

    public static void setAttribute(String name, String value) {
        HttpRequestFacade.setAttribute(name, value);
    }

    public static void removeAttribute(String name) {
        HttpRequestFacade.removeAttribute(name);
    }

    public static String getAttributeNames() {
        return HttpRequestFacade.getAttributeNames();
    }

    public static boolean isUserInRole(String role) {
        return HttpRequestFacade.isUserInRole(role);
    }

    public static String getAuthType() {
        return HttpRequestFacade.getAuthType();
    }

    public static String getCharacterEncoding() {
        return HttpRequestFacade.getCharacterEncoding();
    }

    public static int getContentLength() {
        return HttpRequestFacade.getContentLength();
    }

    public static String getContentType() {
        return HttpRequestFacade.getContentType();
    }

    public static String getBytes() throws IOException {
        return HttpRequestFacade.getBytes();
    }

    public static String getText() throws IOException {
        return HttpRequestFacade.getText();
    }

    public static ServletInputStream getInputStream() throws IOException {
        return HttpRequestFacade.getInputStream();
    }

    public static String getParameter(String name) {
        return HttpRequestFacade.getParameter(name);
    }

    public static String getParameters() {
        return HttpRequestFacade.getParameters();
    }

    public static String getParameterNames() {
        return HttpRequestFacade.getParameterNames();
    }

    public static String getParameterValues(String name) {
        return HttpRequestFacade.getParameterValues(name);
    }

    public static String getResourcePath() {
        return HttpRequestFacade.getResourcePath();
    }

    public static String getProtocol() {
        return HttpRequestFacade.getProtocol();
    }

    public static String getScheme() {
        return HttpRequestFacade.getScheme();
    }

    public static String getContextPath() {
        return HttpRequestFacade.getContextPath();
    }

    public static String getServerName() {
        return HttpRequestFacade.getServerName();
    }

    public static int getServerPort() {
        return HttpRequestFacade.getServerPort();
    }

    public static String getQueryString() {
        return HttpRequestFacade.getQueryString();
    }

    public static String getRemoteAddress() {
        return HttpRequestFacade.getRemoteAddress();
    }

    public static String getRemoteHost() {
        return HttpRequestFacade.getRemoteHost();
    }

    public static String getLocale() {
        return HttpRequestFacade.getLocale();
    }

    public static String getRequestURI() {
        return HttpRequestFacade.getRequestURI();
    }

    public static boolean isSecure() {
        return HttpRequestFacade.isSecure();
    }

    public static String getRequestURL() {
        return HttpRequestFacade.getRequestURL();
    }

    public static String getServicePath() {
        return HttpRequestFacade.getServicePath();
    }
}
