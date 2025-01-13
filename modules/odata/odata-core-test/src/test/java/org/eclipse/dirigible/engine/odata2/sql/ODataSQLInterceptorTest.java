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

import static org.apache.olingo.odata2.api.commons.ODataHttpMethod.DELETE;
import static org.apache.olingo.odata2.api.commons.ODataHttpMethod.GET;
import static org.apache.olingo.odata2.api.commons.ODataHttpMethod.PATCH;
import static org.apache.olingo.odata2.api.commons.ODataHttpMethod.POST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.apache.cxf.helpers.IOUtils;
import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.uri.UriInfo;
import org.apache.olingo.odata2.api.uri.info.DeleteUriInfo;
import org.apache.olingo.odata2.api.uri.info.PostUriInfo;
import org.apache.olingo.odata2.api.uri.info.PutMergePatchUriInfo;
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

import liquibase.resource.AbstractResourceAccessor;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * The Class ODataSQLInterceptorTest.
 */
public class ODataSQLInterceptorTest extends AbstractSQLProcessorTest {

    /**
     * Gets the o data entities.
     *
     * @return the o data entities
     */
    @Override
    protected Class<?>[] getODataEntities() {
        Class<?>[] classes = {Car.class, Driver.class, Owner.class, Address.class};
        return classes;
    }

    // /**
    // * Test SQL interceptor read entity.
    // *
    // * @throws Exception the exception
    // */
    // @Test
    // public void testSQLInterceptorReadEntity() throws Exception {
    // sf.addInterceptors(Collections.singletonList(new SQLInterceptor(){
    // @Override
    // public SQLSelectBuilder onRead(SQLSelectBuilder query, UriInfo uriInfo, ODataContext context)
    // throws ODataException {
    // EdmEntityType et = uriInfo.getTargetEntitySet().getEntityType();
    // EdmProperty prop = (EdmProperty) et.getProperty("Make");
    // String sql = query.getSQLTableColumn(et, prop) + "=?";
    // SQLStatementParam param = new SQLStatementParam("BMW", prop, query.getSQLTableColumnInfo(et,
    // prop));
    // SQLWhereClause wc = new SQLWhereClause(sql, param);
    // query.getWhereClause().and(wc);
    // return query;
    // }
    //
    // }));
    // Response response = OData2RequestBuilder.createRequest(sf) //
    // .segments("Cars") //
    // .param("$top", "10") //
    // .executeRequest(GET);
    // assertEquals(200, response.getStatus());
    // ODataFeed resultFeed = retrieveODataFeed(response, "Cars");
    // List<ODataEntry> entries = resultFeed.getEntries();
    // assertEquals("The interceptor has to add a condition where we get a filter", 1, entries.size());
    // for (ODataEntry entry: entries){
    // Map<String, Object> props = entry.getProperties();
    // assertEquals("BMW", props.get("Make"));
    // }
    // }


    /**
     * Test SQL interceptor create entity.
     *
     * @throws Exception the exception
     */
    @Test
    public void testSQLInterceptorCreateEntity() throws Exception {
        sf.addInterceptors(Collections.singletonList(new SQLInterceptor() {

            @Override
            public SQLInsertBuilder onCreate(SQLInsertBuilder query, PostUriInfo uriInfo, ODataContext context) throws ODataException {
                ODataEntry entry = query.getEntry();
                entry.getProperties()
                     .put("FirstName", "Dan");
                entry.getProperties()
                     .put("LastName", "Kolov");
                return query;
            }
        }));
        String existingCarId = "639cac17-4cfd-4d94-b5d0-111fd5488423";
        String newDriverId = "11111111-1111-1111-1111-111111111111";
        String content = "{" //
                + "  \"d\": {" //
                + "    \"__metadata\": {" //
                + "      \"type\": \"org.eclipse.dirigible.engine.odata2.sql.entities.Driver\"" //
                + "    }," //
                + "    \"Id\": \"" + newDriverId + "\"," //
                + "    \"FirstName\": \"John\"," //
                + "    \"LastName\": \"Smith\"," //
                + "    \"Car\": " + "         { \"Id\": \"" + existingCarId + "\"}" //
                + " }" //
                + "}";

        System.out.println(content);
        Response createResponse = modifyingRequestBuilder(sf, content)//
                                                                      .segments("Drivers") //
                                                                      .accept("application/json")//
                                                                      .param("content-type", "application/json") //
                                                                      .content(content)//
                                                                      .contentSize(content.length())
                                                                      .executeRequest(POST);

        assertEquals(201, createResponse.getStatus());
        String createResponseStr = IOUtils.toString((InputStream) createResponse.getEntity());
        // System.out.println(createResponseStr);

        assertTrue(createResponseStr.contains("Dan"));
        assertTrue(createResponseStr.contains("Kolov"));
        assertFalse(createResponseStr.contains("John"));
        assertFalse(createResponseStr.contains("Smith"));
    }


