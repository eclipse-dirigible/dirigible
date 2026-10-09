/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.http;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.classic.methods.HttpTrace;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.api.http.client.HttpClientFile;
import org.eclipse.dirigible.components.api.http.client.HttpClientHeader;
import org.eclipse.dirigible.components.api.http.client.HttpClientParam;
import org.eclipse.dirigible.components.api.http.client.HttpClientProxyUtils;
import org.eclipse.dirigible.components.api.http.client.HttpClientRequestOptions;
import org.eclipse.dirigible.components.api.http.client.HttpClientResponse;
import org.springframework.stereotype.Component;

/**
 * Java face for HTTP operations.
 */
@Component
public class HttpClientFacade {

    /**
     * Performs a GET request for the specified URL and options.
     *
     * @param url the URL
     * @param options the options
     * @return the response as JSON
     * @throws IOException In case an I/O exception occurs
     */
    public static final String get(String url, String options) throws IOException {
        HttpClientRequestOptions httpClientRequestOptions = parseOptions(options);
        HttpGet httpGet = createGetRequest(url, httpClientRequestOptions);
        CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(httpClientRequestOptions.isSslTrustAllEnabled());
        CloseableHttpResponse response = httpClient.execute(httpGet);
        return processResponse(response, httpClientRequestOptions.isBinary());
    }

    /**
     * Performs a POST request for the specified URL and options.
     *
     * @param url the URL
     * @param options the options
     * @return the response as JSON
     * @throws IOException In case an I/O exception occurs
     */
    public static final String post(String url, String options) throws IOException {
        HttpClientRequestOptions httpClientRequestOptions = parseOptions(options);
        HttpPost httpPost = createPostRequest(url, httpClientRequestOptions);
        CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(httpClientRequestOptions.isSslTrustAllEnabled());
        CloseableHttpResponse response = httpClient.execute(httpPost);
        return processResponse(response, httpClientRequestOptions.isBinary());
    }

    /**
     * Performs a PUT request for the specified URL and options.
     *
     * @param url the URL
     * @param options the options
     * @return the response as JSON
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static final String put(String url, String options) throws IOException {
        HttpClientRequestOptions httpClientRequestOptions = parseOptions(options);
        HttpPut httpPut = createPutRequest(url, httpClientRequestOptions);
        CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(httpClientRequestOptions.isSslTrustAllEnabled());
        CloseableHttpResponse response = httpClient.execute(httpPut);
        return processResponse(response, httpClientRequestOptions.isBinary());
    }

    /**
     * Performs a PATCH request for the specified URL and options.
     *
     * @param url the URL
     * @param options the options
     * @return the response as JSON
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static final String patch(String url, String options) throws IOException {
        HttpClientRequestOptions httpClientRequestOptions = parseOptions(options);
        HttpPatch httpPatch = createPatchRequest(url, httpClientRequestOptions);
        CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(httpClientRequestOptions.isSslTrustAllEnabled());
        CloseableHttpResponse response = httpClient.execute(httpPatch);
        return processResponse(response, httpClientRequestOptions.isBinary());
    }

    /**
     * Performs a DELETE request for the specified URL and options.
     *
     * @param url the URL
     * @param options the options
     * @return the response as JSON
     * @throws IOException In case an I/O exception occurs
     */
    public static final String delete(String url, String options) throws IOException {
        HttpClientRequestOptions httpClientRequestOptions = parseOptions(options);
        HttpDelete httpDelete = createDeleteRequest(url, httpClientRequestOptions);

        CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(httpClientRequestOptions.isSslTrustAllEnabled());
        CloseableHttpResponse response = httpClient.execute(httpDelete);
        return processResponse(response, httpClientRequestOptions.isBinary());
    }

    /**
     * Performs a HEAD request for the specified URL and options.
     *
     * @param url the URL
     * @param options the options
     * @return the response as JSON
     * @throws IOException In case an I/O exception occurs
     */
    public static final String head(String url, String options) throws IOException {
        HttpClientRequestOptions httpClientRequestOptions = parseOptions(options);
        HttpHead httpHead = createHeadRequest(url, httpClientRequestOptions);

        CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(httpClientRequestOptions.isSslTrustAllEnabled());
        CloseableHttpResponse response = httpClient.execute(httpHead);
        return processResponse(response, httpClientRequestOptions.isBinary());
    }

