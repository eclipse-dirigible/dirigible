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
import static org.junit.Assert.assertEquals;

import java.io.InputStream;

import jakarta.ws.rs.core.Response;

import org.apache.cxf.helpers.IOUtils;
import org.junit.Test;

/**
 * The Class CategoriesODataNorthwindTest.
 */
public class CategoriesODataNorthwindTest extends AbstractODataNorthwindTest {

    /**
     * HTTP GET: https://services.odata.org/V2/Northwind/Northwind.svc/Categories/$count
     *
     * @throws Exception the exception
     */
    @Test
    public void testCount() throws Exception {
        Response response = OData2RequestBuilder.createRequest(sf) //
                                                .segments("Categories") //
                                                .segments("$count") //
                                                .executeRequest(GET);
        int count = Integer.valueOf(IOUtils.toString((InputStream) response.getEntity()));
        int expectedCount = 8;
        assertEquals(expectedCount, count);
    }

    /**
     * HTTP GET: https://services.odata.org/V2/Northwind/Northwind.svc/Categories?$format=json
     *
     * @throws Exception the exception
     */
    @Test
    public void testGet() throws Exception {
        Response response = OData2RequestBuilder.createRequest(sf) //
                                                .segments("Categories") //
                                                .param("$format", "json") //
                                                .executeRequest(GET);
        String data = IOUtils.toString((InputStream) response.getEntity());
        String expectedData = loadExpectedData("Categories-get.json");
        assertEquals(expectedData, data);
    }

    /**
     * HTTP GET: https://services.odata.org/V2/Northwind/Northwind.svc/Categories(1)?$format=json
     *
     * @throws Exception the exception
     */
    @Test
    public void testGetById() throws Exception {
        Response response = OData2RequestBuilder.createRequest(sf) //
                                                .segments("Categories(1)") //
                                                .param("$format", "json") //
                                                .executeRequest(GET);
        String data = IOUtils.toString((InputStream) response.getEntity());
        String expectedData = loadExpectedData("Categories-getById.json");
        assertEquals(expectedData, data);
    }

    /**
     * HTTP GET: https://services.odata.org/V2/Northwind/Northwind.svc/Categories(1)/Products/$count
     *
     * @throws Exception the exception
     */
    @Test
    public void testNavigationProductsCount() throws Exception {
        Response response = OData2RequestBuilder.createRequest(sf) //
                                                .segments("Categories(1)") //
                                                .segments("Products")
                                                .segments("$count") //
                                                .executeRequest(GET);
        int count = Integer.valueOf(IOUtils.toString((InputStream) response.getEntity()));
        int expectedCount = 12;
        assertEquals(expectedCount, count);
    }

    /**
     * HTTP GET:
     * https://services.odata.org/V2/Northwind/Northwind.svc/Categories(1)/Products?$format=json
     *
     * @throws Exception the exception
     */
    @Test
    public void testNavigationProductsGet() throws Exception {
        Response response = OData2RequestBuilder.createRequest(sf) //
                                                .segments("Categories(1)") //
                                                .segments("Products") //
                                                .param("$format", "json") //
                                                .executeRequest(GET);
        String data = IOUtils.toString((InputStream) response.getEntity());
        String expectedData = loadExpectedData("Categories-navigationProductsGet.json");
        assertEquals(expectedData, data);
    }
}
