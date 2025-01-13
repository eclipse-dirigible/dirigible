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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.olingo.odata2.api.edm.EdmMultiplicity;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
import org.eclipse.dirigible.components.odata.api.ODataAssociation;
import org.eclipse.dirigible.components.odata.api.ODataEntity;
import org.eclipse.dirigible.components.odata.api.ODataHandlerMethods;
import org.eclipse.dirigible.components.odata.api.ODataHandlerTypes;
import org.eclipse.dirigible.components.odata.api.ODataProperty;
import org.eclipse.dirigible.components.odata.domain.OData;

/**
 * The Class ODataMetadataUtil.
 */
public class ODataMetadataUtil {

    /**
     * Gets the entity.
     *
     * @param model the model
     * @param name the name
     * @param navigation the navigation
     * @return the entity
     */
    public static ODataEntity getEntity(OData model, String name, String navigation) {
        for (ODataEntity entity : model.getEntities()) {
            if (name.equals(entity.getName())) {
                return entity;
            }
        }
        throw new IllegalArgumentException(
                String.format("There is no entity with name: %s, referenced by the navigation: %s", name, navigation));
    }

    /**
     * Gets the entity by table name.
     *
     * @param model the model
     * @param tableName the table name
     * @return the entity by table name
     */
    public static ODataEntity getEntityByTableName(OData model, String tableName) {
        for (ODataEntity entity : model.getEntities()) {
            if (tableName.equals(entity.getTable())) {
                return entity;
            }
        }
        throw new IllegalArgumentException(String.format("There is no table with name: %s defined in the model", tableName));
    }

    /**
     * Gets the table name by entity.
     *
     * @param model the model
     * @param entityName the entity name
     * @return the table name by entity
     */
    public static String getTableNameByEntity(OData model, String entityName) {
        for (ODataEntity entity : model.getEntities()) {
            if (entityName.equals(entity.getName())) {
                return entity.getTable();
            }
        }
        throw new IllegalArgumentException(String.format("There is no entity with name: %s defined in the model", entityName));
    }

    /**
     * Gets the entity property column by property name.
     *
     * @param model the model
     * @param entityName the entity name
     * @param propertyName the property name
     * @return the entity property column by property name
     */
    public static String getEntityPropertyColumnByPropertyName(OData model, String entityName, String propertyName) {
        for (ODataEntity entity : model.getEntities()) {
            if (entityName.equals(entity.getName())) {
                if (!entity.getProperties()
                           .isEmpty()) {
                    for (ODataProperty property : entity.getProperties()) {
                        if (property.getName()
                                    .equals(propertyName)) {
                            return property.getColumn();
                        }
                    }
                } else {
                    return propertyName;
                }
            }
        }
        throw new IllegalArgumentException(String.format("There is no entity with name: %s and property with name: %s defined in the model",
                entityName, propertyName));
    }

    /**
     * Gets the association.
     *
     * @param model the model
     * @param name the name
     * @param navigation the navigation
     * @return the association
     */
    public static ODataAssociation getAssociation(OData model, String name, String navigation) {
        for (ODataAssociation association : model.getAssociations()) {
            if (name.equals(association.getName())) {
                return association;
            }
        }
        throw new IllegalArgumentException(
                String.format("There is no association with name: %s, referenced by the navigation: %s", name, navigation));
    }

    /**
     * Validate multiplicity.
     *
     * @param actualValue the actual value
     */
    public static void validateMultiplicity(String actualValue) {
        try {
            EdmMultiplicity.fromLiteral(actualValue);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(String.format("Unsupported multiplicity value: %s", actualValue));
        }
    }

    /**
     * Validate handler definition types.
     *
     * @param actualValue the actual value
     * @param entityName the entity name
     */
    public static void validateHandlerDefinitionTypes(String actualValue, String entityName) {
        try {
            ODataHandlerTypes.fromValue(actualValue);
        } catch (IllegalArgumentException ex) {
            throw new OData2TransformerException(
                    String.format("There is inconsistency in odata file for entity %s on handler definition: %s", entityName, actualValue));
        }
    }

    /**
     * Validate handler definition method.
     *
     * @param actualValue the actual value
     * @param entityName the entity name
     */
    public static void validateHandlerDefinitionMethod(String actualValue, String entityName) {
        try {
            ODataHandlerMethods.fromValue(actualValue);
        } catch (IllegalArgumentException ex) {
            throw new OData2TransformerException(
                    String.format("There is inconsistency in odata file for entity %s on handler definition: %s", entityName, actualValue));
        }
    }

    /**
     * Check if the provided ODataProperty column is the same as the one defined in the DB for the given
     * entity.
     *
     * @param dbColumnNames db artifact column name
     * @param entityProperties list of entity properties
     * @param entityName name of entity
     */
    public static void validateODataPropertyName(List<TableColumn> dbColumnNames, List<ODataProperty> entityProperties, String entityName) {
        if (!entityProperties.isEmpty()) {
            ArrayList<String> invalidProps = new ArrayList<>();
            entityProperties.forEach(column -> {
                List<TableColumn> consistentProps = dbColumnNames.stream()
                                                                 .filter(prop -> prop.getName()
                                                                                     .equals(column.getColumn()))
                                                                 .collect(Collectors.toList());
                if (consistentProps.isEmpty()) {
                    invalidProps.add(column.getColumn());
                }
            });
            if (!invalidProps.isEmpty()) {
                throw new OData2TransformerException(String.format(
                        "There is inconsistency for entity '%s'. Odata column definitions for %s do not match the DB table column definition.",
                        entityName, invalidProps.stream()
                                                .map(String::valueOf)
                                                .collect(Collectors.joining(","))));
            }
            if (entityProperties.size() > dbColumnNames.size()) {
                throw new OData2TransformerException(String.format(
                        "There is inconsistency for entity '%s'. The number of defined odata columns do not match the number of DB table columns",
                        entityName));
            }
        }
    }

}
