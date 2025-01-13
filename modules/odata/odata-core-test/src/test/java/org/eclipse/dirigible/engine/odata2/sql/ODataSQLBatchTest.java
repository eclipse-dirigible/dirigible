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
import org.apache.olingo.odata2.api.client.batch.*;
import org.apache.olingo.odata2.api.commons.ODataHttpMethod;
import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.uri.UriInfo;
import org.apache.olingo.odata2.api.uri.info.DeleteUriInfo;
import org.apache.olingo.odata2.api.uri.info.PostUriInfo;
import org.apache.olingo.odata2.api.uri.info.PutMergePatchUriInfo;
import org.apache.olingo.odata2.core.batch.v2.BatchParser;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLInterceptor;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatementParam;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLDeleteBuilder;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLInsertBuilder;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLSelectBuilder;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLUpdateBuilder;
import org.eclipse.dirigible.engine.odata2.sql.clause.SQLWhereClause;
import org.eclipse.dirigible.engine.odata2.sql.entities.Address;
import org.eclipse.dirigible.engine.odata2.sql.entities.Car;
import org.eclipse.dirigible.engine.odata2.sql.entities.Driver;
import org.eclipse.dirigible.engine.odata2.sql.entities.Owner;
import org.junit.Test;

import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

import static org.apache.olingo.odata2.api.commons.ODataHttpMethod.*;
import static org.junit.Assert.*;

/**
 * The Class ODataSQLBatchTest.
 */
public class ODataSQLBatchTest extends AbstractSQLProcessorTest {

    /**
     * Gets the o data entities.
     *
     * @return the o data entities
     */
    @Override
    protected Class<?>[] getODataEntities() {
        return new Class[] {Car.class, Driver.class, Owner.class, Address.class};
    }


    /**
     * Gets the boundary.
     *
     * @param body the body
     * @return the boundary
     */
    private String getBoundary(String body) {
        return body.split("\r\n")[0].substring(2);
    }

    /**
     * Test execute metadata request in batch.
     *
     * @throws Exception the exception
     */
    @Test
    public void testExecuteMetadataRequestInBatch() throws Exception {
        List<BatchPart> batch = new ArrayList<>();
        BatchPart request = BatchQueryPart.method(ODataHttpMethod.GET.name())
                                          .uri("$metadata")
                                          .build();
        batch.add(request);
        ODataResponse res = OData2RequestBuilder.createRequest(sf)
                                                .executeBatchRequest(batch);
        assertEquals(202, res.getStatus()
                             .getStatusCode()); // Accepted
        String responseEntity = IOUtils.toString(res.getEntityAsStream());
        String boundary = getBoundary(responseEntity);

        BatchParser parser = new BatchParser("multipart/mixed;boundary=" + boundary, true);
        List<BatchSingleResponse> responses = parser.parseBatchResponse(new ByteArrayInputStream(responseEntity.getBytes()));
        for (BatchSingleResponse response : responses) {
            assertEquals("200", response.getStatusCode());
            assertEquals("OK", response.getStatusInfo());
            assertTrue(response.getBody()
                               .startsWith(
                                       "<?xml version='1.0' encoding='UTF-8'?><edmx:Edmx xmlns:edmx=\"http://schemas.microsoft.com/ado/2007/06/edmx\" Version=\"1.0\"><edmx:DataServices"));
        }
    }


