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

import org.apache.cxf.helpers.IOUtils;
import org.eclipse.dirigible.engine.odata2.sql.entities.View;
import org.junit.Test;

import jakarta.ws.rs.core.Response;
import java.io.InputStream;

import static org.apache.olingo.odata2.api.commons.ODataHttpMethod.GET;
import static org.junit.Assert.assertTrue;

/**
 * The Class ODataSQLProcessorViewTest.
 */
public class ODataSQLProcessorViewTest extends AbstractSQLProcessorTest {

    /**
     * Gets the o data entities.
     *
     * @return the o data entities
     */
    @Override
    protected Class<?>[] getODataEntities() {
        return new Class<?>[] {View.class};
    }

    /**
     * Test SQL processor with generated id.
     *
     * @throws Exception the exception
     */
    @Test
    public void testSQLProcessorWithGeneratedId() throws Exception {
        Response response = OData2RequestBuilder.createRequest(sf) //
                                                .segments("Views") //
                                                .accept("application/atom+xml")
                                                .executeRequest(GET);
        String content = IOUtils.toString((InputStream) response.getEntity());
        // check the row number
        assertTrue(content.contains("3"));
    }
}
