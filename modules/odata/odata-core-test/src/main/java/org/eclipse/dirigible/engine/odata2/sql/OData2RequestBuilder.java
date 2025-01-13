/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql;

import static org.apache.olingo.odata2.api.commons.ODataHttpMethod.GET;
import static org.apache.olingo.odata2.api.commons.ODataHttpMethod.POST;
import static org.apache.olingo.odata2.api.commons.ODataHttpMethod.PUT;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.PathSegmentImpl;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.olingo.odata2.api.ODataServiceFactory;
import org.apache.olingo.odata2.api.client.batch.BatchPart;
import org.apache.olingo.odata2.api.commons.ODataHttpMethod;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataRequest;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.core.ODataContextImpl;
import org.apache.olingo.odata2.core.ODataPathSegmentImpl;
import org.apache.olingo.odata2.core.PathInfoImpl;
import org.apache.olingo.odata2.core.batch.BatchHandlerImpl;
import org.apache.olingo.odata2.core.processor.ODataSingleProcessorService;
import org.apache.olingo.odata2.core.rest.ODataSubLocator;
import org.apache.olingo.odata2.core.rest.SubLocatorParameter;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;
import org.eclipse.dirigible.engine.odata2.sql.processor.DefaultSQLProcessor;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;


/**
 * Base class for OData API tests, which can be used to simulate calls to the OData API without
 * having to use a servlet.
 *
 * In order to use the class, inherit from it and overwrite the
 * {@link OData2RequestBuilder#executeRequest(ODataHttpMethod)} method. You can use this method also
 * to provide mock data to your API. If you API retrieves handles to data sources via the servlet
 * context or the servlet request, use the
 * {@link OData2RequestBuilder#enrichServletContextMock(ServletContext)} and
 * {@link OData2RequestBuilder#enrichServletRequestMock(ServletRequest)} methods to provide the
 * handles to you mock data.
 *
 */
public class OData2RequestBuilder {

    /** The Constant PROTOCOL. */
    private static final String PROTOCOL = "http";

    /** The Constant HOST. */
    private static final String HOST = "localhost";

    /** The Constant PORT. */
    private static final int PORT = 8080;

    /** The Constant RELATIVE_SERVICE_ROOT. */
    private static final String RELATIVE_SERVICE_ROOT = "/api/v1";

    /** The path segment string list. */
    //
    private final List<String> pathSegmentStringList = new ArrayList<>();

    /** The query params. */
    private final MultivaluedMap<String, String> queryParams = new MetadataMap<>();

    /** The accept. */
    private String accept = "*/*"; // Default value; Maybe overruled by constructor

    /** The content size. */
    private int contentSize = 1024 * 4;

    /** The service factory. */
    private ODataServiceFactory serviceFactory;

    /**
     * Segments.
     *
     * @param pathSegmentStrings the path
     * @return OData2RequestBuilder
     */
    public OData2RequestBuilder segments(final String... pathSegmentStrings) {
        pathSegmentStringList.addAll(Arrays.asList(pathSegmentStrings));
        return this;
    }

    /**
     * Param.
     *
     * @param name the param name
     * @param value the param value
     * @return OData2RequestBuilder
     */
    public OData2RequestBuilder param(final String name, final String value) {
        queryParams.add(name, value);
        return this;
    }

    /**
     * Accept.
     *
     * @param accept the accept header
     * @return OData2RequestBuilder
     */
    public OData2RequestBuilder accept(final String accept) {
        this.accept = accept;
        return this;
    }

    /**
     * Content size.
     *
     * @param contentSize the contentSize header
     * @return OData2RequestBuilder
     */
    public OData2RequestBuilder contentSize(final int contentSize) {
        this.contentSize = contentSize;
        return this;
    }

    /**
     * Execute request.
     *
     * @return executeRequest
     * @throws IOException in case of error
     * @throws ODataException in case of error
     */
    public Response executeRequest() throws IOException, ODataException {
        return executeRequest(GET);
    }

    /**
     * Service factory.
     *
     * @param serviceFactory serviceFactory
     * @return OData2RequestBuilder
     */
    public OData2RequestBuilder serviceFactory(ODataServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
        return this;
    }

    /**
     * Creates the request.
     *
     * @param sf ODataServiceFactory
     * @return OData2RequestBuilder
     */
    public static OData2RequestBuilder createRequest(ODataServiceFactory sf) {
        final OData2RequestBuilder request = new OData2RequestBuilder();
        return request.serviceFactory(sf);
    }