    /**
     * Test execute metadata request batch change set two updates.
     *
     * @throws Exception the exception
     */
    @Test
    public void testExecuteMetadataRequestBatchChangeSetTwoUpdates() throws Exception {
        String content = "{" //
                + "  \"d\": {" //
                + "    \"__metadata\": {" //
                + "      \"type\": \"org.eclipse.dirigible.engine.odata2.sql.entities.Car\"" //
                + "    }," //
                + "    \"Id\": \"XXXXXXXXX\"," //
                + "    \"Price\": 123456789.0" //
                + " }" + "}";

        List<BatchPart> batch = new ArrayList<>();

        Map<String, String> changeSetHeaders = new HashMap<>();
        changeSetHeaders.put("content-type", "application/json");
        changeSetHeaders.put("Accept", "application/json");
        BatchChangeSetPart changeRequest = BatchChangeSetPart.method(PUT.toString())
                                                             .uri("Cars('639cac17-4cfd-4d94-b5d0-111fd5488423')")
                                                             .body(content.replaceAll("XXXXXXXXX", "639cac17-4cfd-4d94-b5d0-111fd5488423"))
                                                             .headers(changeSetHeaders)
                                                             .contentId("1")
                                                             .build();
        BatchChangeSet changeSet = BatchChangeSet.newBuilder()
                                                 .build();
        changeSet.add(changeRequest);

        changeSetHeaders = new HashMap<>();
        changeSetHeaders.put("content-type", "application/json;odata=verbose");
        changeSetHeaders.put("Accept", "application/json");
        BatchChangeSetPart changeRequest2 = BatchChangeSetPart.method(PUT.toString())
                                                              .uri("Cars('3b1ea3aa-e18a-434b-9d6b-a1044ba8c7e5')")
                                                              .body(content.replaceAll("XXXXXXXXX", "3b1ea3aa-e18a-434b-9d6b-a1044ba8c7e5"))
                                                              .headers(changeSetHeaders)
                                                              .contentId("2")
                                                              .build();
        changeSet.add(changeRequest2);
        batch.add(changeSet);


        ODataResponse res = OData2RequestBuilder.createRequest(sf)
                                                .executeBatchRequest(batch);
        assertEquals(202, res.getStatus()
                             .getStatusCode()); // Accepted
        String responseEntity = IOUtils.toString(res.getEntityAsStream());
        String boundary = getBoundary(responseEntity);

        BatchParser parser = new BatchParser("multipart/mixed;boundary=" + boundary, true);
        List<BatchSingleResponse> responses = parser.parseBatchResponse(new ByteArrayInputStream(responseEntity.getBytes()));
        assertEquals(2, responses.size());
        for (BatchSingleResponse response : responses) {
            assertEquals("204", response.getStatusCode());
            assertEquals("No Content", response.getStatusInfo());
        }
        assertCarHasPrice("Cars('639cac17-4cfd-4d94-b5d0-111fd5488423')", 123456789.0d);
        assertCarHasPrice("Cars('3b1ea3aa-e18a-434b-9d6b-a1044ba8c7e5')", 123456789.0d);
    }

    /**
     * Test execute metadata request batch change set rollback.
     *
     * @throws Exception the exception
     */
    @Test
    public void testExecuteMetadataRequestBatchChangeSetRollback() throws Exception {
        String content = "{" //
                + "  \"d\": {" //
                + "    \"__metadata\": {" //
                + "      \"type\": \"org.eclipse.dirigible.engine.odata2.sql.entities.Car\"" //
                + "    }," //
                + "    \"Id\": \"XXXXXXXXX\"," //
                + "    \"Price\": 123456789.0" //
                + " }" + "}";

        List<BatchPart> batch = new ArrayList<>();

        Map<String, String> changeSetHeaders = new HashMap<>();
        changeSetHeaders.put("content-type", "application/json");
        changeSetHeaders.put("Accept", "application/json");
        BatchChangeSetPart changeRequest = BatchChangeSetPart.method(PUT.toString())
                                                             .uri("Cars('639cac17-4cfd-4d94-b5d0-111fd5488423')")
                                                             .body(content.replaceAll("XXXXXXXXX", "639cac17-4cfd-4d94-b5d0-111fd5488423"))
                                                             .headers(changeSetHeaders)
                                                             .contentId("1")
                                                             .build();
        BatchChangeSet changeSet = BatchChangeSet.newBuilder()
                                                 .build();
        changeSet.add(changeRequest);

        changeSetHeaders = new HashMap<>();
        changeSetHeaders.put("content-type", "application/json;odata=verbose");
        changeSetHeaders.put("Accept", "application/json");
        BatchChangeSetPart invalidPriceChangeSet = BatchChangeSetPart.method(PUT.toString())
                                                                     .uri("Cars('3b1ea3aa-e18a-434b-9d6b-a1044ba8c7e5')")
                                                                     .body(content.replaceAll("123456789.0", "BOOM")// Simulate error
                                                                                                                    // by setting
                                                                                                                    // invalid price
                                                                                  .replaceAll("XXXXXXXXX",
                                                                                          "3b1ea3aa-e18a-434b-9d6b-a1044ba8c7e5"))
                                                                     .headers(changeSetHeaders)
                                                                     .contentId("2")
                                                                     .build();
        changeSet.add(invalidPriceChangeSet);
        batch.add(changeSet);


        ODataResponse res = OData2RequestBuilder.createRequest(sf)
                                                .executeBatchRequest(batch);
        assertEquals(202, res.getStatus()
                             .getStatusCode()); // Accepted
        String responseEntity = IOUtils.toString(res.getEntityAsStream());
        String boundary = getBoundary(responseEntity);

        BatchParser parser = new BatchParser("multipart/mixed;boundary=" + boundary, true);
        List<BatchSingleResponse> responses = parser.parseBatchResponse(new ByteArrayInputStream(responseEntity.getBytes()));
        assertEquals(1, responses.size());
        for (BatchSingleResponse response : responses) {
            assertEquals("400", response.getStatusCode());
            assertEquals("Bad Request", response.getStatusInfo());
        }

        assertCarHasPrice("Cars('639cac17-4cfd-4d94-b5d0-111fd5488423')", 67000.0);
        assertCarHasPrice("Cars('3b1ea3aa-e18a-434b-9d6b-a1044ba8c7e5')", 7000.0);
    }

