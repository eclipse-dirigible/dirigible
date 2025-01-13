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
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmLiteralKind;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.edm.EdmSimpleType;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.uri.KeyPredicate;
import org.apache.olingo.odata2.api.uri.PathSegment;
import org.apache.olingo.odata2.api.uri.UriInfo;
import org.apache.olingo.odata2.api.uri.UriParser;
import org.apache.olingo.odata2.core.ODataPathSegmentImpl;
import org.apache.olingo.odata2.core.edm.provider.EdmImplProv;
import org.apache.olingo.odata2.core.ep.entry.ODataEntryImpl;
import org.apache.olingo.odata2.core.uri.UriParserImpl;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBindingProvider;
import org.eclipse.dirigible.engine.odata2.sql.clause.SQLSelectClause;
import org.eclipse.dirigible.engine.odata2.sql.edm.*;
import org.eclipse.dirigible.engine.odata2.sql.mapping.DefaultEdmTableMappingProvider;
import org.eclipse.dirigible.engine.odata2.sql.test.util.OData2TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static java.util.Collections.EMPTY_MAP;
import static org.junit.Assert.*;

/**
 * The Class SQLSelectBuilderTest.
 */
public class SQLSelectBuilderTest {

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
                Entity4.class, //
                Entity5.class, //
                Entity6.class //
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
     * @return the o data path segment impl
     */
    private ODataPathSegmentImpl createPathSegment() {
        return new ODataPathSegmentImpl("Entities1", Collections.emptyMap());
    }

    /**
     * Test MPL count.
     *
     * @throws Exception the exception
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testMPLCount() throws Exception {
        // MessageProcessingLogs/$count
        PathSegment ps1 = new ODataPathSegmentImpl("Entities1", EMPTY_MAP);
        PathSegment ps3 = new ODataPathSegmentImpl("$count", EMPTY_MAP);
        UriInfo uriInfo = uriParser.parse(Arrays.asList(ps1, ps3), EMPTY_MAP);

        SQLSelectBuilder q = builder.buildSelectCountQuery(uriInfo, null);
        SQLContext context = new SQLContext();
        assertEquals("SELECT COUNT(*) FROM \"MPLHEADER\" AS \"T0\"", q.buildSelect(context));
    }

    /**
     * Test count with date.
     *
     * @throws Exception the exception
     */
    @Test
    public void testCountWithDate() throws Exception {
        PathSegment ps1 = createPathSegment();
        PathSegment ps2 = new ODataPathSegmentImpl("$count", Collections.emptyMap());
        Map<String, String> params = new HashMap<>();
        params.put("$filter", "Status eq 'ERROR' and LogEnd lt datetime'2014-10-02T09:14:00'");
        UriInfo uriInfo = uriParser.parse(Arrays.asList(ps1, ps2), params);

        SQLSelectBuilder q = builder.buildSelectCountQuery(uriInfo, null);
        assertEquals("\"T0\".\"STATUS\" = ? AND \"T0\".\"LOGEND\" < ?", q.getWhereClause()
                                                                         .getWhereClause());
        assertFalse(q.getJoinWhereClauses()
                     .hasNext());

        assertEquals("ERROR", q.getStatementParams()
                               .get(0)
                               .getValue());
        assertTrue(q.getStatementParams()
                    .get(1)
                    .getValue() instanceof Calendar);
        assertEquals("SELECT COUNT(*) FROM \"MPLHEADER\" AS \"T0\" WHERE \"T0\".\"STATUS\" = ? AND \"T0\".\"LOGEND\" < ?",
                q.buildSelect(context));
    }

