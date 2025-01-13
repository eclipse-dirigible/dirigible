/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.eclipse.dirigible.components.api.http.client.HttpClientRequestOptions;
import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class HttpClientAsyncFacade.
 */
@Component
public final class HttpClientAsyncFacade {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(HttpClientAsyncFacade.class);

    /** The default engine executor. */
    private JavascriptService defaultEngineExecutor = null;

    /** The requests counter. */
    private int requestsCounter = 0;

    /** The async http requests. */
    private List<AsyncHttpRequest> asyncHttpRequests = new ArrayList<AsyncHttpRequest>();

    /** The count down latch. */
    private CountDownLatch countDownLatch;

    @Autowired
    public HttpClientAsyncFacade(JavascriptService defaultEngineExecutor) {
        this.defaultEngineExecutor = defaultEngineExecutor;
    }

    /**
     * Gets the default engine executor.
     *
     * @return the default engine executor
     */
    protected JavascriptService getDefaultEngineExecutor() {
        return defaultEngineExecutor;
    }

    /**
     * Create HttpResponseCallback.
     *
     * @param completeCallback the complete callback
     * @return HttpResponseCallback
     */
    public HttpResponseCallback createCallback(String completeCallback) {
        return new HttpResponseCallback(completeCallback, null, null);
    }

    /**
     * Create HttpResponseCallback.
     *
     * @param completeCallback the complete callback
     * @param failCallback the fail callback
     * @return HttpResponseCallback
     */
    public HttpResponseCallback createCallback(String completeCallback, String failCallback) {
        return new HttpResponseCallback(completeCallback, failCallback, null);
    }

    /**
     * Create HttpResponseCallback.
     *
     * @param completeCallback the complete callback
     * @param failCallback the fail callback
     * @param cancelCallback the cancel callback
     * @return HttpResponseCallback
     */
    public HttpResponseCallback createCallback(String completeCallback, String failCallback, String cancelCallback) {
        return new HttpResponseCallback(completeCallback, failCallback, cancelCallback);
    }

    /**
     * Performs a Async GET request for the specified URL and options.
     *
     * @param url the URL
     * @param options the options
     * @param httpResponseCallback the callback
     */
    public void getAsync(String url, String options, HttpResponseCallback httpResponseCallback) {
        requestsCounter++;
        HttpClientRequestOptions httpClientRequestOptions = HttpClientFacade.parseOptions(options);
        httpResponseCallback.setOptions(httpClientRequestOptions);
        HttpGet request = HttpClientFacade.createGetRequest(url, httpClientRequestOptions);
        asyncHttpRequests.add(new AsyncHttpRequest(request, httpResponseCallback.getCallback()));
    }

    /**
     * Performs a Async POST request for the specified URL and options.
     *
     * @param url the URL
     * @param options the options
     * @param httpResponseCallback the callback
     * @throws IOException In case an I/O exception occurs
     */
    public void postAsync(String url, String options, HttpResponseCallback httpResponseCallback) throws IOException {
        requestsCounter++;
        HttpClientRequestOptions httpClientRequestOptions = HttpClientFacade.parseOptions(options);
        httpResponseCallback.setOptions(httpClientRequestOptions);
        HttpPost request = HttpClientFacade.createPostRequest(url, httpClientRequestOptions);
        asyncHttpRequests.add(new AsyncHttpRequest(request, httpResponseCallback.getCallback()));
    }

    /**
     * Performs a Async PUT request for the specified URL and options.
     *
     * @param url the URL
     * @param options the options
     * @param httpResponseCallback the callback
     * @throws IOException In case an I/O exception occurs
     */
    public void putAsync(String url, String options, HttpResponseCallback httpResponseCallback) throws IOException {
        requestsCounter++;
        HttpClientRequestOptions httpClientRequestOptions = HttpClientFacade.parseOptions(options);
        httpResponseCallback.setOptions(httpClientRequestOptions);
        HttpPut request = HttpClientFacade.createPutRequest(url, httpClientRequestOptions);
        asyncHttpRequests.add(new AsyncHttpRequest(request, httpResponseCallback.getCallback()));
    }

    /**
     * Performs a Async PATCH request for the specified URL and options.
     *
     * @param url the URL
     * @param options the options
     * @param httpResponseCallback the callback
     * @throws IOException In case an I/O exception occurs
     */
    public void patchAsync(String url, String options, HttpResponseCallback httpResponseCallback) throws IOException {
        requestsCounter++;
        HttpClientRequestOptions httpClientRequestOptions = HttpClientFacade.parseOptions(options);
        httpResponseCallback.setOptions(httpClientRequestOptions);
        HttpPatch request = HttpClientFacade.createPatchRequest(url, httpClientRequestOptions);
        asyncHttpRequests.add(new AsyncHttpRequest(request, httpResponseCallback.getCallback()));
    }

    /**
     * Performs a Async DELETE request for the specified URL and options.
     *
     * @param url the URL
     * @param options the options
     * @param httpResponseCallback the callback
     */
    public void deleteAsync(String url, String options, HttpResponseCallback httpResponseCallback) {
        requestsCounter++;
        HttpClientRequestOptions httpClientRequestOptions = HttpClientFacade.parseOptions(options);
        httpResponseCallback.setOptions(httpClientRequestOptions);
        HttpDelete request = HttpClientFacade.createDeleteRequest(url, httpClientRequestOptions);
        asyncHttpRequests.add(new AsyncHttpRequest(request, httpResponseCallback.getCallback()));
    }