    /**
     * Performs a TRACE request for the specified URL and options.
     *
     * @param url the URL
     * @param options the options
     * @return the response as JSON
     * @throws IOException In case an I/O exception occurs
     */
    public static final String trace(String url, String options) throws IOException {
        HttpClientRequestOptions httpClientRequestOptions = parseOptions(options);
        HttpTrace httpTrace = createTraceRequest(url, httpClientRequestOptions);

        CloseableHttpClient httpClient = HttpClientProxyUtils.getHttpClient(httpClientRequestOptions.isSslTrustAllEnabled());
        CloseableHttpResponse response = httpClient.execute(httpTrace);
        return processResponse(response, httpClientRequestOptions.isBinary());
    }

    /**
     * Prepare headers.
     *
     * @param httpClientRequestOptions the http client request options
     * @param httpRequestBase the http request base
     */
    private static void prepareHeaders(HttpClientRequestOptions httpClientRequestOptions, HttpUriRequestBase httpRequestBase) {
        for (HttpClientHeader httpClientHeader : httpClientRequestOptions.getHeaders()) {
            httpRequestBase.setHeader(httpClientHeader.getName(), httpClientHeader.getValue());
        }
    }

    /** The Constant recognizedTextMimeTypes. */
    private static final HashSet<String> recognizedTextMimeTypes = new HashSet<>(Arrays.asList("application/json; charset=utf-8",
            "text/json; charset=utf-8", "text/json", "application/CSV", "application/csv", "text/csv", "json",
            ContentType.TEXT_PLAIN.getMimeType(), ContentType.TEXT_HTML.getMimeType(), ContentType.TEXT_XML.getMimeType(),
            ContentType.APPLICATION_JSON.getMimeType(), ContentType.APPLICATION_ATOM_XML.getMimeType(),
            ContentType.APPLICATION_XML.getMimeType(), ContentType.APPLICATION_XHTML_XML.getMimeType()));

    /**
     * Process http client response.
     *
     * @param response the response
     * @param binary the binary
     * @return the http client response
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static HttpClientResponse processHttpClientResponse(CloseableHttpResponse response, boolean binary) throws IOException {
        try {
            HttpClientResponse httpClientResponse = new HttpClientResponse();
            httpClientResponse.setStatusCode(response.getCode());
            httpClientResponse.setStatusMessage(response.getReasonPhrase());
            httpClientResponse.setProtocol(response.getVersion()
                                                   .getProtocol());
            httpClientResponse.setProtocol(response.getVersion()
                                                   .getProtocol());
            HttpEntity entity = response.getEntity();
            if (entity != null && entity.getContent() != null) {
                byte[] content = IOUtils.toByteArray(entity.getContent());
                String processedContentType = contentTypeOf(entity).getMimeType();
                boolean isSupportedTextType = recognizedTextMimeTypes.contains(processedContentType);
                if (!isSupportedTextType) {
                    Optional<String> recognizedTextMimeType = recognizedTextMimeTypes.stream()
                                                                                     .filter(e -> processedContentType.contains(e))
                                                                                     .findFirst();
                    isSupportedTextType = recognizedTextMimeType.isPresent();
                }

                if (!binary && isSupportedTextType) {
                    Charset charset = contentTypeOf(entity).getCharset();
                    String text = new String(content, charset != null ? charset : StandardCharsets.UTF_8);
                    httpClientResponse.setText(text);
                } else {
                    httpClientResponse.setData(content);
                }
            }

            for (Header header : response.getHeaders()) {
                httpClientResponse.getHeaders()
                                  .add(new HttpClientHeader(header.getName(), header.getValue()));
            }
            EntityUtils.consume(entity);
            return httpClientResponse;
        } finally {
            response.close();
        }
    }

    /**
     * The entity's content type, or the default text one when it declares none - the HttpClient 5
     * replacement for 4.x's ContentType.getOrDefault(HttpEntity). In 5.x an entity's content type is a
     * String, so it is parsed leniently: a malformed header yields no content type rather than an
     * exception, and the caller then falls back to the same default 4.x used.
     *
     * @param entity the entity
     * @return the content type, never null
     */
    private static ContentType contentTypeOf(HttpEntity entity) {
        String declared = entity.getContentType();
        ContentType parsed = declared == null ? null : ContentType.parseLenient(declared);
        return parsed != null ? parsed : ContentType.DEFAULT_TEXT;
    }

