/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.builder;

import org.apache.olingo.odata2.annotation.processor.core.edm.AnnotationEdmProvider;
import org.apache.olingo.odata2.api.uri.PathSegment;
import org.apache.olingo.odata2.api.uri.UriInfo;
import org.apache.olingo.odata2.api.uri.UriParser;
import org.apache.olingo.odata2.core.ODataPathSegmentImpl;
import org.apache.olingo.odata2.core.edm.provider.EdmImplProv;
import org.apache.olingo.odata2.core.uri.UriParserImpl;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBindingProvider;
import org.eclipse.dirigible.engine.odata2.sql.edm.CTEntity;
import org.eclipse.dirigible.engine.odata2.sql.edm.Entity1;
import org.eclipse.dirigible.engine.odata2.sql.edm.Entity2;
import org.eclipse.dirigible.engine.odata2.sql.edm.Entity3;
import org.eclipse.dirigible.engine.odata2.sql.mapping.DefaultEdmTableMappingProvider;
import org.eclipse.dirigible.engine.odata2.sql.test.util.OData2TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * The Class SQLSelectBuilderNavigationPropertiesTest.
 */
public class SQLSelectBuilderNavigationPropertiesTest {

    /** The Constant SERVER_SIDE_PAGING_DEFAULT_SUFFIX. */
    private static final String SERVER_SIDE_PAGING_DEFAULT_SUFFIX =
            String.format(" FETCH FIRST %d ROWS ONLY", SQLQueryBuilder.DEFAULT_SERVER_PAGING_SIZE);

    /** The provider. */
    AnnotationEdmProvider provider;

    /** The uri parser. */
    UriParser uriParser;

    /** The builder. */
    SQLQueryBuilder builder;

    /** The context. */
    SQLContext context;

    /**
     * Sets the up.
     *
     * @throws Exception the exception
     */
    @Before
    public void setUp() throws Exception {
        Class<?>[] classes = { //
                Entity1.class, //
                Entity2.class, //
                Entity3.class, //
                CTEntity.class //
        };
        provider = new AnnotationEdmProvider(Arrays.asList(classes));
        EdmImplProv edm = new EdmImplProv(provider);
        uriParser = new UriParserImpl(edm);
        EdmTableBindingProvider tableMappingProvider = new DefaultEdmTableMappingProvider(OData2TestUtils.resources(classes));
        builder = new SQLQueryBuilder(tableMappingProvider);
        context = new SQLContext();
    }

    /**
     * Creates the path segment.
     *
     * @param path the path
     * @return the o data path segment impl
     */
    private ODataPathSegmentImpl createPathSegment(final String path) {
        return new ODataPathSegmentImpl(path, Collections.<String, List<String>>emptyMap());
    }

    /**
     * Test zero to one navigation with filter.
     *
     * @throws Exception the exception
     */
    @Test
    public void testZeroToOneNavigationWithFilter() throws Exception {
        PathSegment ps1 = createPathSegment("Entities2");
        Map<String, String> params = new HashMap<>();
        params.put("$filter", "Entity1/Status eq 'ERROR' and Value eq 'Something'");
        UriInfo uriInfo = uriParser.parse(Arrays.asList(ps1), params);

        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        assertEquals("SELECT T0.ID AS \"ID_T0\", T0.NAME AS \"NAME_T0\", T0.VALUE AS \"VALUE_T0\" "
                + "FROM ITOP_MPLUSERDEFINEDATTRIBUTE AS T0 " + "LEFT JOIN MPLHEADER AS T1 ON T1.ID = T0.HEADER_ID WHERE T1.STATUS = ? "
                + "AND T0.VALUE = ? ORDER BY T0.ID ASC" + SERVER_SIDE_PAGING_DEFAULT_SUFFIX, q.buildSelect(context));
    }

    // @Test
    /**
     * Test zero to one navigation with select.
     *
     * @throws Exception the exception
     */
    // @Ignore // TODO This feature is not implemented yet!
    public void testZeroToOneNavigationWithSelect() throws Exception {
        PathSegment ps1 = createPathSegment("Entities2");
        Map<String, String> params = new HashMap<>();
        params.put("$select", "Entity1/Status, Value");
        UriInfo uriInfo = uriParser.parse(Arrays.asList(ps1), params);

        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        assertEquals("SELECT T0.ID AS ID_T0, T0.NAME AS NAME_T0, T0.VALUE AS VALUE_T0 " //
                + "FROM ITOP_MPLUSERDEFINEDATTRIBUTE AS T0 LEFT JOIN MPLHEADER AS T1 ON T1.ID = T0.HEADER_ID " //
                + "WHERE T1.STATUS = ? AND T0.VALUE = ?" + SERVER_SIDE_PAGING_DEFAULT_SUFFIX, q.buildSelect(context));
    }

