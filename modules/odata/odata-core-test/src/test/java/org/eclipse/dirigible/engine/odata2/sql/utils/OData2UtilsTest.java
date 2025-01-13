/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.edm.EdmStructuralType;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderException;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataErrorContext;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.uri.KeyPredicate;
import org.apache.olingo.odata2.api.uri.UriInfo;
import org.easymock.EasyMock;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLSelectBuilder;
import org.eclipse.dirigible.engine.odata2.sql.processor.ExpandCallBack;
import org.junit.Test;

/**
 * The Class OData2UtilsTest.
 */
public class OData2UtilsTest {

    /**
     * Test write entry with expand not found.
     *
     * @throws EntityProviderException the entity provider exception
     * @throws ODataException the o data exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testWriteEntryWithExpandNotFound() throws EntityProviderException, ODataException, IOException {
        ODataContext ctx = EasyMock.createMock(ODataContext.class);
        UriInfo uriInfo = EasyMock.createMock(UriInfo.class);
        EasyMock.replay(ctx, uriInfo);
        String contentType = "application/json";

        // null written to response => valid error document has to be returned as response
        ODataResponse response = ExpandCallBack.writeEntryWithExpand(ctx, uriInfo, (Map<String, Object>) null, contentType);
        assertNotNull(response);

        // try to retrieve the error document from the response
        ODataErrorContext errorResponse = null;
        try (InputStream content = response.getEntityAsStream()) {
            errorResponse = EntityProvider.readErrorDocument(content, contentType);
        }

        assertEquals(contentType, errorResponse.getContentType());
        assertEquals("No content", errorResponse.getMessage());
        assertEquals(HttpStatusCodes.NOT_FOUND.getStatusCode(), response.getStatus()
                                                                        .getStatusCode());
    }

    /**
     * Test write entry with expand not found 2.
     *
     * @throws EntityProviderException the entity provider exception
     * @throws ODataException the o data exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testWriteEntryWithExpandNotFound2() throws EntityProviderException, ODataException, IOException {
        ODataContext ctx = EasyMock.createMock(ODataContext.class);
        UriInfo uriInfo = EasyMock.createMock(UriInfo.class);
        EasyMock.replay(ctx, uriInfo);
        String contentType = "application/json";

        // null written to response => valid error document has to be returned as response
        ODataResponse response = ExpandCallBack.writeEntryWithExpand(ctx, uriInfo, Collections.emptyMap(), contentType);
        assertNotNull(response);

        // try to retrieve the error document from the response
        ODataErrorContext errorResponse = null;
        try (InputStream content = response.getEntityAsStream()) {
            errorResponse = EntityProvider.readErrorDocument(content, contentType);
        }

        assertEquals(contentType, errorResponse.getContentType());
        assertEquals("No content", errorResponse.getMessage());
        assertEquals(HttpStatusCodes.NOT_FOUND.getStatusCode(), response.getStatus()
                                                                        .getStatusCode());
    }

    /**
     * Test get tenant name from context multiple batch parents.
     */
    @Test
    public void testGetTenantNameFromContextMultipleBatchParents() {
        String resultTenantName = "TestName";
        ODataContext testContext = EasyMock.createMock(ODataContext.class);
        ODataContext testContext2 = EasyMock.createMock(ODataContext.class);
        ODataContext testContext3 = EasyMock.createMock(ODataContext.class);
        EasyMock.expect(testContext.getBatchParentContext())
                .andReturn(testContext2)
                .times(1);
        EasyMock.expect(testContext2.getBatchParentContext())
                .andReturn(testContext3)
                .times(2);
        EasyMock.expect(testContext3.getBatchParentContext())
                .andReturn(null)
                .times(1);
        EasyMock.expect(testContext3.getParameter(OData2Constants.ODATA_CTX_PARAMETER_TENANT_NAME))
                .andReturn(resultTenantName);
        EasyMock.replay(testContext, testContext2, testContext3);
        assertEquals(resultTenantName, OData2Utils.getTenantNameFromContext(testContext));
        EasyMock.verify(testContext, testContext2, testContext3);
    }