    /**
     * Process response.
     *
     * @param response the response
     * @param binary the binary
     * @return the string
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static String processResponse(CloseableHttpResponse response, boolean binary) throws IOException {
        HttpClientResponse httpClientResponse = processHttpClientResponse(response, binary);
        return GsonHelper.toJson(httpClientResponse);
    }

    /**
     * Parse HTTP Request Options. Every caller dereferences the result, so a missing options document
     * yields the defaults rather than null - the client Java SDK's no-options overloads pass none.
     *
     * @param options the options, may be null or blank
     * @return the http client request options, never null
     */
    public static HttpClientRequestOptions parseOptions(String options) {
        if (options == null || options.isBlank()) {
            return new HttpClientRequestOptions();
        }
        return GsonHelper.fromJson(options, HttpClientRequestOptions.class);
    }

    /**
     * Prepare HTTP Request Configurations.
     *
     * @param httpClientRequestOptions the http client request options
     * @return the request config
     */
    public static RequestConfig prepareConfig(String httpClientRequestOptions) {
        return prepareConfig(parseOptions(httpClientRequestOptions));
    }

    /**
     * Prepare HTTP Request Configurations.
     *
     * @param httpClientRequestOptions the http client request options
     * @return the request config
     */
    public static RequestConfig prepareConfig(HttpClientRequestOptions httpClientRequestOptions) {
        // relativeRedirectsAllowed is deliberately not set: HttpClient 5 dropped the option and always
        // allows a relative redirect, so there is nothing to map it to. The option stays on
        // HttpClientRequestOptions so an existing options document still parses - it simply has no
        // effect, which is also what HttpClient 5 does with it.
        RequestConfig.Builder configBuilder = RequestConfig.custom();
        configBuilder.setAuthenticationEnabled(httpClientRequestOptions.isAuthenticationEnabled())
                     .setCircularRedirectsAllowed(httpClientRequestOptions.isCircularRedirectsAllowed())
                     .setContentCompressionEnabled(httpClientRequestOptions.isContentCompressionEnabled())
                     .setExpectContinueEnabled(httpClientRequestOptions.isExpectContinueEnabled())
                     .setRedirectsEnabled(httpClientRequestOptions.isRedirectsEnabled())
                     .setMaxRedirects(httpClientRequestOptions.getMaxRedirects())
                     // HttpClient 5 takes Timeout objects rather than bare milliseconds, and has no
                     // socket timeout on the request config: the 4.x socketTimeout (how long to wait
                     // for response data) is responseTimeout here, which is what it meant.
                     .setConnectionRequestTimeout(Timeout.ofMilliseconds(httpClientRequestOptions.getConnectionRequestTimeout()))
                     .setConnectTimeout(Timeout.ofMilliseconds(httpClientRequestOptions.getConnectTimeout()))
                     .setResponseTimeout(Timeout.ofMilliseconds(httpClientRequestOptions.getSocketTimeout()))
                     .setCookieSpec(httpClientRequestOptions.getCookieSpec())
                     .setProxyPreferredAuthSchemes(httpClientRequestOptions.getProxyPreferredAuthSchemes())
                     .setTargetPreferredAuthSchemes(httpClientRequestOptions.getTargetPreferredAuthSchemes());

        if ((httpClientRequestOptions.getProxyHost() != null) && (httpClientRequestOptions.getProxyPort() != 0)) {
            configBuilder.setProxy(new HttpHost(httpClientRequestOptions.getProxyHost(), httpClientRequestOptions.getProxyPort()));
        }

        RequestConfig config = configBuilder.build();
        return config;
    }