    /**
     * Performs a Async HEAD request for the specified URL and options.
     *
     * @param url the URL
     * @param options the options
     * @param httpResponseCallback the callback
     */
    public void headAsync(String url, String options, HttpResponseCallback httpResponseCallback) {
        requestsCounter++;
        HttpClientRequestOptions httpClientRequestOptions = HttpClientFacade.parseOptions(options);
        httpResponseCallback.setOptions(httpClientRequestOptions);
        HttpHead request = HttpClientFacade.createHeadRequest(url, httpClientRequestOptions);
        asyncHttpRequests.add(new AsyncHttpRequest(request, httpResponseCallback.getCallback()));
    }

    /**
     * Performs a Async TRACE request for the specified URL and options.
     *
     * @param url the URL
     * @param options the options
     * @param httpResponseCallback the callback
     */
    public void traceAsync(String url, String options, HttpResponseCallback httpResponseCallback) {
        requestsCounter++;
        HttpClientRequestOptions httpClientRequestOptions = HttpClientFacade.parseOptions(options);
        httpResponseCallback.setOptions(httpClientRequestOptions);
        HttpTrace request = HttpClientFacade.createTraceRequest(url, httpClientRequestOptions);
        asyncHttpRequests.add(new AsyncHttpRequest(request, httpResponseCallback.getCallback()));
    }

    /**
     * Execute request asynchronously.
     *
     * @throws InterruptedException in case an concurrency exception occurs
     * @throws IOException in case an I/O exception occurs
     */
    public void execute() throws InterruptedException, IOException {
        countDownLatch = new CountDownLatch(requestsCounter);
        try (CloseableHttpAsyncClient httpClient = HttpAsyncClients.createDefault()) {
            httpClient.start();
            for (AsyncHttpRequest next : asyncHttpRequests) {
                httpClient.execute(next.getRequest(), next.getCallback());
            }
            countDownLatch.await();
        }
    }

    /**
     * The Class AsyncHttpRequest.
     */
    private static class AsyncHttpRequest {

        /** The request. */
        private final HttpUriRequest request;

        /** The callback. */
        private final FutureCallback<HttpResponse> callback;

        /**
         * Instantiates a new async http request.
         *
         * @param request the request
         * @param callback the callback
         */
        private AsyncHttpRequest(HttpUriRequest request, FutureCallback<HttpResponse> callback) {
            this.request = request;
            this.callback = callback;
        }

        /**
         * Gets the request.
         *
         * @return the request
         */
        private HttpUriRequest getRequest() {
            return request;
        }

        /**
         * Gets the callback.
         *
         * @return the callback
         */
        private FutureCallback<HttpResponse> getCallback() {
            return callback;
        }
    }

    /**
     * The callback handler.
     */
    public class HttpResponseCallback {

        /** The callback. */
        private final FutureCallback<HttpResponse> callback;

        /** The http client request options. */
        private HttpClientRequestOptions httpClientRequestOptions;

        /**
         * Instantiates a new http response callback.
         *
         * @param completeCallback the complete callback
         * @param failCallback the fail callback
         * @param cancelCallback the cancel callback
         */
        HttpResponseCallback(String completeCallback, String failCallback, String cancelCallback) {
            this.callback = createFutureCallback(completeCallback, failCallback, cancelCallback);
        }

        /**
         * Sets the options.
         *
         * @param httpClientRequestOptions the new options
         */
        public void setOptions(HttpClientRequestOptions httpClientRequestOptions) {
            this.httpClientRequestOptions = httpClientRequestOptions;

        }

        /**
         * Gets the callback.
         *
         * @return the callback
         */
        public FutureCallback<HttpResponse> getCallback() {
            return callback;
        }

        /**
         * Creates the future callback.
         *
         * @param completeCallback the complete callback
         * @param failCallback the fail callback
         * @param cancelCallback the cancel callback
         * @return the future callback
         */
        private FutureCallback<HttpResponse> createFutureCallback(String completeCallback, String failCallback, String cancelCallback) {
            return new FutureCallback<HttpResponse>() {

                private Map<Object, Object> executionContext = new HashMap<Object, Object>();

                @Override
                public void completed(HttpResponse response) {
                    countDownLatch.countDown();
                    if (completeCallback != null) {
                        executionContext.put("response", response);
                        executionContext.put("httpClientRequestOptions", httpClientRequestOptions);
                        executeCallback(completeCallback, executionContext);
                    }
                }

                @Override
                public void failed(Exception exception) {
                    countDownLatch.countDown();
                    if (failCallback != null) {
                        executionContext.put("exception", exception);
                        executionContext.put("httpClientRequestOptions", httpClientRequestOptions);
                        executeCallback(failCallback, executionContext);
                    }
                }

                @Override
                public void cancelled() {
                    countDownLatch.countDown();
                    if (cancelCallback != null) {
                        executionContext.put("httpClientRequestOptions", httpClientRequestOptions);
                        executeCallback(cancelCallback, executionContext);
                    }
                }

                private void executeCallback(String completeCallback, Map<Object, Object> executionContext) {
                    try {
                        getDefaultEngineExecutor().handleCallback(completeCallback, executionContext);
                    } catch (Exception e) {
                        if (logger.isErrorEnabled()) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            };
        }
    }
}
