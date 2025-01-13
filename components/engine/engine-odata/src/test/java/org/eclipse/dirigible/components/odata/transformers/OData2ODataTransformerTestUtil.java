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

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraintForeignKey;
import org.eclipse.dirigible.components.odata.domain.OData;
import org.eclipse.dirigible.components.odata.factory.ODataDefinitionFactoryTest;
import org.eclipse.dirigible.components.odata.synchronizer.ODataSynchronizer;

/**
 * The Class OData2ODataTransformerTestUtil.
 */
public class OData2ODataTransformerTestUtil {

    /**
     * Load data test transform entity property.
     *
     * @param odataDatabaseMetadataUtil the db metadata util
     * @return the o data definition
     * @throws SQLException the SQL exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static OData loadData_testTransformEntityProperty(ODataDatabaseMetadataUtil odataDatabaseMetadataUtil)
            throws SQLException, IOException {
        byte[] orders = IOUtils.toByteArray(ODataDefinitionFactoryTest.class.getResourceAsStream("/entityproperty/Entities.odata"));
        OData definition = ODataSynchronizer.parseOData("/entityproperty/Entities.odata", orders);

        Table model = new Table("ENTITY1");
        new TableColumn("ENTITY1ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("PROPERTY2", "Edm.String", "20", model);
        new TableColumn("PROPERTY3", "Edm.String", "20", model);
        new TableColumn("Country.Id", "Edm.String", "0", model);

        when(odataDatabaseMetadataUtil.getTableMetadata("ENTITY1", null)).thenReturn(model);

        model = new Table("ENTITY2");
        new TableColumn("ENTITY2ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("PROPERTY2", "Edm.Int32", "0", model);
        new TableColumn("PROPERTY3", "Edm.String", "0", model);
        new TableColumn("PROPERTY4_5", "Edm.Int32", "0", model);
        new TableColumn("ENTITY1ENTITY1ID", "Edm.Int32", "0", model);
        new TableConstraintForeignKey("ENTITY1ID", "ENTITY1ENTITY1ID", "ENTITY1ID", null, model.getConstraints());

        when(odataDatabaseMetadataUtil.getTableMetadata("ENTITY2", null)).thenReturn(model);

        model = new Table("ENTITY3");
        new TableColumn("ENTITY3.ID", "Edm.Int32", "0", true, true, model);
        new TableColumn("NAME.ID", "Edm.String", "0", model);

        when(odataDatabaseMetadataUtil.getTableMetadata("ENTITY3", null)).thenReturn(model);

        return definition;
    }
}
