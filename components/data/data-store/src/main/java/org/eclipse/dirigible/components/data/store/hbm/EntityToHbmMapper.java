/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.hbm;

import org.eclipse.dirigible.components.data.store.hbm.HbmXmlDescriptor.HbmIdDescriptor;
import org.eclipse.dirigible.components.data.store.model.EntityFieldMetadata;
import org.eclipse.dirigible.components.data.store.model.EntityFieldMetadata.AssociationDetails;
import org.eclipse.dirigible.components.data.store.model.EntityFieldMetadata.CollectionDetails;
import org.eclipse.dirigible.components.data.store.model.EntityFieldMetadata.ColumnDetails;
import org.eclipse.dirigible.components.data.store.model.EntityMetadata;

/**
 * The Class EntityToHbmMapper.
 */
public class EntityToHbmMapper {

    /**
     * Converts EntityMetadata (typically parsed from TypeScript) into an HbmXmlDescriptor.
     *
     * @param entityMetadata the entity metadata
     * @return the hbm xml descriptor
     */
    public static HbmXmlDescriptor map(EntityMetadata entityMetadata) {

        EntityFieldMetadata idField = entityMetadata.getFields()
                                                    .stream()
                                                    .filter(EntityFieldMetadata::isIdentifier)
                                                    .findFirst()
                                                    .orElseThrow(() -> new IllegalArgumentException(
                                                            "Entity must have an @Id field.\n" + entityMetadata.getEntityName()));

        ColumnDetails idCd = idField.getColumnDetails();
        HbmIdDescriptor idDesc = new HbmIdDescriptor(idField.getPropertyName(),
                // Use ColumnDetails name, falling back to property name if null
                idField.getColumnDetails()
                       .getColumnName() != null ? idField.getColumnDetails()
                                                         .getColumnName()
                               : idField.getPropertyName()
                                        .toUpperCase(),
                mapType(idField.getTypeScriptType(), idCd.getDatabaseType()), mapGenerationStrategy(idField.getGenerationStrategy()));

        String entityName = entityMetadata.getEntityName();
        String tableName = entityMetadata.getTableName();

        // Fallback for tableName
        if (tableName == null || tableName.isEmpty()) {
            tableName = entityMetadata.getEntityName() != null ? entityMetadata.getEntityName()
                                                                               .toUpperCase()
                    : entityName.toUpperCase();
        }

        HbmXmlDescriptor hbmDesc = new HbmXmlDescriptor(entityName, tableName, idDesc);

        entityMetadata.getFields()
                      .stream()
                      .filter(f -> !f.isIdentifier()) // Exclude the ID field, as it's already mapped
                      .forEach(field -> {
                          if (field.isCollection()) {
                              CollectionDetails cd = field.getCollectionDetails();
                              String mappedName = cd.getName() != null ? cd.getName() : field.getPropertyName();
                              HbmXmlDescriptor.HbmCollectionDescriptor collDesc = new HbmXmlDescriptor.HbmCollectionDescriptor(mappedName,
                                      cd.getTableName(), cd.getJoinColumn(), cd.getEntityName(), cd.isInverse(), cd.isLazy(), cd.getFetch(),
                                      cd.getCascade(), cd.isJoinColumnNotNull());
                              hbmDesc.addCollection(collDesc);
                          } else if (field.isAssociation()) {
                              AssociationDetails ad = field.getAssociationDetails();
                              String mappedName = ad.getName() != null ? ad.getName() : field.getPropertyName();
                              HbmXmlDescriptor.HbmAssociationDescriptor assDesc = new HbmXmlDescriptor.HbmAssociationDescriptor(mappedName,
                                      ad.getEntityName(), ad.getJoinColumn(), ad.getCascade(), ad.isNotNull(), ad.getLazy());
                              hbmDesc.addAssociation(assDesc);
                          } else {
                              ColumnDetails cd = field.getColumnDetails();

                              String mappedColumnName = cd.getColumnName() != null ? cd.getColumnName()
                                      : field.getPropertyName()
                                             .toUpperCase();

                              HbmXmlDescriptor.HbmPropertyDescriptor propDesc = new HbmXmlDescriptor.HbmPropertyDescriptor(
                                      field.getPropertyName(), mappedColumnName, mapType(field.getTypeScriptType(), cd.getDatabaseType()),
                                      cd.getLength(), cd.isNullable(), cd.getDefaultValue(), cd.getPrecision(), cd.getScale());
                              hbmDesc.addProperty(propDesc);
                          }
                      });

        return hbmDesc;
    }

    /**
     * Mapping from TypeScript types and Database type hints to Hibernate HBM types.
     *
     * @param tsType the ts type
     * @param dbType the db type
     * @return the string
     */
    private static String mapType(String tsType, String dbType) {
        // Clean up the TypeScript type (remove '| null' for easier matching)
        String cleanTsType = tsType.toLowerCase()
                                   .replace(" | null", "")
                                   .replace(" | undefined", "")
                                   .trim();
        String cleanDbType = (dbType != null) ? dbType.toLowerCase()
                                                      .trim()
                : "";

        if (!cleanDbType.isEmpty()) {
            return switch (cleanDbType) {
                // String/Text Types
                case "varchar", "nvarchar", "char", "text", "ntext" -> "string";
                // Integer Types
                case "tinyint", "smallint" -> "short";
                case "int", "integer" -> "integer";
                case "bigint", "long" -> "long";
                // Floating Point/Decimal Types
                case "float", "real" -> "float";
                case "double", "double precision" -> "double";
                case "numeric", "decimal", "money", "currency" -> "big_decimal";
                // Boolean Type
                case "boolean" -> "boolean";
                case "bit" -> "bit";
                // Date/Time Types
                case "date" -> "date";
                case "time" -> "time";
                case "datetime", "timestamp", "datetime2" -> "timestamp";
                // Binary/LOB Types
                case "binary", "varbinary" -> "binary";
                case "blob" -> "blob";
                case "clob" -> "clob";
                // UUID
                case "uuid" -> "uuid-char"; // Assuming string representation
                // Default for explicit DB type that isn't fully matched
                default -> cleanDbType;
            };
        }

        // Fallback mapping based on TypeScript type
        return switch (cleanTsType) {
            case "number" -> "long"; // Default number to a generic long/integer type
            case "string" -> "string";
            case "boolean" -> "boolean";
            case "date" -> "timestamp";
            default -> "string"; // Fallback for any unknown object or complex type
        };
    }

    /**
     * Map generation strategy.
     *
     * @param strategy the strategy
     * @return the string
     */
    private static String mapGenerationStrategy(String strategy) {
        if (strategy == null)
            return "assigned";
        return strategy.toLowerCase();
    }

}
