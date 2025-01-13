/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.transformers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.odata.api.ODataHandlerMethods;
import org.eclipse.dirigible.components.odata.api.ODataHandlerTypes;
import org.eclipse.dirigible.components.odata.domain.OData;
import org.eclipse.dirigible.components.odata.domain.ODataHandler;
import org.eclipse.dirigible.components.odata.factory.ODataDefinitionFactoryTest;
import org.eclipse.dirigible.components.odata.synchronizer.ODataSynchronizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The Class OData2ODataHTransformerTest.
 */
@ExtendWith(MockitoExtension.class)
public class OData2ODataHTransformerTest {

    /** The odata 2 O data H transformer. */
    @InjectMocks
    OData2ODataHTransformer odata2ODataHTransformer;

    /**
     * Test transform.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransform() throws IOException, SQLException {
        byte[] employee =
                IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeWithHandlers.odata"));
        OData definition = ODataSynchronizer.parseOData("/transformers/EmployeeWithHandlers.odata", employee);

        List<ODataHandler> actualResult = odata2ODataHTransformer.transform(definition);

        assertEquals(actualResult.size(), 4);
        assertEquals("employeeType", actualResult.get(0)
                                                 .getName());
        assertEquals("np", actualResult.get(0)
                                       .getNamespace());
        assertEquals(ODataHandlerMethods.create.name(), actualResult.get(0)
                                                                    .getMethod());
        assertEquals(ODataHandlerTypes.before.name(), actualResult.get(0)
                                                                  .getKind());
        assertEquals("/test1/myhandler", actualResult.get(0)
                                                     .getHandler());

        assertEquals("employeeType", actualResult.get(1)
                                                 .getName());
        assertEquals("np", actualResult.get(1)
                                       .getNamespace());
        assertEquals(ODataHandlerMethods.update.name(), actualResult.get(1)
                                                                    .getMethod());
        assertEquals(ODataHandlerTypes.after.name(), actualResult.get(1)
                                                                 .getKind());
        assertEquals("/test2/myhandler", actualResult.get(1)
                                                     .getHandler());

        assertEquals("employeeType", actualResult.get(2)
                                                 .getName());
        assertEquals("np", actualResult.get(2)
                                       .getNamespace());
        assertEquals(ODataHandlerMethods.delete.name(), actualResult.get(2)
                                                                    .getMethod());
        assertEquals(ODataHandlerTypes.on.name(), actualResult.get(2)
                                                              .getKind());
        assertEquals("/test3/myhandler", actualResult.get(2)
                                                     .getHandler());

        assertEquals("employeeType", actualResult.get(3)
                                                 .getName());
        assertEquals("np", actualResult.get(3)
                                       .getNamespace());
        assertEquals(ODataHandlerMethods.delete.name(), actualResult.get(3)
                                                                    .getMethod());
        assertEquals(ODataHandlerTypes.forbid.name(), actualResult.get(3)
                                                                  .getKind());
    }

    /**
     * Test transform with incorrect O data handler type.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SQLException the SQL exception
     */
    @Test
    public void testTransformWithIncorrectODataHandlerType() throws IOException, SQLException {
        try {
            byte[] employee = IOUtils.toByteArray(
                    ODataDefinitionFactoryTest.class.getResourceAsStream("/transformers/EmployeeWithWrongHandler.odata"));
            OData definition = ODataSynchronizer.parseOData("/transformers/EmployeeWithWrongHandler.odata", employee);
            odata2ODataHTransformer.transform(definition);
        } catch (Exception e) {
            assertTrue(e instanceof OData2TransformerException);
        }
    }
}