    // @Test(expected = UriSyntaxException.class)
    // // Olingo 2.0.6: one-to-many navigation with filter and attribute access is not supported by
    // OData.
    // // Hence the UriSyntaxException has been introduced for that case.
    // public void testOneToManyNavigationWithFilter() throws Exception {
    // PathSegment ps1 = createPathSegment("Entities1");
    // Map<String, String> params = new HashMap<>();
    // params.put("$filter", "Status eq 'ERROR' and Entity2/Value eq 'Something'");
    // UriInfo uriInfo = uriParser.parse(Arrays.asList(ps1), params);
    //
    // SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo);
    // assertEquals(
    // "SELECT T0.MESSAGEGUID AS MESSAGEGUID_T0, T0.LOGSTART AS LOGSTART_T0, T0.LOGEND AS LOGEND_T0,
    // T0.SENDER AS SENDER_T0, T0.RECEIVER AS RECEIVER_T0, T0.STATUS AS STATUS_T0, T0.MESSAGEGUID AS
    // MESSAGEGUID_T0 "
    // + "FROM MPLHEADER AS T0 LEFT JOIN ITOP_MPLUSERDEFINEDATTRIBUTE AS T1 ON T1.HEADER_ID = T0.ID " //
    // + "WHERE T0.STATUS = ? AND T1.VALUE = ?" + SERVER_SIDE_PAGING_DEFAULT_SUFFIX,
    // q.buildSelect(context));
    // }

    // @Test
    // @Ignore
    /**
     * Test one to many navigation with select.
     *
     * @throws Exception the exception
     */
    // TODO This feature is not implemented yet!
    public void testOneToManyNavigationWithSelect() throws Exception {
        PathSegment ps1 = createPathSegment("Entities1");
        Map<String, String> params = new HashMap<>();
        params.put("$select", "Status,Entity2/Value");
        params.put("$filter", "Status eq 'ERROR' and Entity2/Value eq 'Something'");
        UriInfo uriInfo = uriParser.parse(Arrays.asList(ps1), params);

        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        assertEquals(
                "SELECT T0.MESSAGEGUID AS MESSAGEGUID_T0, T0.LOGSTART AS LOGSTART_T0, T0.LOGEND AS LOGEND_T0, T0.SENDER AS SENDER_T0, T0.RECEIVER AS RECEIVER_T0, T0.STATUS AS STATUS_T0, T0.MESSAGEGUID AS MESSAGEGUID_T0 "
                        + "FROM MPLHEADER AS T0 LEFT JOIN ITOP_MPLUSERDEFINEDATTRIBUTE AS T1 ON T1.HEADER_ID = T0.ID " //
                        + "WHERE T0.STATUS = ? AND T1.VALUE = ?" + SERVER_SIDE_PAGING_DEFAULT_SUFFIX,
                q.buildSelect(context));
    }

    /**
     * Test two step zero to one navigation with filter.
     *
     * @throws Exception the exception
     */
    @Test
    public void testTwoStepZeroToOneNavigationWithFilter() throws Exception {
        PathSegment ps1 = createPathSegment("Entities3");
        Map<String, String> params = new HashMap<>();
        params.put("$filter", "Entity2/Entity1/Status eq 'ERROR' and Description eq 'Something'");
        UriInfo uriInfo = uriParser.parse(Arrays.asList(ps1), params);

        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        assertEquals(
                "SELECT T0.ID AS \"ID_T0\", T0.DESCRIPTION AS \"DESCRIPTION_T0\", T2.CT_ID AS \"CT_ID_T2\", "
                        + "T2.CT_DETAIL AS \"CT_DETAIL_T2\" "
                        + "FROM ENTITY3_TABLE AS T0 LEFT JOIN ITOP_MPLUSERDEFINEDATTRIBUTE AS T3 ON T3.ID = T0.ID_OF_ENTITY2 "
                        + "LEFT JOIN MPLHEADER AS T1 ON T1.ID = T3.HEADER_ID "
                        + "LEFT JOIN COMPLEX_TYPE_ENTITY_TABLE AS T2 ON T2.CT_ID = T0.COMPLEX_TYPE_JOIN_COLUMN "
                        + "WHERE T1.STATUS = ? AND T0.DESCRIPTION = ? ORDER BY T0.ID ASC" + SERVER_SIDE_PAGING_DEFAULT_SUFFIX,
                q.buildSelect(context));
    }

    /**
     * Test two step zero to one navigation with filter on complex type.
     *
     * @throws Exception the exception
     */
    @Test
    public void testTwoStepZeroToOneNavigationWithFilterOnComplexType() throws Exception {
        PathSegment ps1 = createPathSegment("Entities3");
        Map<String, String> params = new HashMap<>();
        params.put("$filter", "Entity2/Entity1/Status eq 'ERROR' and ComplexTypeProperty/Detail eq 'Something'");
        UriInfo uriInfo = uriParser.parse(Arrays.asList(ps1), params);

        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        assertEquals(
                "SELECT T0.ID AS \"ID_T0\", T0.DESCRIPTION AS \"DESCRIPTION_T0\", T2.CT_ID AS \"CT_ID_T2\", "
                        + "T2.CT_DETAIL AS \"CT_DETAIL_T2\" " + "FROM ENTITY3_TABLE AS T0 "
                        + "LEFT JOIN ITOP_MPLUSERDEFINEDATTRIBUTE AS T3 ON T3.ID = T0.ID_OF_ENTITY2 "
                        + "LEFT JOIN MPLHEADER AS T1 ON T1.ID = T3.HEADER_ID "
                        + "LEFT JOIN COMPLEX_TYPE_ENTITY_TABLE AS T2 ON T2.CT_ID = T0.COMPLEX_TYPE_JOIN_COLUMN "
                        + "WHERE T1.STATUS = ? AND T2.CT_DETAIL = ? ORDER BY T0.ID ASC" + SERVER_SIDE_PAGING_DEFAULT_SUFFIX,
                q.buildSelect(context));
    }

}
