/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.http.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gson.JsonObject;

/**
 * The Class HttpClientRequestOptions.
 */
public class HttpClientRequestOptions {

    // for reference:
    // https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/config/RequestConfig.html

    /** The expect continue enabled. */
    private boolean expectContinueEnabled;

    /** The proxy host. */
    private String proxyHost;

    /** The proxy port. */
    private int proxyPort;

    /** The cookie spec. */
    private String cookieSpec;

    /** The redirects enabled. */
    private boolean redirectsEnabled;

    /** The relative redirects allowed. */
    private boolean relativeRedirectsAllowed;

    /** The circular redirects allowed. */
    private boolean circularRedirectsAllowed;

    /** The max redirects. */
    private int maxRedirects;

    /** The authentication enabled. */
    private boolean authenticationEnabled;

    /** The target preferred auth schemes. */
    private Collection<String> targetPreferredAuthSchemes;

    /** The proxy preferred auth schemes. */
    private Collection<String> proxyPreferredAuthSchemes;

    /** The connection request timeout. */
    private int connectionRequestTimeout;

    /** The connect timeout. */
    private int connectTimeout;

    /** The socket timeout. */
    private int socketTimeout;

    /** The content compression enabled. */
    private boolean contentCompressionEnabled;

    /** The ssl trust all enabled. */
    private boolean sslTrustAllEnabled;

    /** The data. */
    // binary content for POST and PUT
    private byte[] data;

    /** The text. */
    // text content for POST and PUT
    private String text;

    /** The files. */
    // file content for POST and PUT
    private String[] files;

    /** The character encoding. */
    // encoding for POST
    private String characterEncoding = StandardCharsets.UTF_8.name();

    /** The character encoding enabled. */
    // enable encoding for POST
    private boolean characterEncodingEnabled = true;

    /** The content type. */
    // content type for POST
    private String contentType = "text/plain";

    /** The headers. */
    // headers
    private List<HttpClientHeader> headers = new ArrayList<HttpClientHeader>();

    /** The params. */
    // params
    private List<HttpClientParam> params = new ArrayList<HttpClientParam>();

    /** The binary. */
    // whether to request as binary or text
    private boolean binary;

    /** The context. */
    // context
    private JsonObject context;

    /**
     * Checks if is expect continue enabled.
     *
     * @return true, if is expect continue enabled
     */
    public boolean isExpectContinueEnabled() {
        return expectContinueEnabled;
    }

    /**
     * Sets the expect continue enabled.
     *
     * @param expectContinueEnabled the new expect continue enabled
     */
    public void setExpectContinueEnabled(boolean expectContinueEnabled) {
        this.expectContinueEnabled = expectContinueEnabled;
    }

    /**
     * Gets the proxy host.
     *
     * @return the proxy host
     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * Sets the proxy host.
     *
     * @param proxyHost the new proxy host
     */
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    /**
     * Gets the proxy port.
     *
     * @return the proxy port
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * Sets the proxy port.
     *
     * @param proxyPort the new proxy port
     */
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * Gets the cookie spec.
     *
     * @return the cookie spec
     */
    public String getCookieSpec() {
        return cookieSpec;
    }

    /**
     * Sets the cookie spec.
     *
     * @param cookieSpec the new cookie spec
     */
    public void setCookieSpec(String cookieSpec) {
        this.cookieSpec = cookieSpec;
    }

    /**
     * Checks if is redirects enabled.
     *
     * @return true, if is redirects enabled
     */
    public boolean isRedirectsEnabled() {
        return redirectsEnabled;
    }

    /**
     * Sets the redirects enabled.
     *
     * @param redirectsEnabled the new redirects enabled
     */
    public void setRedirectsEnabled(boolean redirectsEnabled) {
        this.redirectsEnabled = redirectsEnabled;
    }

    /**
     * Checks if is relative redirects allowed.
     *
     * @return true, if is relative redirects allowed
     */
    public boolean isRelativeRedirectsAllowed() {
        return relativeRedirectsAllowed;
    }

    /**
     * Sets the relative redirects allowed.
     *
     * @param relativeRedirectsAllowed the new relative redirects allowed
     */
    public void setRelativeRedirectsAllowed(boolean relativeRedirectsAllowed) {
        this.relativeRedirectsAllowed = relativeRedirectsAllowed;
    }

    /**
     * Checks if is circular redirects allowed.
     *
     * @return true, if is circular redirects allowed
     */
    public boolean isCircularRedirectsAllowed() {
        return circularRedirectsAllowed;
    }

    /**
     * Sets the circular redirects allowed.
     *
     * @param circularRedirectsAllowed the new circular redirects allowed
     */
    public void setCircularRedirectsAllowed(boolean circularRedirectsAllowed) {
        this.circularRedirectsAllowed = circularRedirectsAllowed;
    }

    /**
     * Gets the max redirects.
     *
     * @return the max redirects
     */
    public int getMaxRedirects() {
        return maxRedirects;
    }