    /**
     * Test get tenant name from context one batch parent.
     */
    @Test
    public void testGetTenantNameFromContextOneBatchParent() {
        String resultTenantName = "TestName";
        ODataContext testContext = EasyMock.createMock(ODataContext.class);
        ODataContext testContext2 = EasyMock.createMock(ODataContext.class);
        EasyMock.expect(testContext.getBatchParentContext())
                .andReturn(testContext2)
                .times(1);
        EasyMock.expect(testContext2.getBatchParentContext())
                .andReturn(null)
                .times(1);
        EasyMock.expect(testContext2.getParameter(OData2Constants.ODATA_CTX_PARAMETER_TENANT_NAME))
                .andReturn(resultTenantName);
        EasyMock.replay(testContext, testContext2);
        assertEquals(resultTenantName, OData2Utils.getTenantNameFromContext(testContext));
        EasyMock.verify(testContext, testContext2);
    }

    /**
     * Test get tenant name from context no batch parent.
     */
    @Test
    public void testGetTenantNameFromContextNoBatchParent() {
        String resultTenantName = "TestName";
        ODataContext testContext = EasyMock.createMock(ODataContext.class);
        EasyMock.expect(testContext.getBatchParentContext())
                .andReturn(null)
                .times(1);
        EasyMock.expect(testContext.getParameter(OData2Constants.ODATA_CTX_PARAMETER_TENANT_NAME))
                .andReturn(resultTenantName);
        EasyMock.replay(testContext);
        assertEquals(resultTenantName, OData2Utils.getTenantNameFromContext(testContext));
        EasyMock.verify(testContext);
    }

    /**
     * Test get key predicate value by property name.
     *
     * @throws EdmException the edm exception
     */
    @Test
    public void testGetKeyPredicateValueByPropertyName() throws EdmException {
        EdmProperty edmProperty = EasyMock.createMock(EdmProperty.class);
        KeyPredicate keyPredicate = EasyMock.createMock(KeyPredicate.class);
        List<KeyPredicate> keyPredicates = new ArrayList<>();
        keyPredicates.add(keyPredicate);

        EasyMock.expect(edmProperty.getName())
                .andReturn("TestProperty");
        EasyMock.replay(edmProperty);

        EasyMock.expect(keyPredicate.getProperty())
                .andReturn(edmProperty);
        EasyMock.expect(keyPredicate.getLiteral())
                .andReturn("TestValue");
        EasyMock.replay(keyPredicate);

        assertEquals("TestValue", OData2Utils.getKeyPredicateValueByPropertyName("TestProperty", keyPredicates));
        EasyMock.verify(edmProperty);
        EasyMock.verify(keyPredicate);
    }

    /**
     * Test is property parameter.
     *
     * @throws EdmException the edm exception
     */
    @Test
    public void testIsPropertyParameter() throws EdmException {
        EdmProperty edmProperty = EasyMock.createMock(EdmProperty.class);
        SQLSelectBuilder query = EasyMock.createMock(SQLSelectBuilder.class);
        EdmStructuralType entityType = EasyMock.createMock(EdmStructuralType.class);

        EasyMock.expect(edmProperty.getName())
                .andReturn("TestProperty");
        EasyMock.replay(edmProperty);

        List<String> targetParameters = new ArrayList<>();
        targetParameters.add("TestProperty");

        EasyMock.expect(query.getSQLTableParameters(entityType))
                .andReturn(targetParameters);
        EasyMock.replay(query);

        EasyMock.replay(entityType);

        assertEquals(true, OData2Utils.isPropertyParameter(edmProperty, query, entityType));
        EasyMock.verify(edmProperty);
        EasyMock.verify(query);
        EasyMock.verify(entityType);
    }
}