    /**
     * This methods executes an OData Request based on applied parameters and mocked REST layer.
     *
     * @param method Mandatory parameter defining Http Method to be used for the request. Expected
     *        values are: GET, PUT, POST, DELETE, PATCH, MERGE
     * @return OData Response
     * @throws IOException in case of error
     * @throws ODataException in case of error
     */
    public Response executeRequest(final ODataHttpMethod method) throws IOException, ODataException {

        final EasyMockSupport easyMockSupport = new EasyMockSupport();

        final SubLocatorParameter subLocatorParam = new SubLocatorParameter();
        // Instantiate ServiceFactory
        subLocatorParam.setServiceFactory(serviceFactory);
        // Create PathSegments
        subLocatorParam.setPathSegments(createPathSegments(pathSegmentStringList));
        // Mock HttpHeaders
        final HttpHeaders httpHeaders = easyMockSupport.createMock(HttpHeaders.class);
        expect(httpHeaders.getAcceptableLanguages()).andReturn(Collections.singletonList(Locale.US));
        final MultivaluedMap<String, String> headersMap = new MetadataMap<>();
        headersMap.add("Accept", accept);
        headersMap.addAll("accept-encoding", Arrays.asList("gzip", "deflate", "sdch"));
        headersMap.add("accept-language", "en-US,en;q=0.8");
        headersMap.add("Authorization", "Basic dG9tY2F0OnRvbWNhdA==");
        headersMap.add("cache-control", "no-cache");
        headersMap.add("connection", "keep-alive");
        headersMap.add("Content-Type", "application/json");
        headersMap.add("host", HOST + "" + PORT);
        headersMap.add("user-agent", "Mozilla/5.0");
        expect(httpHeaders.getRequestHeaders()).andReturn(headersMap);
        final Capture<String> nameCapture = Capture.newInstance(CaptureType.LAST);
        //
        final IAnswer<List<String>> getRequestHeaderAnswer = () -> headersMap.get(nameCapture.getValue());
        //
        expect(httpHeaders.getRequestHeader(capture(nameCapture))).andAnswer(getRequestHeaderAnswer)
                                                                  .anyTimes();
        subLocatorParam.setHttpHeaders(httpHeaders);

        // Mock ServletContext
        ServletContext servletContext = easyMockSupport.createMock(ServletContext.class);
        enrichServletContextMock(servletContext);

        // Mock HttpServletRequest
        final HttpServletRequest servletRequest = easyMockSupport.createMock(HttpServletRequest.class);

        expect(servletRequest.getMethod()).andReturn(method.name());
        expect(servletRequest.getServerName()).andReturn(HOST);
        expect(servletRequest.getServerPort()).andReturn(PORT);
        expect(servletRequest.getScheme()).andReturn(PROTOCOL);
        expect(servletRequest.getRemoteUser()).andReturn("RemoteUser")
                                              .anyTimes();
        // needed for tests in it-op
        expect(servletRequest.getHeader("x-forwarded-for")).andReturn("127.0.0.1")
                                                           .anyTimes();
        StringBuilder pathSegmentsString = new StringBuilder();
        for (String pathSegment : pathSegmentStringList) {
            pathSegmentsString.append('/')
                              .append(pathSegment);
        }
        expect(servletRequest.getRequestURL()).andReturn(new StringBuffer(absoluteServiceRoot() + pathSegmentsString));
        expect(servletRequest.getRequestURI()).andReturn(RELATIVE_SERVICE_ROOT + pathSegmentsString)
                                              .anyTimes();
        expect(servletRequest.getQueryString()).andReturn(createQueryString(queryParams));
        getServletInputStream(method, easyMockSupport, servletRequest);
        expect(servletRequest.getServletContext()).andReturn(servletContext)
                                                  .anyTimes();
        enrichServletRequestMock(servletRequest);
        subLocatorParam.setServletRequest(servletRequest);

        // Mock UriInfo
        final UriInfo uriInfo = easyMockSupport.createMock(UriInfo.class);
        expect(uriInfo.getBaseUri()).andReturn(URI.create(absoluteServiceRoot()));
        expect(uriInfo.getQueryParameters()).andReturn(queryParams);
        subLocatorParam.setUriInfo(uriInfo);

        easyMockSupport.replayAll();

        final ODataSubLocator subLocator = ODataSubLocator.create(subLocatorParam);
        Response response = null;

        switch (method) {
            case GET:
                response = subLocator.handleGet();
                break;
            case PUT:
                response = subLocator.handlePut();
                break;
            case POST:
                // Note: As of now this class does not cover x-http-method. If needed handlePost needs
                // to be invoked by the proper value for @HeaderParam("X-HTTP-Method")
                response = subLocator.handlePost(null, null);
                break;
            case DELETE:
                response = subLocator.handleDelete();
                break;
            case PATCH:
                response = subLocator.handlePatch();
                break;
            case MERGE:
                response = subLocator.handleMerge();
                break;
        }

        easyMockSupport.verifyAll();
        return response;
    }



