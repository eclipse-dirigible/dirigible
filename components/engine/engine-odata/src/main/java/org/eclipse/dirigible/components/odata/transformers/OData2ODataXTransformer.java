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

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
import org.eclipse.dirigible.components.odata.api.ODataAssociation;
import org.eclipse.dirigible.components.odata.api.ODataEntity;
import org.eclipse.dirigible.components.odata.api.ODataParameter;
import org.eclipse.dirigible.components.odata.api.ODataProperty;
import org.eclipse.dirigible.components.odata.api.TableMetadataProvider;
import org.eclipse.dirigible.components.odata.domain.OData;
import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class OData2ODataXTransformer.
 */
public class OData2ODataXTransformer {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(OData2ODataXTransformer.class);

    /** The Constant TABLE_TYPES. */
    public static final List<String> TABLE_TYPES = List.of(ISqlKeywords.METADATA_TABLE, ISqlKeywords.METADATA_BASE_TABLE);

    /** The Constant VIEW_TYPES. */
    public static final List<String> VIEW_TYPES = List.of(ISqlKeywords.METADATA_VIEW, ISqlKeywords.METADATA_CALC_VIEW);

    /** The property name escaper. */
    private final ODataPropertyNameEscaper propertyNameEscaper;

    /** The table metadata provider. */
    private final TableMetadataProvider tableMetadataProvider;

    /**
     * Instantiates a new o data 2 O data X transformer.
     *
     * @param tableMetadataProvider the table metadata provider
     */
    public OData2ODataXTransformer(TableMetadataProvider tableMetadataProvider) {
        this(tableMetadataProvider, new DefaultPropertyNameEscaper());
    }

    /**
     * Instantiates a new o data 2 O data X transformer.
     *
     * @param tableMetadataProvider the table metadata provider
     * @param propertyNameEscaper the property name escaper
     */
    public OData2ODataXTransformer(TableMetadataProvider tableMetadataProvider, ODataPropertyNameEscaper propertyNameEscaper) {
        this.tableMetadataProvider = tableMetadataProvider;
        this.propertyNameEscaper = propertyNameEscaper;
    }

