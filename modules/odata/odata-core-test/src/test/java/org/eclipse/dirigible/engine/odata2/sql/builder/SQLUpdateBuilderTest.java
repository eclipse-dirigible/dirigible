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
import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmStructuralType;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.core.edm.provider.EdmImplProv;
import org.apache.olingo.odata2.core.ep.entry.ODataEntryImpl;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatement;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatementParam;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBindingProvider;
import org.eclipse.dirigible.engine.odata2.sql.edm.*;
import org.eclipse.dirigible.engine.odata2.sql.mapping.DefaultEdmTableMappingProvider;
import org.eclipse.dirigible.engine.odata2.sql.test.util.OData2TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The Class SQLUpdateBuilderTest.
 */
public final class SQLUpdateBuilderTest {

    /** The provider. */
    AnnotationEdmProvider provider;

    /** The edm. */
    EdmImplProv edm;

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
                Entity3.class, //
                Entity4.class, //
                Entity5.class};
        provider = new AnnotationEdmProvider(Arrays.asList(classes));
        edm = new EdmImplProv(provider);
        tableMappingProvider = new DefaultEdmTableMappingProvider(OData2TestUtils.resources(classes));
    }

    /**
     * Test update expression.
     *
     * @throws Exception the exception
     */
    @Test
    public void testUpdateExpression() throws Exception {
        SQLUpdateBuilder update = createUpdateExpression();
        assertNotNull(update);
        SQLStatement expression = update.build(null);
        String result = expression.sql();
        Assert.assertEquals("UPDATE ENTITY4_TABLE SET ID4_3=? WHERE ID4_1=? AND ID4_2=?", result);

        List<SQLStatementParam> params = expression.getStatementParams();
        // the values must be ordered as the elements in the query
        assertEquals("UpdateIt", params.get(0)
                                       .getValue());
        assertEquals("1", params.get(1)
                                .getValue());
        assertEquals("2", params.get(2)
                                .getValue());
    }

    /**
     * Creates the update expression.
     *
     * @return the SQL update builder
     * @throws ODataException the o data exception
     */
    private SQLUpdateBuilder createUpdateExpression() throws ODataException {
        EdmEntityType type = edm.getEntityType(Entity4.class.getPackage()
                                                            .getName(),
                Entity4.class.getSimpleName());
        final Map<String, Object> keys = new HashMap<>();
        keys.put("Id4_1", "1");
        keys.put("Id4_2", "2");

        final Map<String, Object> data = new HashMap<>(keys);
        data.put("Id4_3", "UpdateIt");
        ODataEntry entry = new ODataEntryImpl(data, null, null, null);

        SQLUpdateBuilder builder = new SQLUpdateBuilder(tableMappingProvider, keys) {
            protected boolean isUpdateTarget(final EdmStructuralType target) {
                return true;
            }
        };
        builder.update(type, entry);
        return builder;
    }
}