    /**
     * Execute batch request.
     *
     * @param batch the batch
     * @return the o data response
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws URISyntaxException the URI syntax exception
     * @throws ODataException the o data exception
     */
    public ODataResponse executeBatchRequest(List<BatchPart> batch) throws IOException, URISyntaxException, ODataException {
        InputStream body = EntityProvider.writeBatchRequest(batch, "batch_1");
        String batchRequestBody = IOUtils.toString(body);

        PathInfoImpl pathInfo = new PathInfoImpl();
        pathInfo.setServiceRoot(new URI("https://localhost:8080/odata/v2"));
        pathInfo.setODataPathSegment(Collections.singletonList(new ODataPathSegmentImpl("$batch", null)));
        ODataRequest batchRequest = ODataRequest.method(POST)
                                                .contentType("application/json")
                                                .pathInfo(pathInfo)
                                                .acceptableLanguages(Collections.singletonList(Locale.ENGLISH))
                                                .build();

        ODataContext testContext = new ODataContextImpl(batchRequest, serviceFactory);

        ODataSingleProcessorService service = (ODataSingleProcessorService) serviceFactory.createService(testContext);
        DefaultSQLProcessor proc = (DefaultSQLProcessor) service.getProcessor();
        proc.setContext(testContext);
        BatchHandlerImpl handler = new BatchHandlerImpl(serviceFactory, service);

        return proc.executeBatch(handler, "multipart/mixed; boundary=batch_1",
                new ByteArrayInputStream(batchRequestBody.getBytes(StandardCharsets.UTF_8)));
    }


    /**
     * Gets the servlet input stream.
     *
     * @param method the method
     * @param easyMockSupport the easyMockSupport
     * @param servletRequest the servletRequest
     * @throws IOException in case of error
     */
    protected void getServletInputStream(final ODataHttpMethod method, final EasyMockSupport easyMockSupport,
            final HttpServletRequest servletRequest) throws IOException {
        @SuppressWarnings("resource")
        final ServletInputStream contentInputStream = easyMockSupport.createMock(ServletInputStream.class); // NOSONAR mock doesn't have to
                                                                                                            // be closed
        if (method.equals(POST) || method.equals(PUT)) {
            expect(contentInputStream.available()).andReturn(0)
                                                  .anyTimes();
            if (contentSize > 0) {
                expect(contentInputStream.read(EasyMock.anyObject())).andReturn(contentSize)
                                                                     .times(1)
                                                                     .andReturn(-1)
                                                                     .times(1)
                                                                     .andReturn(0)
                                                                     .anyTimes();
            } else {
                expect(contentInputStream.read(EasyMock.anyObject())).andReturn(contentSize)
                                                                     .times(1);
            }
        }
        expect(servletRequest.getInputStream()).andReturn(contentInputStream)
                                               .atLeastOnce();
    }

    /**
     * Content.
     *
     * @param content the content
     * @return OData2RequestBuilder
     */
    public OData2RequestBuilder content(@SuppressWarnings("unused") final String content) { // NOSONAR to be overridden by subclasses
        return this;
    }

    /**
     * Callback to add entries to the servletContext needed for the respective test context. EasyMock is
     * used for the tests, so you can add data by using {@link EasyMock#expect(Object)} in the passed
     * {@link ServletContext} reference, for instance.
     *
     * @param servletContext the EasyMock instance of the {@link ServletContext}.
     */
    protected void enrichServletContextMock(final ServletContext servletContext) {
        // default implementation is empty
    }

    /**
     * Callback to add entries to the servletRequest needed for the test request. EasyMock is used for
     * the tests, so you can add data by using {@link EasyMock#expect(Object)} in the passed
     * {@link ServletContext} reference, for instance.
     *
     * @param servletRequest the EasyMock instance of the {@link ServletRequest}.
     *
     */
    protected void enrichServletRequestMock(final ServletRequest servletRequest) {
        // default implementation is empty
    }

    /**
     * Creates the query string.
     *
     * @param queryParams the query params
     * @return the string
     */
    private String createQueryString(final MultivaluedMap<String, String> queryParams) {
        StringBuilder queryString = new StringBuilder();
        for (Entry<String, List<String>> entry : queryParams.entrySet()) {
            for (String paramValue : entry.getValue()) {
                if (queryString.length() > 0) {
                    queryString.append('&');
                }
                queryString.append(entry.getKey())
                           .append('=')
                           .append(paramValue);
            }
        }
        return HttpUtils.pathEncode(queryString.toString());
    }

    /**
     * Creates the path segments.
     *
     * @param segmentStrings the segment strings
     * @return the list
     */
    private List<PathSegment> createPathSegments(final Collection<String> segmentStrings) {
        final ArrayList<PathSegment> pathSegments = new ArrayList<>();
        if (segmentStrings != null) {
            for (String segmentString : segmentStrings) {
                pathSegments.add(new PathSegmentImpl(segmentString));
            }
        }
        return pathSegments;
    }

    /**
     * Absolute service root.
     *
     * @return the string
     */
    private String absoluteServiceRoot() {
        return PROTOCOL + "://" + HOST + ":" + PORT + RELATIVE_SERVICE_ROOT;
    }

}