    /**
     * Sets the max redirects.
     *
     * @param maxRedirects the new max redirects
     */
    public void setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
    }

    /**
     * Checks if is authentication enabled.
     *
     * @return true, if is authentication enabled
     */
    public boolean isAuthenticationEnabled() {
        return authenticationEnabled;
    }

    /**
     * Sets the authentication enabled.
     *
     * @param authenticationEnabled the new authentication enabled
     */
    public void setAuthenticationEnabled(boolean authenticationEnabled) {
        this.authenticationEnabled = authenticationEnabled;
    }

    /**
     * Gets the target preferred auth schemes.
     *
     * @return the target preferred auth schemes
     */
    public Collection<String> getTargetPreferredAuthSchemes() {
        return targetPreferredAuthSchemes;
    }

    /**
     * Sets the target preferred auth schemes.
     *
     * @param targetPreferredAuthSchemes the new target preferred auth schemes
     */
    public void setTargetPreferredAuthSchemes(Collection<String> targetPreferredAuthSchemes) {
        this.targetPreferredAuthSchemes = targetPreferredAuthSchemes;
    }

    /**
     * Gets the proxy preferred auth schemes.
     *
     * @return the proxy preferred auth schemes
     */
    public Collection<String> getProxyPreferredAuthSchemes() {
        return proxyPreferredAuthSchemes;
    }

    /**
     * Sets the proxy preferred auth schemes.
     *
     * @param proxyPreferredAuthSchemes the new proxy preferred auth schemes
     */
    public void setProxyPreferredAuthSchemes(Collection<String> proxyPreferredAuthSchemes) {
        this.proxyPreferredAuthSchemes = proxyPreferredAuthSchemes;
    }

    /**
     * Gets the connection request timeout.
     *
     * @return the connection request timeout
     */
    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    /**
     * Sets the connection request timeout.
     *
     * @param connectionRequestTimeout the new connection request timeout
     */
    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    /**
     * Gets the connect timeout.
     *
     * @return the connect timeout
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the connect timeout.
     *
     * @param connectTimeout the new connect timeout
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Gets the socket timeout.
     *
     * @return the socket timeout
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * Sets the socket timeout.
     *
     * @param socketTimeout the new socket timeout
     */
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * Checks if is content compression enabled.
     *
     * @return true, if is content compression enabled
     */
    public boolean isContentCompressionEnabled() {
        return contentCompressionEnabled;
    }

    /**
     * Sets the content compression enabled.
     *
     * @param contentCompressionEnabled the new content compression enabled
     */
    public void setContentCompressionEnabled(boolean contentCompressionEnabled) {
        this.contentCompressionEnabled = contentCompressionEnabled;
    }

    /**
     * Checks if is ssl trust all enabled.
     *
     * @return true, if is ssl trust all enabled
     */
    public boolean isSslTrustAllEnabled() {
        return sslTrustAllEnabled;
    }

    /**
     * Sets the ssl trust all enabled.
     *
     * @param sslTrustAllEnabled the new ssl trust all enabled
     */
    public void setSslTrustAllEnabled(boolean sslTrustAllEnabled) {
        this.sslTrustAllEnabled = sslTrustAllEnabled;
    }

    /**
     * Gets the data.
     *
     * @return the data
     */
    public byte[] getData() {
        return data != null ? data.clone() : null;
    }

    /**
     * Sets the data.
     *
     * @param data the new data
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Gets the text.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text.
     *
     * @param text the new text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the files.
     *
     * @return the files
     */
    public String[] getFiles() {
        return files.clone();
    }

    /**
     * Sets the files.
     *
     * @param files the new files
     */
    public void setFiles(String[] files) {
        this.files = files;
    }

    /**
     * Gets the character encoding.
     *
     * @return the character encoding
     */
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    /**
     * Sets the character encoding.
     *
     * @param characterEncoding the new character encoding
     */
    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    /**
     * Checks if the character encoding is enabled.
     *
     * @return true, if the character encoding is enabled
     */
    public boolean isCharacterEncodingEnabled() {
        return characterEncodingEnabled;
    }

    /**
     * Sets the character encoding enabled.
     *
     * @param characcerEncodingEnabled the new character encoding enabled
     */
    public void setCharacterEncodingEnabled(boolean characcerEncodingEnabled) {
        this.characterEncodingEnabled = characcerEncodingEnabled;
    }

    /**
     * Gets the content type.
     *
     * @return the content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type.
     *
     * @param contentType the new content type
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Gets the headers.
     *
     * @return the headers
     */
    public List<HttpClientHeader> getHeaders() {
        return headers;
    }

    /**
     * Sets the headers.
     *
     * @param headers the new headers
     */
    public void setHeaders(List<HttpClientHeader> headers) {
        this.headers = headers;
    }

    /**
     * Gets the params.
     *
     * @return the params
     */
    public List<HttpClientParam> getParams() {
        return params;
    }

    /**
     * Sets the params.
     *
     * @param params the new params
     */
    public void setParams(List<HttpClientParam> params) {
        this.params = params;
    }

    /**
     * Checks if is binary.
     *
     * @return true, if is binary
     */
    public boolean isBinary() {
        return binary;
    }

    /**
     * Sets the binary.
     *
     * @param binary the new binary
     */
    public void setBinary(boolean binary) {
        this.binary = binary;
    }

    /**
     * Gets the context.
     *
     * @return the context
     */
    public JsonObject getContext() {
        return context;
    }

    /**
     * Sets the context.
     *
     * @param context the new context
     */
    public void setContext(JsonObject context) {
        this.context = context;
    }
}