    /**
     * Test build select with filter.
     *
     * @throws Exception the exception
     */
    @Test
    public void testBuildSelectWithFilter() throws Exception {
        PathSegment ps1 = createPathSegment();
        Map<String, String> params = new HashMap<>();
        params.put("$filter", "Status eq 'ERROR' and LogEnd lt datetime'2014-10-02T09:14:00'");
        UriInfo uriInfo = uriParser.parse(Collections.singletonList(ps1), params);

        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        assertEquals("\"T0\".\"STATUS\" = ? AND \"T0\".\"LOGEND\" < ?", q.getWhereClause()
                                                                         .getWhereClause());
        assertFalse(q.getJoinWhereClauses()
                     .hasNext());

        assertEquals("ERROR", q.getStatementParams()
                               .get(0)
                               .getValue());
        assertTrue(q.getStatementParams()
                    .get(1)
                    .getValue() instanceof Calendar);
        // the AlternateWebLink is mapped to MESSAGEID, therefore 2 times MESSAGEID
        assertEquals(
                "SELECT \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\", \"T0\".\"LOGSTART\" AS \"LOGSTART_T0\", \"T0\".\"LOGEND\" AS \"LOGEND_T0\", "
                        + "\"T0\".\"SENDER\" AS \"SENDER_T0\", \"T0\".\"RECEIVER\" AS \"RECEIVER_T0\", \"T0\".\"STATUS\" AS \"STATUS_T0\", \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\" "
                        + "FROM \"MPLHEADER\" AS \"T0\" "
                        + "WHERE \"T0\".\"STATUS\" = ? AND \"T0\".\"LOGEND\" < ? ORDER BY \"T0\".\"MESSAGEGUID\" ASC"
                        + SERVER_SIDE_PAGING_DEFAULT_SUFFIX,
                q.buildSelect(context));
    }

    /**
     * Test build select with dynamic filter.
     *
     * @throws Exception the exception
     */
    @Test
    public void testBuildSelectWithDynamicFilter() throws Exception {
        PathSegment ps1 = createPathSegment();
        Map<String, String> params = new HashMap<>();
        params.put("$filter", "Status eq 'ERROR' and LogEnd lt datetime'2014-10-02T09:14:00'");
        UriInfo uriInfo = uriParser.parse(Collections.singletonList(ps1), params);

        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        assertEquals("\"T0\".\"STATUS\" = ? AND \"T0\".\"LOGEND\" < ?", q.getWhereClause()
                                                                         .getWhereClause());
        assertFalse(q.getJoinWhereClauses()
                     .hasNext());

        assertEquals("ERROR", q.getStatementParams()
                               .get(0)
                               .getValue());
        assertTrue(q.getStatementParams()
                    .get(1)
                    .getValue() instanceof Calendar);
        // the AlternateWebLink is mapped to MESSAGEID, therefore 2 times MESSAGEID
        assertEquals(
                "SELECT \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\", \"T0\".\"LOGSTART\" AS \"LOGSTART_T0\", \"T0\".\"LOGEND\" AS \"LOGEND_T0\", "
                        + "\"T0\".\"SENDER\" AS \"SENDER_T0\", \"T0\".\"RECEIVER\" AS \"RECEIVER_T0\", \"T0\".\"STATUS\" AS \"STATUS_T0\", "
                        + "\"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\" " + "FROM \"MPLHEADER\" AS \"T0\" "
                        + "WHERE \"T0\".\"STATUS\" = ? AND \"T0\".\"LOGEND\" < ? ORDER BY \"T0\".\"MESSAGEGUID\" ASC"
                        + SERVER_SIDE_PAGING_DEFAULT_SUFFIX,
                q.buildSelect(context));
    }

    /**
     * Test build select with order by.
     *
     * @throws Exception the exception
     */
    @Test
    public void testBuildSelectWithOrderBy() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("$orderby", "Status, LogStart desc");