    /**
     * Transform.
     *
     * @param model the model
     * @return the string[]
     * @throws SQLException the SQL exception
     */
    public String[] transform(OData model) throws SQLException {
        String[] result = new String[2];
        StringBuilder buff = new StringBuilder();
        buff.append("<Schema Namespace=\"")
            .append(model.getNamespace())
            .append("\"\n")
            .append("\txmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\">\n");

        StringBuilder associations = new StringBuilder();
        StringBuilder entitySets = new StringBuilder();
        StringBuilder associationsSets = new StringBuilder();
        for (ODataEntity entity : model.getEntities()) {
            Table tableMetadata = tableMetadataProvider.getTableMetadata(entity);

            if (tableMetadata == null) {
                continue;
            }

            List<TableColumn> idColumns = tableMetadata.getColumns()
                                                       .stream()
                                                       .filter(TableColumn::isPrimaryKey)
                                                       .collect(Collectors.toList());

            if (tableMetadata.getKind() == null || (idColumns.isEmpty() && ISqlKeywords.METADATA_TABLE.equals(tableMetadata.getKind()))) {
                if (logger.isErrorEnabled()) {
                    logger.error("Table {} not available for entity {}, so it will be skipped.", entity.getTable(), entity.getName());
                }
                continue;
            }
            if (!VIEW_TYPES.contains(tableMetadata.getKind()) && !TABLE_TYPES.contains(tableMetadata.getKind())) {
                if (logger.isErrorEnabled()) {
                    logger.error("Unsupported database type {} for entity object {}", tableMetadata.getKind(), entity.getTable());
                }
                continue;
            }

            boolean isPretty = Boolean.parseBoolean(Configuration.get(ODataDatabaseMetadataUtil.DIRIGIBLE_GENERATE_PRETTY_NAMES, "true"));

            List<ODataParameter> entityParameters = entity.getParameters();
            List<ODataProperty> entityProperties = entity.getProperties();

            if (tableMetadata.getKind()
                             .equals(ISqlKeywords.METADATA_TABLE)) {
                ODataMetadataUtil.validateODataPropertyName(tableMetadata.getColumns(), entityProperties, entity.getName());
            }

            buff.append("\t<EntityType Name=\"")
                .append(entity.getName())
                .append("Type")
                .append("\"");
            entity.getAnnotationsEntityType()
                  .forEach((key, value) -> buff.append(" ")
                                               .append(key)
                                               .append("=\"")
                                               .append(value)
                                               .append("\""));
            buff.append(">\n");

            List<TableColumn> entityOrigKeys = checkIfViewHasExposedOriginalKeysFromTable(tableMetadata, entity);
            buff.append("\t\t<Key>\n");

            // Keys are explicit defined only on VIEW artifact
            if (VIEW_TYPES.contains(tableMetadata.getKind())) {
                entityParameters.forEach(parameter -> buff.append("\t\t\t<PropertyRef Name=\"")
                                                          .append(propertyNameEscaper.escape(parameter.getName()))
                                                          .append("\" />\n"));

                if (entityOrigKeys.size() > 0) {
                    entityOrigKeys.forEach(key -> {
                        String columnValue =
                                ODataDatabaseMetadataUtil.getPropertyNameFromDbColumnName(key.getName(), entityProperties, isPretty);
                        buff.append("\t\t\t<PropertyRef Name=\"")
                            .append(propertyNameEscaper.escape(columnValue))
                            .append("\" />\n");
                    });
                } else {
                    // Local key was generated
                    entity.getKeys()
                          .forEach(key -> buff.append("\t\t\t<PropertyRef Name=\"")
                                              .append(propertyNameEscaper.escape(key))
                                              .append("\" />\n"));
                }
            } else {
                idColumns.forEach(column -> {
                    String nameValue =
                            ODataDatabaseMetadataUtil.getPropertyNameFromDbColumnName(column.getName(), entityProperties, isPretty);
                    buff.append("\t\t\t<PropertyRef Name=\"")
                        .append(propertyNameEscaper.escape(nameValue))
                        .append("\" />\n");
                });
            }
            buff.append("\t\t</Key>\n");

            // Add keys and parameters as property
            if (VIEW_TYPES.contains(tableMetadata.getKind())) {
                if (entityOrigKeys.size() == 0) {
                    // Local key was generated
                    entity.getKeys()
                          .forEach(key -> buff.append("\t\t<Property Name=\"")
                                              .append(key)
                                              .append("\"")
                                              .append(" Type=\"")
                                              .append("Edm.String")
                                              .append("\"")
                                              .append(" Nullable=\"")
                                              .append("false")
                                              .append("\" MaxLength=\"2147483647\"")
                                              .append(" sap:filterable=\"false\"")
                                              .append("/>\n"));
                }

                entityParameters.forEach(parameter -> {
                    buff.append("\t\t<Property Name=\"")
                        .append(parameter.getName())
                        .append("\"")
                        .append(" Nullable=\"")
                        .append(parameter.isNullable())
                        .append("\"")
                        .append(" Type=\"")
                        .append(parameter.getType() != null ? parameter.getType() : "null")
                        .append("\"");
                    buff.append("/>\n");
                });
            }

            // Expose all Db columns in case no entity props are defined
            if (entityProperties.isEmpty() && entityParameters.isEmpty()) {
                tableMetadata.getColumns()
                             .forEach(column -> {
                                 String columnValue = ODataDatabaseMetadataUtil.getPropertyNameFromDbColumnName(column.getName(),
                                         entityProperties, isPretty);
                                 buff.append("\t\t<Property Name=\"")
                                     .append(propertyNameEscaper.escape(columnValue))
                                     .append("\"")
                                     .append(" Nullable=\"")
                                     .append(ODataDatabaseMetadataUtil.isNullable(column, entityProperties))
                                     .append("\"")
                                     .append(" Type=\"")
                                     .append(ODataDatabaseMetadataUtil.getType(column, entityProperties))
                                     .append("\"");
                                 buff.append("/>\n");
                             });
            } else {
                // In case entity props are defined expose only them
                Table finalTableMetadata = tableMetadata;
                entityProperties.forEach(prop -> {
                    List<TableColumn> dbColumn = finalTableMetadata.getColumns()
                                                                   .stream()
                                                                   .filter(el -> el.getName()
                                                                                   .equals(prop.getColumn()))
                                                                   .collect(Collectors.toList());
                    if (dbColumn.size() > 0) {
                        String columnValue = ODataDatabaseMetadataUtil.getPropertyNameFromDbColumnName(dbColumn.get(0)
                                                                                                               .getName(),
                                entityProperties, isPretty);
                        buff.append("\t\t<Property Name=\"")
                            .append(columnValue)
                            .append("\"")
                            .append(" Nullable=\"")
                            .append(prop.isNullable())
                            .append("\"")
                            .append(" Type=\"")
                            .append(prop.getType() != null ? prop.getType()
                                    : dbColumn.get(0)
                                              .getType())
                            .append("\"");
                        prop.getAnnotationsProperty()
                            .forEach((key, value) -> buff.append(" ")
                                                         .append(key)
                                                         .append("=\"")
                                                         .append(value)
                                                         .append("\""));
                        buff.append("/>\n");
                    } else {
                        throw new OData2TransformerException(String.format(
                                "There is inconsistency for entity '%s'. Odata column definitions for %s do not match the DB table column definition.",
                                entity.getName(), prop.getName()));
                    }
                });
            }

            entity.getNavigations()
                  .forEach(relation -> {
                      ODataAssociation association = ODataMetadataUtil.getAssociation(model, relation.getAssociation(), relation.getName());
                      String fromRole = association.getFrom()
                                                   .getEntity();
                      String toRole = association.getTo()
                                                 .getEntity();
                      buff.append("\t\t<NavigationProperty Name=\"")
                          .append(relation.getName())
                          .append("\"")
                          .append(" Relationship=\"")
                          .append(model.getNamespace())
                          .append(".")
                          .append(relation.getAssociation())
                          .append("Type\"")
                          .append(" FromRole=\"")
                          .append(fromRole)
                          .append("Principal")
                          .append("\"")
                          .append(" ToRole=\"")
                          .append(toRole)
                          .append("Dependent")
                          .append("\"");
                      relation.getAnnotationsNavigationProperty()
                              .forEach((key, value) -> buff.append(" ")
                                                           .append(key)
                                                           .append("=\"")
                                                           .append(value)
                                                           .append("\""));
                      buff.append("/>\n");
                  });

            // Keep associations for later use
            entity.getNavigations()
                  .forEach(relation -> {
                      ODataAssociation association = ODataMetadataUtil.getAssociation(model, relation.getAssociation(), relation.getName());
                      String fromRole = association.getFrom()
                                                   .getEntity();
                      String toRole = association.getTo()
                                                 .getEntity();
                      String fromMultiplicity = association.getFrom()
                                                           .getMultiplicity();
                      ODataMetadataUtil.validateMultiplicity(fromMultiplicity);
                      String toMultiplicity = association.getTo()
                                                         .getMultiplicity();
                      ODataMetadataUtil.validateMultiplicity(toMultiplicity);
                      associations.append("\t<Association Name=\"")
                                  .append(relation.getAssociation())
                                  .append("Type\">\n")
                                  .append("\t\t<End Type=\"")
                                  .append(model.getNamespace())
                                  .append(".")
                                  .append(fromRole)
                                  .append("Type\"")
                                  .append(" Role=\"")
                                  .append(fromRole)
                                  .append("Principal")
                                  .append("\" Multiplicity=\"")
                                  .append(fromMultiplicity)
                                  .append("\"/>\n")
                                  .append("\t\t<End Type=\"")
                                  .append(model.getNamespace())
                                  .append(".")
                                  .append(toRole)
                                  .append("Type\"")
                                  .append(" Role=\"")
                                  .append(toRole)
                                  .append("Dependent")
                                  .append("\" Multiplicity=\"")
                                  .append(toMultiplicity)
                                  .append("\"/>\n")
                                  .append(" \t</Association>\n");
                  });

            // Keep entity sets for later use
            entitySets.append("\t\t<EntitySet Name=\"")
                      .append(entity.getAlias())
                      .append("\" EntityType=\"")
                      .append(model.getNamespace())
                      .append(".")
                      .append(entity.getName())
                      .append("Type\"");
            entity.getAnnotationsEntitySet()
                  .forEach((key, value) -> entitySets.append(" ")
                                                     .append(key)
                                                     .append("=\"")
                                                     .append(value)
                                                     .append("\""));
            entitySets.append("/>\n");

            // Keep associations sets for later use
            entity.getNavigations()
                  .forEach(relation -> {
                      ODataAssociation association = ODataMetadataUtil.getAssociation(model, relation.getAssociation(), relation.getName());
                      String fromRole = association.getFrom()
                                                   .getEntity();
                      String toRole = association.getTo()
                                                 .getEntity();
                      String fromSet = entity.getAlias();
                      ODataEntity toSetEntity = ODataMetadataUtil.getEntity(model, toRole, relation.getName());
                      String toSet = toSetEntity.getAlias();
                      associationsSets.append("\t<AssociationSet Name=\"")
                                      .append(relation.getAssociation())
                                      .append("\"")
                                      .append(" Association=\"")
                                      .append(model.getNamespace())
                                      .append(".")
                                      .append(relation.getAssociation())
                                      .append("Type\"");
                      association.getAnnotationsAssociationSet()
                                 .forEach((key, value) -> associationsSets.append(" ")
                                                                          .append(key)
                                                                          .append("=\"")
                                                                          .append(value)
                                                                          .append("\""));
                      associationsSets.append(">\n");
                      associationsSets.append("\t\t\t<End Role=\"")
                                      .append(fromRole)
                                      .append("Principal")
                                      .append("\"")
                                      .append(" EntitySet=\"")
                                      .append(fromSet)
                                      .append("\"/>\n")
                                      .append(" \t\t\t<End Role=\"")
                                      .append(toRole)
                                      .append("Dependent")
                                      .append("\"")
                                      .append(" EntitySet=\"")
                                      .append(toSet)
                                      .append("\"/>\n")
                                      .append("\t\t\t</AssociationSet>\n");
                  });
            buff.append("\t</EntityType>\n");
        }
        buff.append(associations);

        StringBuilder container = new StringBuilder();
        container.append(entitySets);
        container.append(associationsSets);

        buff.append("</Schema>\n");
        result[0] = buff.toString();
        result[1] = container.toString();
        return result;
    }

    /**
     * Check if view has exposed original keys from table.
     *
     * @param tableMetadata the table metadata
     * @param entity the entity
     * @return the list
     */
    private List<TableColumn> checkIfViewHasExposedOriginalKeysFromTable(Table tableMetadata, ODataEntity entity) {
        return tableMetadata.getColumns()
                            .stream()
                            .filter(el -> entity.getKeys()
                                                .stream()
                                                .anyMatch(x -> x.equals(el.getName())))
                            .collect(Collectors.toList());
    }
}
