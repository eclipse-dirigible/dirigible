/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.binding;

import org.apache.olingo.odata2.annotation.processor.core.edm.AnnotationEdmProvider;
import org.apache.olingo.odata2.core.edm.provider.EdmImplProv;
import org.eclipse.dirigible.engine.odata2.sql.edm.Entity1;
import org.eclipse.dirigible.engine.odata2.sql.edm.Entity2;
import org.eclipse.dirigible.engine.odata2.sql.edm.Entity3;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import static org.eclipse.dirigible.engine.odata2.sql.test.util.OData2TestUtils.stream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The Class EdmTableBindingFactoryTest.
 */
@SuppressWarnings("unchecked")
public class EdmTableBindingFactoryTest {

    /** The edm. */
    private EdmImplProv edm;

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
        AnnotationEdmProvider provider = new AnnotationEdmProvider(Arrays.asList(classes));
        edm = new EdmImplProv(provider);
    }

    /**
     * Test mpl binding.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testMplBinding() throws IOException {
        try (InputStream stream = stream(Entity1.class)) {
            Map<String, Object> config = new EdmTableBindingFactory().loadTableBindings(stream);
            assertEquals("MPLHEADER", config.get("sqlTable"));
            assertEquals("STATUS", config.get("Status"));
            assertEquals("MESSAGEGUID", config.get("MessageGuid"));
        }
    }

    /**
     * Test reference 1.
     *
     * @throws Exception the exception
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testReference1() throws Exception {
        try (InputStream stream = stream(Entity2.class)) {
            Map<String, Object> config = new EdmTableBindingFactory().loadTableBindings(stream);
            assertEquals("NAME", config.get("Name"));
            assertEquals("VALUE", config.get("Value"));
            assertTrue("complex column definition not correctly read!", config.get("Id") instanceof Map);
            assertEquals("ID", ((Map) config.get("Id")).get("name"));
            assertEquals("NUMERIC", ((Map) config.get("Id")).get("sqlType"));
            assertEquals(Arrays.asList("HEADER_ID"), ((Map<String, Object>) config.get("_ref_Entity1")).get("joinColumn"));
            assertEquals("ID", config.get("_pk_"));
        }
    }


    /**
     * Test reference 2.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testReference2() throws IOException {
        try (InputStream stream = stream(Entity1.class)) {
            Map<String, Object> config = new EdmTableBindingFactory().loadTableBindings(stream);
            assertEquals(Arrays.asList("ID"), ((Map<String, Object>) config.get("_ref_Entity2")).get("joinColumn"));
        }
    }

    /**
     * Test table mapping.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testTableMapping() throws IOException {
        try (InputStream stream = stream(Entity1.class)) {
            Map<String, Object> config = new EdmTableBindingFactory().loadTableBindings(stream);
            assertEquals(Arrays.asList("ID"), ((Map<String, Object>) config.get("_ref_Entity2")).get("joinColumn"));
        }
    }

    /**
     * Test create table bindings.
     *
     * @throws Exception the exception
     */
    @Test
    public void testCreateTableBindings() throws Exception {
        try (InputStream stream = stream(Entity1.class)) {
            EdmTableBinding config = new EdmTableBindingFactory().createTableBinding(stream);
            assertEquals("STATUS", config.getColumnName("Status"));
        }
    }

    /**
     * Test get PK.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGetPK() throws Exception {
        try (InputStream stream = stream(Entity1.class)) {
            EdmTableBinding config = new EdmTableBindingFactory().createTableBinding(stream);
            assertEquals("ID", config.getPrimaryKey());
        }
    }

    /**
     * Test get join column entity 2.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGetJoinColumnEntity2() throws Exception {
        try (InputStream stream = stream(Entity1.class)) {
            EdmTableBinding config = new EdmTableBindingFactory().createTableBinding(stream);
            assertEquals(Arrays.asList("ID"), config.getJoinColumnTo(edm.getEntityType(Entity2.class.getPackage()
                                                                                                    .getName(),
                    Entity2.class.getSimpleName())));
        }
    }

    /**
     * Test get data structure type.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGetDataStructureType() throws Exception {
        try (InputStream stream = stream(Entity1.class)) {
            EdmTableBinding config = new EdmTableBindingFactory().createTableBinding(stream);
            assertEquals(EdmTableBinding.DataStructureType.TABLE, config.getDataStructureType());
        }
    }

    /**
     * Test get parameters.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGetParameters() throws Exception {
        try (InputStream stream = stream(Entity1.class)) {
            EdmTableBinding config = new EdmTableBindingFactory().createTableBinding(stream);
            assertEquals(Arrays.asList(), config.getParameters());
        }
    }
}