        String expectedSelectStatment =
                "SELECT \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\", \"T0\".\"LOGSTART\" AS \"LOGSTART_T0\", \"T0\".\"LOGEND\" AS \"LOGEND_T0\", "
                        + "\"T0\".\"SENDER\" AS \"SENDER_T0\", \"T0\".\"RECEIVER\" AS \"RECEIVER_T0\", \"T0\".\"STATUS\" AS \"STATUS_T0\", \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\" "
                        + "FROM \"MPLHEADER\" AS \"T0\" " + "ORDER BY \"T0\".\"STATUS\" ASC, \"T0\".\"LOGSTART\" DESC"
                        + SERVER_SIDE_PAGING_DEFAULT_SUFFIX;
        testBuildSelectStatement(params, context.getDatabaseSystem(), expectedSelectStatment);
    }

    /**
     * Test build select without order by.
     *
     * @throws Exception the exception
     */
    @Test
    public void testBuildSelectWithoutOrderBy() throws Exception {
        Map<String, String> params = new HashMap<>();

        String expectedSelectStatment =
                "SELECT \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\", \"T0\".\"LOGSTART\" AS \"LOGSTART_T0\", \"T0\".\"LOGEND\" AS \"LOGEND_T0\", "
                        + "\"T0\".\"SENDER\" AS \"SENDER_T0\", \"T0\".\"RECEIVER\" AS \"RECEIVER_T0\", \"T0\".\"STATUS\" AS \"STATUS_T0\", \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\" "
                        + "FROM \"MPLHEADER\" AS \"T0\" " + "ORDER BY \"T0\".\"MESSAGEGUID\" ASC" + SERVER_SIDE_PAGING_DEFAULT_SUFFIX;
        testBuildSelectStatement(params, context.getDatabaseSystem(), expectedSelectStatment);
    }

    /**
     * Test build select with skip 0 and top.
     *
     * @throws Exception the exception
     */
    @Test
    public void testBuildSelectWithSkip0AndTop() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("$skip", "0");
        params.put("$top", "10");

        String expectedSelectStatment =
                "SELECT \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\", \"T0\".\"LOGSTART\" AS \"LOGSTART_T0\", \"T0\".\"LOGEND\" AS \"LOGEND_T0\", "
                        + "\"T0\".\"SENDER\" AS \"SENDER_T0\", \"T0\".\"RECEIVER\" AS \"RECEIVER_T0\", \"T0\".\"STATUS\" AS \"STATUS_T0\", "
                        + "\"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\" "
                        + "FROM \"MPLHEADER\" AS \"T0\" ORDER BY \"T0\".\"MESSAGEGUID\" ASC " + "FETCH FIRST 10 ROWS ONLY";
        testBuildSelectStatement(params, context.getDatabaseSystem(), expectedSelectStatment);
    }

    /**
     * Test build select with select.
     *
     * @throws Exception the exception
     */
    @Test
    public void testBuildSelectWithSelect() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("$select", "Status");
        params.put("$orderby", "Status, LogStart desc");

        String expectedSelectStatment =
                "SELECT \"T0\".\"STATUS\" AS \"STATUS_T0\", \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\" FROM \"MPLHEADER\" AS \"T0\" "
                        + "ORDER BY \"T0\".\"STATUS\" ASC, \"T0\".\"LOGSTART\" DESC" + SERVER_SIDE_PAGING_DEFAULT_SUFFIX;
        testBuildSelectStatement(params, context.getDatabaseSystem(), expectedSelectStatment);
    }

    /**
     * Test build select with select primary key.
     *
     * @throws Exception the exception
     */
    @Test
    public void testBuildSelectWithSelectPrimaryKey() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("$select", "MessageGuid");
        params.put("$orderby", "Status, LogStart desc");

        String expectedSelectStatment =
                "SELECT \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\" FROM \"MPLHEADER\" AS \"T0\" ORDER BY \"T0\".\"STATUS\" ASC, \"T0\".\"LOGSTART\" DESC"
                        + SERVER_SIDE_PAGING_DEFAULT_SUFFIX;
        testBuildSelectStatement(params, context.getDatabaseSystem(), expectedSelectStatment);
    }

    /**
     * Test build select with select attribute primary key is also selected.
     *
     * @throws Exception the exception
     */
    @Test
    public void testBuildSelectWithSelectAttribute_PrimaryKeyIsAlsoSelected() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("$select", "Status");
        params.put("$orderby", "Status, LogStart desc");

        // The primary key is always selected in addition
        String expectedSelectStatment =
                "SELECT \"T0\".\"STATUS\" AS \"STATUS_T0\", \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\" FROM \"MPLHEADER\" AS \"T0\" ORDER BY \"T0\".\"STATUS\" ASC, \"T0\".\"LOGSTART\" DESC"
                        + SERVER_SIDE_PAGING_DEFAULT_SUFFIX;
        testBuildSelectStatement(params, context.getDatabaseSystem(), expectedSelectStatment);
    }

    /**
     * Test build select statement with select top.
     *
     * @throws ODataException the o data exception
     */
    @Test
    public void testBuildSelectStatementWithSelectTop() throws ODataException {
        String expectedSelectStmnt =
                "SELECT \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\" FROM \"MPLHEADER\" AS \"T0\" ORDER BY \"T0\".\"LOGSTART\" "
                        + "DESC FETCH FIRST 12 ROWS ONLY";
        testBuildSelectStatementWithSelectTop(context.getDatabaseSystem(), 12, expectedSelectStmnt);
    }

    /**
     * Test build select statement with select top postgres.
     *
     * @throws ODataException the o data exception
     */
    @Test
    public void testBuildSelectStatementWithSelectTopPostgres() throws ODataException {
        String expectedSelectStatement =
                "SELECT \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\" FROM \"MPLHEADER\" AS \"T0\" ORDER BY \"T0\".\"LOGSTART\" DESC LIMIT 4";
        testBuildSelectStatementWithSelectTop(DatabaseSystem.POSTGRESQL, 4, expectedSelectStatement);
    }

    /**
     * Test build select statement with select top sybase.
     *
     * @throws ODataException the o data exception
     */
    @Test
    public void testBuildSelectStatementWithSelectTopH2() throws ODataException {
        String expectedSelectStatement =
                "SELECT \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\" FROM \"MPLHEADER\" AS \"T0\" ORDER BY \"T0\".\"LOGSTART\" DESC LIMIT 5";
        testBuildSelectStatementWithSelectTop(DatabaseSystem.H2, 5, expectedSelectStatement);
    }

    /**
     * Test build select statement with select top HANA.
     *
     * @throws ODataException the o data exception
     */
    @Test
    public void testBuildSelectStatementWithSelectTopHANA() throws ODataException {
        String expectedSelectStatement =
                "SELECT \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\" FROM \"MPLHEADER\" AS \"T0\" ORDER BY \"T0\".\"LOGSTART\" DESC LIMIT 3";
        testBuildSelectStatementWithSelectTop(DatabaseSystem.HANA, 3, expectedSelectStatement);
    }

    private void testBuildSelectStatementWithSelectTop(DatabaseSystem databaseSystem, Integer top, String expectedSelectStatement)
            throws ODataException {
        Map<String, String> params = new HashMap<>();
        params.put("$select", "MessageGuid");
        params.put("$orderby", "LogStart desc");
        params.put("$top", top.toString());

        testBuildSelectStatement(params, databaseSystem, expectedSelectStatement);
    }

    /**
     * Test build select statement with select top and skip HANA.
     *
     * @throws ODataException the o data exception
     */
    @Test
    public void testBuildSelectStatementWithSelectTopAndSkipHANA() throws ODataException {
        String expectedSelectStatement =
                "SELECT \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\" FROM \"MPLHEADER\" AS \"T0\" ORDER BY \"T0\".\"STATUS\" ASC, \"T0\".\"LOGSTART\" DESC LIMIT 2 OFFSET 5";
        testBuildSelectStatementWithSelectTopAndSkip(DatabaseSystem.HANA, 2, 5, expectedSelectStatement);
    }

    /**
     * Test build select statement with select top and skip sybase.
     *
     * @throws ODataException the o data exception
     */
    @Test
    public void testBuildSelectStatementWithSelectTopAndSkipH2() throws ODataException {
        String expectedSelectStatement =
                "SELECT \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\" FROM \"MPLHEADER\" AS \"T0\" ORDER BY \"T0\".\"STATUS\" ASC, \"T0\".\"LOGSTART\" DESC LIMIT 10 OFFSET 20";
        testBuildSelectStatementWithSelectTopAndSkip(DatabaseSystem.H2, 10, 20, expectedSelectStatement);
    }

    /**
     * Test build select statement with select top and skip postgre.
     *
     * @throws ODataException the o data exception
     */
    @Test
    public void testBuildSelectStatementWithSelectTopAndSkipPostgre() throws ODataException {
        String expectedSelectStatement =
                "SELECT \"T0\".\"MESSAGEGUID\" AS \"MESSAGEGUID_T0\" FROM \"MPLHEADER\" AS \"T0\" ORDER BY \"T0\".\"STATUS\" ASC, \"T0\".\"LOGSTART\" DESC LIMIT 2 OFFSET 6";
        testBuildSelectStatementWithSelectTopAndSkip(DatabaseSystem.POSTGRESQL, 2, 6, expectedSelectStatement);
    }

    private void testBuildSelectStatementWithSelectTopAndSkip(DatabaseSystem databaseSystem, Integer top, Integer skip,
            String expectedSelectStatement) throws ODataException {
        Map<String, String> params = new HashMap<>();
        params.put("$select", "MessageGuid");
        params.put("$orderby", "Status, LogStart desc");
        params.put("$skip", skip.toString());
        params.put("$top", top.toString());

        testBuildSelectStatement(params, databaseSystem, expectedSelectStatement);
    }

    private void testBuildSelectStatement(Map<String, String> uriParams, DatabaseSystem databaseSystem, String expectedSelectStatment)
            throws ODataException {
        PathSegment ps1 = createPathSegment();
        UriInfo uriInfo = uriParser.parse(Collections.singletonList(ps1), uriParams);
        SQLSelectBuilder selectBuilder = builder.buildSelectEntitySetQuery(uriInfo, null);
        SQLContext context = new SQLContext(databaseSystem);

        assertEquals(expectedSelectStatment, selectBuilder.buildSelect(context));
    }

    /**
     * Test calculate effective skip from skip and skiptoken.
     *
     * @throws ODataException the o data exception
     */
    @Test
    public void testCalculateEffectiveSkipFromSkipAndSkiptoken() throws ODataException {
        PathSegment ps1 = createPathSegment();
        Map<String, String> params = new HashMap<>();
        params.put("$skip", "3");
        params.put("$skiptoken", "5");

        UriInfo uriInfo = uriParser.parse(Collections.singletonList(ps1), params);
        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        int actualSkip = q.getSelectExpression()
                          .getSkip();

        assertEquals(8, actualSkip);
    }

    /**
     * Test calculate effective skip from skiptoken without skip.
     *
     * @throws ODataException the o data exception
     */
    @Test
    public void testCalculateEffectiveSkipFromSkiptokenWithoutSkip() throws ODataException {
        PathSegment ps1 = createPathSegment();
        Map<String, String> params = new HashMap<>();
        params.put("$skiptoken", "5");

        UriInfo uriInfo = uriParser.parse(Collections.singletonList(ps1), params);
        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        int actualSkip = q.getSelectExpression()
                          .getSkip();

        assertEquals(5, actualSkip);
    }

    /**
     * Test calculate effective skip from skip without skiptoken.
     *
     * @throws ODataException the o data exception
     */
    @Test
    public void testCalculateEffectiveSkipFromSkipWithoutSkiptoken() throws ODataException {
        PathSegment ps1 = createPathSegment();
        Map<String, String> params = new HashMap<>();
        params.put("$skip", "3");

        UriInfo uriInfo = uriParser.parse(Collections.singletonList(ps1), params);
        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        int actualSkip = q.getSelectExpression()
                          .getSkip();

        assertEquals(3, actualSkip);
    }

    /**
     * Test calculate effective skip without skip and skiptoken.
     *
     * @throws ODataException the o data exception
     */
    @Test
    public void testCalculateEffectiveSkipWithoutSkipAndSkiptoken() throws ODataException {
        PathSegment ps1 = createPathSegment();
        Map<String, String> params = new HashMap<>();

        UriInfo uriInfo = uriParser.parse(Collections.singletonList(ps1), params);
        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        int actualSkip = q.getSelectExpression()
                          .getSkip();

        assertEquals(SQLSelectClause.NOT_SET, actualSkip);
    }

    /**
     * Test calculate effective top from top in uri.
     *
     * @throws ODataException the o data exception
     */
    @Test
    public void testCalculateEffectiveTopFromTopInUri() throws ODataException {
        PathSegment ps1 = createPathSegment();
        Map<String, String> params = new HashMap<>();
        params.put("$top", "3");

        UriInfo uriInfo = uriParser.parse(Collections.singletonList(ps1), params);
        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        int actualTop = q.getSelectExpression()
                         .getTop();

        assertEquals(3, actualTop);
        assertFalse(q.isServersidePaging());
    }

    /**
     * Test calculate effective top without top in uri.
     *
     * @throws ODataException the o data exception
     */
    @Test
    public void testCalculateEffectiveTopWithoutTopInUri() throws ODataException {
        PathSegment ps1 = createPathSegment();
        Map<String, String> params = new HashMap<>();

        UriInfo uriInfo = uriParser.parse(Collections.singletonList(ps1), params);
        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        int actualTop = q.getSelectExpression()
                         .getTop();

        assertEquals(SQLQueryBuilder.DEFAULT_SERVER_PAGING_SIZE, actualTop);
        assertTrue(q.isServersidePaging());
    }

    /**
     * Test calculate effective top beyond serverside paging in uri.
     *
     * @throws ODataException the o data exception
     */
    @Test
    public void testCalculateEffectiveTopBeyondServersidePagingInUri() throws ODataException {
        PathSegment ps1 = createPathSegment();
        Map<String, String> params = new HashMap<>();
        params.put("$top", Integer.toString(SQLQueryBuilder.DEFAULT_SERVER_PAGING_SIZE + 10));

        UriInfo uriInfo = uriParser.parse(Collections.singletonList(ps1), params);
        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        int actualTop = q.getSelectExpression()
                         .getTop();

        assertEquals(SQLQueryBuilder.DEFAULT_SERVER_PAGING_SIZE, actualTop);
        assertTrue(q.isServersidePaging());
    }

    /**
     * Test select with composed key.
     *
     * @throws Exception the exception
     */
    @Test
    public void testSelectWithComposedKey() throws Exception {
        Map<String, String> params = new HashMap<>();
        PathSegment ps1 = new ODataPathSegmentImpl("Entities4(Id4_1=11,Id4_2=22)", Collections.emptyMap());
        UriInfo uriInfo = uriParser.parse(Collections.singletonList(ps1), params);

        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        SQLContext context = new SQLContext();
        String expected = "SELECT \"T0\".\"ID4_1\" AS \"ID4_1_T0\", \"T0\".\"ID4_2\" AS \"ID4_2_T0\", "
                + "\"T0\".\"ID4_3\" AS \"ID4_3_T0\" " + "FROM \"ENTITY4_TABLE\" AS \"T0\" "
                + "WHERE \"T0\".\"ID4_1\" = ? AND \"T0\".\"ID4_2\" = ? ORDER BY \"T0\".\"ID4_1\" ASC, \"T0\".\"ID4_2\" ASC "
                + "FETCH FIRST 1000 ROWS ONLY";
        assertEquals(expected, q.buildSelect(context));
    }

    /**
     * Test select with parameters for entity when calc view and hana db.
     *
     * @throws Exception the exception
     */
    @Test
    public void testSelectWithParametersForEntityWhenCalcViewAndHanaDb() throws Exception {
        Map<String, String> params = new HashMap<>();
        PathSegment ps = new ODataPathSegmentImpl("Entities6(CurrentEmployeeId=1,CurrentEmployeeName='Ben',ID=3)", Collections.emptyMap());
        UriInfo uriInfo = uriParser.parse(Collections.singletonList(ps), params);

        SQLSelectBuilder q = builder.buildSelectEntityQuery(uriInfo, null);
        SQLContext context = new SQLContext(DatabaseSystem.HANA);

        List<String> selectSql = Arrays.asList(q.buildSelect(context)
                                                .split("SELECT|FROM|WHERE"));

        List<String> selectedColumns = Arrays.asList(selectSql.get(1)
                                                              .trim()
                                                              .split(", "));
        assertTrue(selectedColumns.contains("\"T0\".\"ID\" AS \"ID_T0\""));
        assertTrue(selectedColumns.contains("\"T0\".\"NAME\" AS \"NAME_T0\""));
        assertTrue(selectedColumns.contains("? AS CurrentEmployeeId_T0"));
        assertTrue(selectedColumns.contains("? AS CurrentEmployeeName_T0"));

        List<String> targetDbEntity = Arrays.asList(selectSql.get(2)
                                                             .split("\\(|\\)"));
        String targetDbEntityName = targetDbEntity.get(0)
                                                  .trim();
        assertEquals("\"ENTITY6_TABLE\"", targetDbEntityName);
        List<String> targetDbEntityParameters = Arrays.asList(targetDbEntity.get(1)
                                                                            .split(", "));
        assertTrue(targetDbEntityParameters.contains("placeholder.\"$$CurrentEmployeeId$$\" => ?"));
        assertTrue(targetDbEntityParameters.contains("placeholder.\"$$CurrentEmployeeName$$\" => ?"));
        String targetDbEntityAlias = targetDbEntity.get(2)
                                                   .trim();
        assertEquals("AS \"T0\"", targetDbEntityAlias);

        String whereClause = selectSql.get(3);
        assertEquals("\"T0\".\"ID\" = ?", whereClause.trim());
    }

    /**
     * Test select with parameters for entity set when calc view and hana db.
     *
     * @throws Exception the exception
     */
    @Test
    public void testSelectWithParametersForEntitySetWhenCalcViewAndHanaDb() throws Exception {
        Map<String, String> params = new HashMap<>();
        PathSegment ps = new ODataPathSegmentImpl("Entities6(CurrentEmployeeId=1,CurrentEmployeeName='Ben',ID=3)", Collections.emptyMap());
        UriInfo uriInfo = uriParser.parse(Collections.singletonList(ps), params);

        SQLSelectBuilder q = builder.buildSelectEntitySetQuery(uriInfo, null);
        SQLContext context = new SQLContext(DatabaseSystem.HANA);

        List<String> selectSql = Arrays.asList(q.buildSelect(context)
                                                .split("SELECT|FROM|WHERE|ORDER BY|LIMIT"));

        List<String> selectedColumns = Arrays.asList(selectSql.get(1)
                                                              .trim()
                                                              .split(", "));
        assertTrue(selectedColumns.contains("\"T0\".\"ID\" AS \"ID_T0\""));
        assertTrue(selectedColumns.contains("\"T0\".\"NAME\" AS \"NAME_T0\""));
        assertTrue(selectedColumns.contains("? AS CurrentEmployeeId_T0"));
        assertTrue(selectedColumns.contains("? AS CurrentEmployeeName_T0"));

        List<String> targetDbEntity = Arrays.asList(selectSql.get(2)
                                                             .split("\\(|\\)"));
        String targetDbEntityName = targetDbEntity.get(0)
                                                  .trim();
        assertEquals("\"ENTITY6_TABLE\"", targetDbEntityName);
        List<String> targetDbEntityParameters = Arrays.asList(targetDbEntity.get(1)
                                                                            .split(", "));
        assertTrue(targetDbEntityParameters.contains("placeholder.\"$$CurrentEmployeeId$$\" => ?"));
        assertTrue(targetDbEntityParameters.contains("placeholder.\"$$CurrentEmployeeName$$\" => ?"));
        String targetDbEntityAlias = targetDbEntity.get(2)
                                                   .trim();
        assertEquals("AS \"T0\"", targetDbEntityAlias);

        String whereClause = selectSql.get(3);
        assertEquals("\"T0\".\"ID\" = ?", whereClause.trim());

        String orderByClause = selectSql.get(4);
        assertEquals("CurrentEmployeeId_T0 ASC, CurrentEmployeeName_T0 ASC, \"T0\".\"ID\" ASC", orderByClause.trim());

        String limitClause = selectSql.get(5);
        assertEquals("1000", limitClause.trim());
    }

    /**
     * Test delete with composed key.
     *
     * @throws Exception the exception
     */
    @Test
    public void testDeleteWithComposedKey() throws Exception {
        Configuration.set("DIRIGIBLE_DATABASE_NAMES_CASE_SENSITIVE", "false");
        Map<String, String> params = new HashMap<>();
        PathSegment ps1 = new ODataPathSegmentImpl("Entities4(Id4_1=11,Id4_2=22)", Collections.emptyMap());
        UriInfo uriInfo = uriParser.parse(Collections.singletonList(ps1), params);

        SQLDeleteBuilder deleteBuilder = builder.buildDeleteEntityQuery(uriInfo, mapKeys(uriInfo.getKeyPredicates()), null);
        SQLContext context = new SQLContext();
        String expected = "DELETE FROM \"ENTITY4_TABLE\" WHERE \"ID4_1\"=? AND \"ID4_2\"=?";
        assertEquals(expected, deleteBuilder.build(context)
                                            .sql());
    }

    /**
     * Test insert with composed key.
     *
     * @throws Exception the exception
     */
    @Test
    public void testInsertWithComposedKey() throws Exception {
        Map<String, String> params = new HashMap<>();
        PathSegment ps1 = new ODataPathSegmentImpl("Entities4(Id4_1=11,Id4_2=22)", Collections.emptyMap());
        UriInfo uriInfo = uriParser.parse(Collections.singletonList(ps1), params);

        Map<String, Object> entity4 = new HashMap<>();
        entity4.put("Id4_1", "1");
        entity4.put("Id4_2", "2");
        ODataEntry entity = new ODataEntryImpl(entity4, null, null, null);

        SQLInsertBuilder insertBuilder2 = builder.buildInsertEntityQuery(uriInfo, entity, null);
        String expected = "INSERT INTO \"ENTITY4_TABLE\" (\"ID4_1\",\"ID4_2\") VALUES (?,?)";
        assertEquals(expected, insertBuilder2.build(context)
                                             .sql());
    }

    /**
     * Map keys.
     *
     * @param keys the keys
     * @return the map
     * @throws EdmException the edm exception
     */
    private static Map<String, Object> mapKeys(final List<KeyPredicate> keys) throws EdmException {
        Map<String, Object> keyMap = new HashMap<>();
        for (final KeyPredicate key : keys) {
            final EdmProperty property = key.getProperty();
            final EdmSimpleType type = (EdmSimpleType) property.getType();
            keyMap.put(property.getName(),
                    type.valueOfString(key.getLiteral(), EdmLiteralKind.DEFAULT, property.getFacets(), type.getDefaultType()));
        }
        return keyMap;
    }

}
