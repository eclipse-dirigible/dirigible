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
import org.eclipse.dirigible.engine.odata2.sql.entities.User;
import org.eclipse.dirigible.engine.odata2.sql.entities.Group;
import org.junit.Test;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;

import static org.apache.olingo.odata2.api.commons.ODataHttpMethod.GET;
import static org.junit.Assert.assertTrue;

/**
 * The Class ODataSQLProcessorManyToManyTest.
 */
public class ODataSQLProcessorManyToManyTest extends AbstractSQLProcessorTest {

    /**
     * Gets the o data entities.
     *
     * @return the o data entities
     */
    @Override
    protected Class<?>[] getODataEntities() {
        return new Class<?>[] {User.class, Group.class};
    }

    /**
     * Test SQL processor with mapping table.
     *
     * @throws Exception the exception
     */
    @Test
    public void testSQLProcessorWithMappingTable() throws Exception {
        String UUID = "ec20bbaf-ee7a-4405-91d0-7ad8be889270";
        Response response = OData2RequestBuilder.createRequest(sf) //
                                                .segments("Users('" + UUID + "')", "Groups") //
                                                .accept("application/atom+xml")
                                                .executeRequest(GET);
        String content = IOUtils.toString((InputStream) response.getEntity());
        assertTrue(content.contains("Mid"));
    }
}
