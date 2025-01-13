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
 * The Class MetadataODataNorthwindTest.
 */
public class MetadataODataNorthwindTest extends AbstractODataNorthwindTest {

    /**
     * HTTP GET: https://services.odata.org/V2/Northwind/Northwind.svc/$metadata
     *
     * @throws Exception the exception
     */
    @Test
    public void testMetadataResponse() throws Exception {
        Response response = OData2RequestBuilder.createRequest(sf) //
                                                .segments("$metadata") //
                                                .executeRequest(GET);
        String content = IOUtils.toString((InputStream) response.getEntity());
        content = content.replaceAll("\\n|\\r\\n", System.getProperty("line.separator"));
        String expected = loadExpectedMetadata();
        expected = expected.replaceAll("\\n|\\r\\n", System.getProperty("line.separator"));
        assertEquals(expected, content);
    }
}