    /**
     * Test SQL interceptor delete entity.
     *
     * @throws Exception the exception
     */
    @Test
    public void testSQLInterceptorDeleteEntity() throws Exception {
        sf.addInterceptors(Collections.singletonList(new SQLInterceptor() {

            @Override
            public SQLDeleteBuilder onDelete(SQLDeleteBuilder query, DeleteUriInfo uriInfo, ODataContext context) {
                Map<String, Object> deleteKeys = query.getDeleteKeys();
                deleteKeys.put("Id", "7990d49f-cfaf-48ab-8c6f-adbe7aaa069e");
                return query;
            }
        }));

        Response getCar = OData2RequestBuilder.createRequest(sf) //
                                              .segments("Drivers('695796c4-09a1-11ec-9a03-0242ac130006')") //
                                              .accept("application/json")
                                              .executeRequest(GET);
        assertEquals(200, getCar.getStatus());

        Response deleteCar = OData2RequestBuilder.createRequest(sf) //
                                                 .segments("Drivers('695796c4-09a1-11ec-9a03-0242ac130006')") //
                                                 .accept("application/json")
                                                 .executeRequest(DELETE);
        assertEquals(204, deleteCar.getStatus());

        Response getDeletedCar = OData2RequestBuilder.createRequest(sf) //
                                                     .segments("Drivers('695796c4-09a1-11ec-9a03-0242ac130006')") //
                                                     .accept("application/json")
                                                     .executeRequest(GET);
        assertEquals(200, getDeletedCar.getStatus());
    }

    /**
     * Test SQL interceptor patch entity.
     *
     * @throws Exception the exception
     */
    @Test
    public void testSQLInterceptorPatchEntity() throws Exception {
        sf.addInterceptors(Collections.singletonList(new SQLInterceptor() {

            @Override
            public SQLUpdateBuilder onUpdate(SQLUpdateBuilder query, PutMergePatchUriInfo uriInfo, ODataContext context) {
                ODataEntry entry = query.getUpdateEntry();
                entry.getProperties()
                     .put("Price", "10000000");
                return query;
            }
        }));

        Response existingCar = OData2RequestBuilder.createRequest(sf) //
                                                   .segments("Cars('639cac17-4cfd-4d94-b5d0-111fd5488423')") //
                                                   .accept("application/json")
                                                   .executeRequest(GET);
        assertEquals(200, existingCar.getStatus());

        String content = "{" //
                + "  \"d\": {" //
                + "    \"__metadata\": {" //
                + "      \"type\": \"org.eclipse.dirigible.engine.odata2.sql.entities.Car\"" //
                + "    }," //
                + "    \"Price\": 50001.0" //
                + " }" + "}";

        Response response = modifyingRequestBuilder(sf, content)//
                                                                .segments("Cars('639cac17-4cfd-4d94-b5d0-111fd5488423')") //
                                                                .accept("application/json")//
                                                                .content(content)//
                                                                .param("content-type", "application/json")//
                                                                .contentSize(content.length())
                                                                .executeRequest(PATCH);
        assertEquals(204, response.getStatus());

        existingCar = OData2RequestBuilder.createRequest(sf) //
                                          .segments("Cars('639cac17-4cfd-4d94-b5d0-111fd5488423')") //
                                          .accept("application/json")
                                          .executeRequest(GET);
        assertEquals(200, existingCar.getStatus());
        ODataEntry resultEntry = retrieveODataEntry(existingCar, "Cars");
        Map<String, Object> properties = resultEntry.getProperties();
        assertEquals("BMW", properties.get("Make"));
        assertEquals("530e", properties.get("Model"));
        assertEquals(2021, properties.get("Year"));
        assertEquals(10000000.0d, properties.get("Price"));
    }

}