    /**
     * Test execute metadata request batch change set rollback and put.
     *
     * @throws Exception the exception
     */
    @Test
    public void testExecuteMetadataRequestBatchChangeSetRollbackAndPut() throws Exception {
        String content = "{" //
                + "  \"d\": {" //
                + "    \"__metadata\": {" //
                + "      \"type\": \"org.eclipse.dirigible.engine.odata2.sql.entities.Car\"" //
                + "    }," //
                + "    \"Id\": \"3b1ea3aa-e18a-434b-9d6b-a1044ba8c7e5\"," //
                + "    \"Price\": 123456789.0" //
                + " }" + "}";

        List<BatchPart> batch = new ArrayList<>();

        BatchChangeSet changeSet = BatchChangeSet.newBuilder()
                                                 .build();

        Map<String, String> changeSetHeaders = new HashMap<>();
        changeSetHeaders.put("content-type", "application/json;odata=verbose");
        changeSetHeaders.put("Accept", "application/json");
        BatchChangeSetPart invalidPriceChangeSet = BatchChangeSetPart.method(PUT.toString())
                                                                     .uri("Cars('3b1ea3aa-e18a-434b-9d6b-a1044ba8c7e5')")
                                                                     .body(content.replaceAll("123456789.0", "BOOM"))// Simulate error by
                                                                                                                     // setting invalid
                                                                                                                     // price
                                                                     .headers(changeSetHeaders)
                                                                     .contentId("1")
                                                                     .build();
        changeSet.add(invalidPriceChangeSet);
        batch.add(changeSet);


        ODataResponse res = OData2RequestBuilder.createRequest(sf)
                                                .executeBatchRequest(batch);
        assertEquals(202, res.getStatus()
                             .getStatusCode()); // Accepted
        String responseEntity = IOUtils.toString(res.getEntityAsStream());
        String boundary = getBoundary(responseEntity);

        BatchParser parser = new BatchParser("multipart/mixed;boundary=" + boundary, true);
        List<BatchSingleResponse> responses = parser.parseBatchResponse(new ByteArrayInputStream(responseEntity.getBytes()));
        assertEquals(1, responses.size());
        for (BatchSingleResponse response : responses) {
            assertEquals("400", response.getStatusCode());
            assertEquals("Bad Request", response.getStatusInfo());
        }
        assertCarHasPrice("Cars('3b1ea3aa-e18a-434b-9d6b-a1044ba8c7e5')", 7000.0);

        modifyingRequestBuilder(sf, content)//
                                            .segments("Cars('3b1ea3aa-e18a-434b-9d6b-a1044ba8c7e5')") //
                                            .accept("application/json")//
                                            .content(content)//
                                            .param("content-type", "application/json")//
                                            .contentSize(content.length())
                                            .executeRequest(PUT);

        assertCarHasPrice("Cars('3b1ea3aa-e18a-434b-9d6b-a1044ba8c7e5')", 123456789.0);


    }

}
