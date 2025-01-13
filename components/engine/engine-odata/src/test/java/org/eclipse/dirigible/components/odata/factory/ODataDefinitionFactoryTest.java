/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.odata.domain.OData;
import org.eclipse.dirigible.components.odata.synchronizer.ODataSynchronizer;
import org.junit.jupiter.api.Test;

/**
 * The Class ODataDefinitionFactoryTest.
 */
public class ODataDefinitionFactoryTest {

    /**
     * Test simple.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testSimple() throws IOException {
        byte[] cars = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/cars/Cars.odata"));
        OData definition = ODataSynchronizer.parseOData("/cars/Cars.odata", cars);
        assertEquals(2, definition.getEntities()
                                  .size());
        assertNotNull(definition.getEntities()
                                .get(0));
        assertEquals("CAR", definition.getEntities()
                                      .get(0)
                                      .getTable());
    }

    /**
     * Test navigation.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testNavigation() throws IOException {
        byte[] cars = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/orders/Orders.odata"));
        OData definition = ODataSynchronizer.parseOData("/orders/Orders.odata", cars);
        assertEquals(2, definition.getEntities()
                                  .size());
        assertNotNull(definition.getEntities()
                                .get(0));
        assertEquals("ORDERS", definition.getEntities()
                                         .get(0)
                                         .getTable());
        assertEquals(1, definition.getEntities()
                                  .get(0)
                                  .getNavigations()
                                  .size());
        assertEquals("OrderItems", definition.getEntities()
                                             .get(0)
                                             .getNavigations()
                                             .get(0)
                                             .getAssociation());
        assertEquals(1, definition.getAssociations()
                                  .size());
        assertEquals("OrderItems", definition.getAssociations()
                                             .get(0)
                                             .getName());
    }

    /**
     * Test handlers.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Test
    public void testHandlers() throws IOException {
        byte[] def = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/def/OData1.odata"));
        OData definition = ODataSynchronizer.parseOData("/def/OData1.odata", def);
        assertEquals(1, definition.getEntities()
                                  .size());
        assertNotNull(definition.getEntities()
                                .get(0));
        assertEquals("ENTITY1", definition.getEntities()
                                          .get(0)
                                          .getTable());
        assertEquals(4, definition.getEntities()
                                  .get(0)
                                  .getHandlers()
                                  .size());
        assertEquals("create", definition.getEntities()
                                         .get(0)
                                         .getHandlers()
                                         .get(0)
                                         .getMethod());
        assertEquals("before", definition.getEntities()
                                         .get(0)
                                         .getHandlers()
                                         .get(0)
                                         .getType());
        assertEquals("/test1/myhandler", definition.getEntities()
                                                   .get(0)
                                                   .getHandlers()
                                                   .get(0)
                                                   .getHandler());
    }

}