    /**
     * Build HTTP GET Request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http get
     */
    public static HttpGet createGetRequest(String url, HttpClientRequestOptions httpClientRequestOptions) {
        HttpGet httpGet = new HttpGet(url);
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        httpGet.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpGet);
        return httpGet;
    }

    /**
     * Build HTTP POST Request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http post
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static final HttpPost createPostRequest(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {
        if (httpClientRequestOptions.getData() != null) {
            return createPostBinaryRequest(url, httpClientRequestOptions);
        } else if (httpClientRequestOptions.getText() != null) {
            return createPostTextRequest(url, httpClientRequestOptions);
        } else if (httpClientRequestOptions.getParams() != null && httpClientRequestOptions.getParams()
                                                                                           .size() > 0) {
            return createPostFormRequest(url, httpClientRequestOptions);
        } else if (httpClientRequestOptions.getFiles() != null && httpClientRequestOptions.getFiles()
                                                                                          .size() > 0) {
            return createPostFilesRequest(url, httpClientRequestOptions);
        }
        throw new IllegalArgumentException(
                "The element [data] or [text] or [params] or [files] in [options] have to be set for POST requests");
    }

    /**
     * Creates the post binary request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http post
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static final HttpPost createPostBinaryRequest(String url, HttpClientRequestOptions httpClientRequestOptions)
            throws IOException {
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpPost);
        HttpEntity entity = EntityBuilder.create()
                                         .setBinary(httpClientRequestOptions.getData())
                                         .setContentType(ContentType.APPLICATION_OCTET_STREAM)
                                         .build();
        httpPost.setEntity(entity);
        return httpPost;
    }

    /**
     * Creates the post text request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http post
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static final HttpPost createPostTextRequest(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {
        if (httpClientRequestOptions.getText() == null) {
            throw new IllegalArgumentException("The element [text] in [options] cannot be null for POST requests in [text] mode");
        }
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpPost);

        String contentTypeString = httpClientRequestOptions.getContentType();
        boolean shouldParseContentType = contentTypeString.contains("charset") || contentTypeString.contains(";");
        ContentType contentType = shouldParseContentType ? ContentType.parse(contentTypeString) : ContentType.create(contentTypeString);

        EntityBuilder entityBuilder = EntityBuilder.create()
                                                   .setBinary(httpClientRequestOptions.getText()
                                                                                      .getBytes(StandardCharsets.UTF_8))
                                                   .setContentType(contentType);
        if (httpClientRequestOptions.isCharacterEncodingEnabled()) {
            entityBuilder.setContentEncoding(httpClientRequestOptions.getCharacterEncoding());
        }
        httpPost.setEntity(entityBuilder.build());
        return httpPost;
    }

    /**
     * Creates the post form request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http post
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static final HttpPost createPostFormRequest(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {
        if (httpClientRequestOptions.getParams() == null) {
            throw new IllegalArgumentException("The element [params] in [options] cannot be null for POST requests in [form] mode");
        }
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpPost);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for (HttpClientParam httpClientParam : httpClientRequestOptions.getParams()) {
            params.add(new BasicNameValuePair(httpClientParam.getName(), httpClientParam.getValue()));
        }
        HttpEntity entity = new UrlEncodedFormEntity(params);
        httpPost.setEntity(entity);
        return httpPost;
    }

    /**
     * Creates the post files request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http post
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static final HttpPost createPostFilesRequest(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {
        if (httpClientRequestOptions.getFiles() == null) {
            throw new IllegalArgumentException("The element [files] in [options] cannot be null for POST requests in [file] mode");
        }
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpPost);
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        for (HttpClientFile httpFile : httpClientRequestOptions.getFiles()) {
            File file = new File(httpFile.getValue());
            multipartEntityBuilder.addBinaryBody(httpFile.getName(), file, ContentType.APPLICATION_OCTET_STREAM, file.getName());
        }
        HttpEntity entity = multipartEntityBuilder.build();
        httpPost.setEntity(entity);
        return httpPost;
    }

    /**
     * Build HTTP PUT Request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http put
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static final HttpPut createPutRequest(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {
        if (httpClientRequestOptions.getData() != null) {
            return createPutBinaryRequest(url, httpClientRequestOptions);
        } else if (httpClientRequestOptions.getText() != null) {
            return createPutTextRequest(url, httpClientRequestOptions);
        } else if (httpClientRequestOptions.getParams() != null && httpClientRequestOptions.getParams()
                                                                                           .size() > 0) {
            return createPutFormRequest(url, httpClientRequestOptions);
        } else if (httpClientRequestOptions.getFiles() != null && httpClientRequestOptions.getFiles()
                                                                                          .size() > 0) {
            return createPutFilesRequest(url, httpClientRequestOptions);
        }
        throw new IllegalArgumentException(
                "The element [data] or [text] or [params] or [files] in [options] have to be set for PUT requests");
    }

    /**
     * Creates the put binary request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http put
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static final HttpPut createPutBinaryRequest(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        HttpPut httpPut = new HttpPut(url);
        httpPut.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpPut);
        HttpEntity entity = EntityBuilder.create()
                                         .setBinary(httpClientRequestOptions.getData())
                                         .setContentType(ContentType.APPLICATION_OCTET_STREAM)
                                         .build();
        httpPut.setEntity(entity);
        return httpPut;
    }

    /**
     * Creates the put text request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http put
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static final HttpPut createPutTextRequest(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {
        if (httpClientRequestOptions.getText() == null) {
            throw new IllegalArgumentException("The element [text] in [options] cannot be null for POST requests in [text] mode");
        }
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        HttpPut httpPut = new HttpPut(url);
        httpPut.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpPut);
        EntityBuilder entityBuilder = EntityBuilder.create()
                                                   .setBinary(httpClientRequestOptions.getText()
                                                                                      .getBytes(StandardCharsets.UTF_8))
                                                   .setContentType(ContentType.create(httpClientRequestOptions.getContentType()));
        if (httpClientRequestOptions.isCharacterEncodingEnabled()) {
            entityBuilder.setContentEncoding(httpClientRequestOptions.getCharacterEncoding());
        }
        httpPut.setEntity(entityBuilder.build());
        return httpPut;
    }

    /**
     * Creates the put form request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http put
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static final HttpPut createPutFormRequest(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {
        if (httpClientRequestOptions.getParams() == null) {
            throw new IllegalArgumentException("The element [params] in [options] cannot be null for POST requests in [form] mode");
        }
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        HttpPut httpPut = new HttpPut(url);
        httpPut.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpPut);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for (HttpClientParam httpClientParam : httpClientRequestOptions.getParams()) {
            params.add(new BasicNameValuePair(httpClientParam.getName(), httpClientParam.getValue()));
        }
        HttpEntity entity = new UrlEncodedFormEntity(params);
        httpPut.setEntity(entity);
        return httpPut;
    }

    /**
     * Creates the put files request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http put
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static final HttpPut createPutFilesRequest(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {
        if (httpClientRequestOptions.getFiles() == null) {
            throw new IllegalArgumentException("The element [files] in [options] cannot be null for POST requests in [file] mode");
        }
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        HttpPut httpPut = new HttpPut(url);
        httpPut.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpPut);
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        for (HttpClientFile httpFile : httpClientRequestOptions.getFiles()) {
            File file = new File(httpFile.getValue());
            multipartEntityBuilder.addBinaryBody(httpFile.getName(), file, ContentType.APPLICATION_OCTET_STREAM, file.getName());
        }
        HttpEntity entity = multipartEntityBuilder.build();
        httpPut.setEntity(entity);
        return httpPut;
    }

    /**
     * Build HTTP PATCH Request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http patch
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static final HttpPatch createPatchRequest(String url, HttpClientRequestOptions httpClientRequestOptions) throws IOException {
        if (httpClientRequestOptions.getData() != null) {
            return createPatchBinaryRequest(url, httpClientRequestOptions);
        } else if (httpClientRequestOptions.getText() != null) {
            return createPatchTextRequest(url, httpClientRequestOptions);
        } else if (httpClientRequestOptions.getParams() != null && httpClientRequestOptions.getParams()
                                                                                           .size() > 0) {
            return createPatchFormRequest(url, httpClientRequestOptions);
        } else if (httpClientRequestOptions.getFiles() != null && httpClientRequestOptions.getFiles()
                                                                                          .size() > 0) {
            return createPatchFilesRequest(url, httpClientRequestOptions);
        }
        throw new IllegalArgumentException(
                "The element [data] or [text] or [params] or [files] in [options] have to be set for PATCH requests");
    }

    /**
     * Creates the patch binary request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http patch
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static final HttpPatch createPatchBinaryRequest(String url, HttpClientRequestOptions httpClientRequestOptions)
            throws IOException {
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        HttpPatch httpPatch = new HttpPatch(url);
        httpPatch.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpPatch);
        HttpEntity entity = EntityBuilder.create()
                                         .setBinary(httpClientRequestOptions.getData())
                                         .setContentType(ContentType.APPLICATION_OCTET_STREAM)
                                         .build();
        httpPatch.setEntity(entity);
        return httpPatch;
    }

    /**
     * Creates the patch text request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http patch
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static final HttpPatch createPatchTextRequest(String url, HttpClientRequestOptions httpClientRequestOptions)
            throws IOException {
        if (httpClientRequestOptions.getText() == null) {
            throw new IllegalArgumentException("The element [text] in [options] cannot be null for POST requests in [text] mode");
        }
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        HttpPatch httpPatch = new HttpPatch(url);
        httpPatch.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpPatch);
        EntityBuilder entityBuilder = EntityBuilder.create()
                                                   .setBinary(httpClientRequestOptions.getText()
                                                                                      .getBytes(StandardCharsets.UTF_8))
                                                   .setContentType(ContentType.create(httpClientRequestOptions.getContentType()));
        if (httpClientRequestOptions.isCharacterEncodingEnabled()) {
            entityBuilder.setContentEncoding(httpClientRequestOptions.getCharacterEncoding());
        }
        httpPatch.setEntity(entityBuilder.build());
        return httpPatch;
    }

    /**
     * Creates the patch form request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http patch
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static final HttpPatch createPatchFormRequest(String url, HttpClientRequestOptions httpClientRequestOptions)
            throws IOException {
        if (httpClientRequestOptions.getParams() == null) {
            throw new IllegalArgumentException("The element [params] in [options] cannot be null for POST requests in [form] mode");
        }
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        HttpPatch httpPatch = new HttpPatch(url);
        httpPatch.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpPatch);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for (HttpClientParam httpClientParam : httpClientRequestOptions.getParams()) {
            params.add(new BasicNameValuePair(httpClientParam.getName(), httpClientParam.getValue()));
        }
        HttpEntity entity = new UrlEncodedFormEntity(params);
        httpPatch.setEntity(entity);
        return httpPatch;
    }

    /**
     * Creates the patch files request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http patch
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static final HttpPatch createPatchFilesRequest(String url, HttpClientRequestOptions httpClientRequestOptions)
            throws IOException {
        if (httpClientRequestOptions.getFiles() == null) {
            throw new IllegalArgumentException("The element [files] in [options] cannot be null for POST requests in [file] mode");
        }
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        HttpPatch httpPatch = new HttpPatch(url);
        httpPatch.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpPatch);
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        for (HttpClientFile httpFile : httpClientRequestOptions.getFiles()) {
            File file = new File(httpFile.getValue());
            multipartEntityBuilder.addBinaryBody(httpFile.getName(), file, ContentType.APPLICATION_OCTET_STREAM, file.getName());
        }
        HttpEntity entity = multipartEntityBuilder.build();
        httpPatch.setEntity(entity);
        return httpPatch;
    }

    /**
     * Build HTTP DELETE Request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http delete
     */
    public static HttpDelete createDeleteRequest(String url, HttpClientRequestOptions httpClientRequestOptions) {
        HttpDelete httpDelete = new HttpDelete(url);
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        httpDelete.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpDelete);
        return httpDelete;
    }

    /**
     * Build HTTP HEAD Request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http head
     */
    public static HttpHead createHeadRequest(String url, HttpClientRequestOptions httpClientRequestOptions) {
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        HttpHead httpHead = new HttpHead(url);
        httpHead.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpHead);
        return httpHead;
    }

    /**
     * Build HTTP TRACE Request.
     *
     * @param url the url
     * @param httpClientRequestOptions the http client request options
     * @return the http trace
     */
    public static HttpTrace createTraceRequest(String url, HttpClientRequestOptions httpClientRequestOptions) {
        RequestConfig config = prepareConfig(httpClientRequestOptions);
        HttpTrace httpTrace = new HttpTrace(url);
        httpTrace.setConfig(config);
        prepareHeaders(httpClientRequestOptions, httpTrace);
        return httpTrace;
    }
}
