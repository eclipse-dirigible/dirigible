/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.clause;

import org.apache.olingo.odata2.annotation.processor.core.edm.AnnotationEdmProvider;
import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.core.edm.provider.EdmImplProv;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBindingProvider;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLContext;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLQueryBuilder;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLQueryTestUtils;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLSelectBuilder;
import org.eclipse.dirigible.engine.odata2.sql.edm.Entity1;
import org.eclipse.dirigible.engine.odata2.sql.edm.Entity2;
import org.eclipse.dirigible.engine.odata2.sql.edm.Entity3;
import org.eclipse.dirigible.engine.odata2.sql.mapping.DefaultEdmTableMappingProvider;
import org.eclipse.dirigible.engine.odata2.sql.test.util.OData2TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * The Class SQLJoinClauseTest.
 */
public class SQLJoinClauseTest {

    /** The provider. */
    AnnotationEdmProvider provider;

    /** The edm. */
    EdmImplProv edm;

    /** The builder. */
    SQLQueryBuilder builder;

    /** The context. */
    SQLContext context;

    /** The table mapping provider. */
    EdmTableBindingProvider tableMappingProvider;

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
                Entity3.class //
        };
        provider = new AnnotationEdmProvider(Arrays.asList(classes));
        edm = new EdmImplProv(provider);
        tableMappingProvider = new DefaultEdmTableMappingProvider(OData2TestUtils.resources(classes));
        builder = new SQLQueryBuilder(tableMappingProvider);
        context = new SQLContext();
    }

    /**
     * Test simple join with case sensitive names.
     *
     * @throws Exception the exception
     */
    @Test
    public void testSimpleJoinWithCaseSensitiveNames() throws Exception {
        testSimpleJoin("LEFT JOIN \"MPLHEADER\" AS \"T0\" ON \"T0\".\"ID\" = \"T1\".\"HEADER_ID\"");
    }

    /**
     * Test simple join.
     *
     * @param caseSensitiveNames the case sensitive names
     * @param expectedJoinStatement the expected join statement
     * @throws Exception the exception
     */
    public void testSimpleJoin(String expectedJoinStatement) throws Exception {
        EdmEntityType mpl = edm.getEntityType(Entity1.class.getPackage()
                                                           .getName(),
                Entity1.class.getSimpleName());
        EdmEntityType uda = edm.getEntityType(Entity2.class.getPackage()
                                                           .getName(),
                Entity2.class.getSimpleName());
        SQLSelectBuilder noop = new SQLSelectBuilder(tableMappingProvider);

        SQLJoinClause join = new SQLJoinClause(noop, mpl, uda);
        // the start condition will be registered as T0 of from and T1 for to
        SQLQueryTestUtils.grantTableAliasForStructuralTypeInQuery(noop, mpl);
        SQLQueryTestUtils.grantTableAliasForStructuralTypeInQuery(noop, uda);
        // Use the left join to tolerate null elements
        assertEquals(expectedJoinStatement, join.evaluate(context));

    }

    /**
     * Test simple join flipped because of mapping.
     *
     * @throws Exception the exception
     */
    @Test
    public void testSimpleJoinFlippedBecauseOfMapping() throws Exception {
        EdmEntityType mpl = edm.getEntityType(Entity1.class.getPackage()
                                                           .getName(),
                Entity1.class.getSimpleName());
        EdmEntityType uda = edm.getEntityType(Entity2.class.getPackage()
                                                           .getName(),
                Entity2.class.getSimpleName());
        SQLSelectBuilder noop = new SQLSelectBuilder(tableMappingProvider);

        SQLJoinClause join = new SQLJoinClause(noop, uda, mpl);
        SQLQueryTestUtils.grantTableAliasForStructuralTypeInQuery(noop, uda);
        assertEquals("LEFT JOIN ITOP_MPLUSERDEFINEDATTRIBUTE AS T0 ON T0.HEADER_ID = T1.ID", join.evaluate(context));
    }

    /**
     * Test no need for join from and to are the same.
     *
     * @throws Exception the exception
     */
    @Test
    public void testNoNeedForJoin_fromAndToAreTheSame() throws Exception {
        EdmEntityType from = edm.getEntityType(Entity1.class.getPackage()
                                                            .getName(),
                Entity1.class.getSimpleName());
        SQLSelectBuilder noop = new SQLSelectBuilder(tableMappingProvider);
        SQLJoinClause join = new SQLJoinClause(noop, from, from);
        assertEquals("", join.evaluate(context));
    }

    /**
     * Test surround with double quotes no need.
     */
    @Test
    public void testSurroundWithDoubleQuotes_noNeed() {
        SQLJoinClause join = createJoinClouse();
        String TABLE_NAME = "\"MY_TABLE\"";
        String surroundedValue = join.surroundWithDoubleQuotes(TABLE_NAME);
        assertEquals("Unexpected escaped/surrounded with double quotes value", TABLE_NAME, surroundedValue);
    }

    /**
     * Test surround with double quotes.
     */
    @Test
    public void testSurroundWithDoubleQuotes() {
        SQLJoinClause join = createJoinClouse();
        String TABLE_NAME = "MY_TABLE";
        String surroundedValue = join.surroundWithDoubleQuotes(TABLE_NAME);
        String exoectedValue = "\"" + TABLE_NAME + "\"";
        assertEquals("Unexpected escaped/surrounded with double quotes value", exoectedValue, surroundedValue);
    }

    /**
     * Creates the join clouse.
     *
     * @return the SQL join clause
     */
    private SQLJoinClause createJoinClouse() {
        EdmEntityType entityType1 = Mockito.mock(EdmEntityType.class);
        EdmEntityType entityType2 = Mockito.mock(EdmEntityType.class);
        return new SQLJoinClause(null, entityType1, entityType2);
    }
}
