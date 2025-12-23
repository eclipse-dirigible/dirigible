/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.data.store.hbm.EntityToHbmMapper;
import org.eclipse.dirigible.components.data.store.hbm.HbmXmlDescriptor;
import org.eclipse.dirigible.components.data.store.model.EntityFieldMetadata;
import org.eclipse.dirigible.components.data.store.model.EntityFieldMetadata.ColumnDetails;
import org.eclipse.dirigible.components.data.store.model.EntityMetadata;
import org.eclipse.dirigible.components.data.store.parser.EntityParser;
import org.junit.jupiter.api.Test;

public class TypeScriptClassParserTest {

    @Test
    public void parserTest() throws IOException {
        String carTsCode = IOUtils.toString(DataStoreTest.class.getResourceAsStream("/typescript/CarEntity.ts"), StandardCharsets.UTF_8);

        EntityParser parser = new EntityParser();
        EntityMetadata metadata = parser.parse("/typescript/CarEntity.ts", carTsCode);

        System.out.println("--- Extracted Entity Metadata ---");
        System.out.println("Entity Name: " + metadata.getEntityName());
        System.out.println("Table Name: " + metadata.getTableName());
        System.out.println("---------------------------------");

        for (EntityFieldMetadata field : metadata.getFields()) {
            System.out.printf("Property: %s (%s)\n", field.getPropertyName(), field.getTypeScriptType());
            System.out.printf("  Is ID: %s\n", field.isIdentifier());
            if (field.getGenerationStrategy() != null) {
                System.out.printf("  Strategy: %s\n", field.getGenerationStrategy());
            }
            if (field.getColumnDetails() != null) {
                ColumnDetails cd = field.getColumnDetails();
                System.out.printf("  Column: %s, Type: %s, Length: %s, Nullable: %s, Default: %s\n",
                        cd.getColumnName() != null ? cd.getColumnName() : field.getPropertyName(), cd.getDatabaseType(),
                        cd.getLength() != null ? cd.getLength()
                                                   .toString()
                                : "N/A",
                        cd.isNullable(), cd.getDefaultValue());
            }
            System.out.println();
        }

        assertEquals("CarEntity", metadata.getEntityName());
        assertEquals("CARS", metadata.getTableName());

        assertEquals(4, metadata.getFields()
                                .size());

        assertEquals("id", metadata.getFields()
                                   .get(0)
                                   .getPropertyName());
        assertEquals("car_id", metadata.getFields()
                                       .get(0)
                                       .getColumnDetails()
                                       .getColumnName());
    }

    @Test
    public void mapperTest() throws IOException {
        String carTsCode = IOUtils.toString(DataStoreTest.class.getResourceAsStream("/typescript/CarEntity.ts"), StandardCharsets.UTF_8);

        EntityParser parser = new EntityParser();
        EntityMetadata metadata = parser.parse("/typescript/CarEntity.ts", carTsCode);
        assertEquals("CarEntity", metadata.getEntityName());
        HbmXmlDescriptor hbm = EntityToHbmMapper.map(metadata);
        System.out.println("--- Extracted Entity Metadata as HBM XML ---");
        System.out.println(hbm.serialize());

    }

    @Test
    public void mapperBagTest() throws IOException {
        String carTsCode = IOUtils.toString(DataStoreTest.class.getResourceAsStream("/typescript/OrderEntity.ts"), StandardCharsets.UTF_8);

        EntityParser parser = new EntityParser();
        EntityMetadata metadata = parser.parse("/typescript/OrderEntity.ts", carTsCode);
        assertEquals("Order", metadata.getEntityName());
        HbmXmlDescriptor hbm = EntityToHbmMapper.map(metadata);
        System.out.println("--- Extracted Entity Metadata as HBM XML ---");
        System.out.println(hbm.serialize());

    }

}
