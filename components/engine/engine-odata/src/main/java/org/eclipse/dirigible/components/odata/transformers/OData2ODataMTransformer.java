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

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraintForeignKey;
import org.eclipse.dirigible.components.odata.api.*;
import org.eclipse.dirigible.components.odata.domain.OData;
import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The Class OData2ODataMTransformer.
 */
public class OData2ODataMTransformer {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(OData2ODataMTransformer.class);

    /** The property name escaper. */
    private final ODataPropertyNameEscaper propertyNameEscaper;

    /** The table metadata provider. */
    private final TableMetadataProvider tableMetadataProvider;

    /**
     * Instantiates a new o data 2 O data M transformer.
     */
    public OData2ODataMTransformer() {
        this(new DefaultTableMetadataProvider(), new DefaultPropertyNameEscaper());
    }

    /**
     * Instantiates a new o data 2 O data M transformer.
     *
     * @param metadataProvider the metadata provider
     * @param propertyNameEscaper the property name escaper
     */
    public OData2ODataMTransformer(TableMetadataProvider metadataProvider, ODataPropertyNameEscaper propertyNameEscaper) {
        this.tableMetadataProvider = metadataProvider;
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

        List<String> result = new ArrayList<>();

        for (ODataEntity entity : model.getEntities()) {
            Table tableMetadata = tableMetadataProvider.getTableMetadata(entity);
            if (null == tableMetadata) {
                throw new IllegalArgumentException("Cannot find table metadata for entity: " + entity);
            }
            StringBuilder buff = new StringBuilder();
            buff.append("{\n")
                .append("\t\"edmType\": \"")
                .append(entity.getName())
                .append("Type")
                .append("\",\n")
                .append("\t\"edmTypeFqn\": \"")
                .append(model.getNamespace())
                .append(".")
                .append(entity.getName())
                .append("Type")
                .append("\",\n")
                .append("\t\"sqlTable\": \"")
                .append(entity.getTable())
                .append("\",\n")

                .append("\t\"dataStructureType\": \"")
                .append(tableMetadata.getKind())
                .append("\",\n")
                .append("\t\"sqlSchema\": ");
            buff.append(null == entity.getSchema() ? "null" : ("\"" + entity.getSchema() + "\""))
                .append(",\n");

            boolean isPretty = Boolean.parseBoolean(Configuration.get(ODataDatabaseMetadataUtil.DIRIGIBLE_GENERATE_PRETTY_NAMES, "true"));

            if (tableMetadata.getKind() == null) {
                if (logger.isErrorEnabled()) {
                    logger.error("Table {} not available for entity {}, so it will be skipped.", entity.getTable(), entity.getName());
                }
                continue;
            }

            List<TableColumn> idColumns = tableMetadata.getColumns()
                                                       .stream()
                                                       .filter(TableColumn::isPrimaryKey)
                                                       .collect(Collectors.toList());
            if (idColumns.isEmpty() && ISqlKeywords.METADATA_TABLE.equals(tableMetadata.getKind())) {
                if (logger.isErrorEnabled()) {
                    logger.error("Table {} doesn't have primary keys {}, so it will be skipped.", entity.getTable(), entity.getName());
                }
                continue;
            }

            List<ODataProperty> entityProperties = entity.getProperties();
            ODataMetadataUtil.validateODataPropertyName(tableMetadata.getColumns(), entityProperties, entity.getName());
            // Expose all Db columns in case no entity props are defined
            if (entityProperties.isEmpty()) {
                tableMetadata.getColumns()
                             .forEach(column -> {
                                 String columnValue = ODataDatabaseMetadataUtil.getPropertyNameFromDbColumnName(column.getName(),
                                         entityProperties, isPretty);
                                 buff.append("\t\"")
                                     .append(propertyNameEscaper.escape(columnValue))
                                     .append("\": \"")
                                     .append(column.getName())
                                     .append("\",\n");
                             });
            } else {
                // In case entity props are defined expose only them
                entityProperties.forEach(prop -> {
                    List<TableColumn> dbColumnName = tableMetadata.getColumns()
                                                                  .stream()
                                                                  .filter(x -> x.getName()
                                                                                .equals(prop.getColumn()))
                                                                  .collect(Collectors.toList());
                    buff.append("\t\"")
                        .append(propertyNameEscaper.escape(prop.getName()))
                        .append("\": \"")
                        .append(dbColumnName.get(0)
                                            .getName())
                        .append("\",\n");
                });
            }

            List<ODataParameter> entityParameters = entity.getParameters();
            if (!entityParameters.isEmpty()) {
                List<String> parameterNames = new ArrayList<>();
                entityParameters.forEach(parameter -> {
                    parameterNames.add("\"" + parameter.getName() + "\"");
                    buff.append("\t\"")
                        .append(propertyNameEscaper.escape(parameter.getName()))
                        .append("\": \"")
                        .append(parameter.getName())
                        .append("\",\n");
                });
                buff.append("\t\"_parameters_\" : [")
                    .append(String.join(",", parameterNames))
                    .append("],\n");
            }

            // Process FK relations from DB if they exist
            Map<String, List<TableConstraintForeignKey>> groupRelationsByToTableName = tableMetadata.getConstraints()
                                                                                                    .getForeignKeys()
                                                                                                    .stream()
                                                                                                    .collect(Collectors.groupingBy(
                                                                                                            TableConstraintForeignKey::getReferencedTable));
            // In case there is FK on DB side, but the entity and it's navigation are not defined in the .odata
            // file -> then the relation will not be exposed
            List<Map.Entry<String, List<TableConstraintForeignKey>>> relationsThatExistInOdataFile = groupRelationsByToTableName.entrySet()
                                                                                                                                .stream()
                                                                                                                                .filter(x -> {
                                                                                                                                    for (ODataEntity e : model.getEntities()) {
                                                                                                                                        if (x.getKey()
                                                                                                                                             .equals(e.getTable())) {
                                                                                                                                            return true;
                                                                                                                                        }
                                                                                                                                    }
                                                                                                                                    return false;
                                                                                                                                })
                                                                                                                                .collect(
                                                                                                                                        Collectors.toList());

            final List<String> assembleRefFromFK = relationsThatExistInOdataFile.stream()
                                                                                .map(rel -> {
                                                                                    String fkElement = rel.getValue()
                                                                                                          .stream()
                                                                                                          .map(x -> "\"" + x.getColumns()[0]
                                                                                                                  + "\"")
                                                                                                          .collect(Collectors.joining(","));
                                                                                    ODataEntity toSetEntity =
                                                                                            ODataMetadataUtil.getEntityByTableName(model,
                                                                                                    rel.getKey());
                                                                                    String dependentEntity = toSetEntity.getName();
                                                                                    return assembleOdataMRefSection(dependentEntity,
                                                                                            fkElement, null, null);
                                                                                })
                                                                                .collect(Collectors.toList());
            if (!assembleRefFromFK.isEmpty()) {
                buff.append(String.join(",\n", assembleRefFromFK))
                    .append(",\n");
            }

            // Process Associations from .odata file
            List<ODataAssociation> assWhereEntityIsFROMRole = model.getAssociations()
                                                                   .stream()
                                                                   .filter(ass -> ass.getFrom()
                                                                                     .getEntity()
                                                                                     .equals(entity.getName()))
                                                                   .collect(Collectors.toList());
            List<ODataAssociation> assWhereEntityIsTORole = model.getAssociations()
                                                                 .stream()
                                                                 .filter(ass -> ass.getTo()
                                                                                   .getEntity()
                                                                                   .equals(entity.getName()))
                                                                 .collect(Collectors.toList());

            for (ODataAssociation oDataAssociation : assWhereEntityIsFROMRole) {
                validateAssociationProperties(oDataAssociation, model);
            }
            List<String> assembleRefNav = assWhereEntityIsFROMRole.stream()
                                                                  .map(association -> {
                                                                      String fromRoleEntity = association.getFrom()
                                                                                                         .getEntity();
                                                                      String toRoleEntity = association.getTo()
                                                                                                       .getEntity();
                                                                      ODataEntity toSetEntity = ODataMetadataUtil.getEntity(model,
                                                                              toRoleEntity, association.getName());
                                                                      String dependentEntity = toSetEntity.getName();
                                                                      String fkElement = association.getFrom()
                                                                                                    .getProperties()
                                                                                                    .stream()
                                                                                                    .map(x -> "\""
                                                                                                            + ODataMetadataUtil.getEntityPropertyColumnByPropertyName(
                                                                                                                    model, fromRoleEntity,
                                                                                                                    x)
                                                                                                            + "\"")
                                                                                                    .collect(Collectors.joining(","));
                                                                      String mappingTableName = association.getFrom()
                                                                                                           .getMappingTable()
                                                                                                           .getMappingTableName();
                                                                      String mappingTableJoinColumn = association.getFrom()
                                                                                                                 .getMappingTable()
                                                                                                                 .getMappingTableJoinColumn();
                                                                      return checkRefSectionConsistency(buff, entity,
                                                                              groupRelationsByToTableName, assembleRefFromFK, toSetEntity,
                                                                              dependentEntity, fkElement, mappingTableName,
                                                                              mappingTableJoinColumn);
                                                                  })
                                                                  .filter(Objects::nonNull)
                                                                  .collect(Collectors.toList());
            if (!assembleRefNav.isEmpty()) {
                buff.append(String.join(",\n", assembleRefNav))
                    .append(",\n");
            }

            for (ODataAssociation oDataAssociation : assWhereEntityIsTORole) {
                validateAssociationProperties(oDataAssociation, model);
            }
            assembleRefNav = assWhereEntityIsTORole.stream()
                                                   .map(association -> {
                                                       String fromRoleEntity = association.getFrom()
                                                                                          .getEntity();
                                                       String toRoleEntity = association.getTo()
                                                                                        .getEntity();
                                                       ODataEntity fromSetEntity =
                                                               ODataMetadataUtil.getEntity(model, fromRoleEntity, association.getName());
                                                       String principleEntity = fromSetEntity.getName();
                                                       String fkElement = association.getTo()
                                                                                     .getProperties()
                                                                                     .stream()
                                                                                     .map(x -> "\""
                                                                                             + ODataMetadataUtil.getEntityPropertyColumnByPropertyName(
                                                                                                     model, toRoleEntity, x)
                                                                                             + "\"")
                                                                                     .collect(Collectors.joining(","));
                                                       String mappingTableName = association.getTo()
                                                                                            .getMappingTable()
                                                                                            .getMappingTableName();
                                                       String mappingTableJoinColumn = association.getTo()
                                                                                                  .getMappingTable()
                                                                                                  .getMappingTableJoinColumn();
                                                       return checkRefSectionConsistency(buff, entity, groupRelationsByToTableName,
                                                               assembleRefFromFK, fromSetEntity, principleEntity, fkElement,
                                                               mappingTableName, mappingTableJoinColumn);
                                                   })
                                                   .filter(Objects::nonNull)
                                                   .collect(Collectors.toList());
            if (!assembleRefNav.isEmpty()) {
                buff.append(String.join(",\n", assembleRefNav))
                    .append(",\n");
            }

            if (entity.getKeyGenerated() != null && !entity.getKeyGenerated()
                                                           .isEmpty()) {
                buff.append("\t\"keyGenerated\": \"")
                    .append(entity.getKeyGenerated())
                    .append("\",\n");
            }

            if ("aggregate".equals(entity.getAnnotationsEntityType()
                                         .get("sap:semantics"))) {
                buff.append("\t\"aggregationType\" : ");
                if (!entity.getAggregationsTypeAndColumn()
                           .isEmpty()) {
                    buff.append("\"derived\",\n");
                    Map<String, String> aggregationsTypeAndColumn = entity.getAggregationsTypeAndColumn();
                    String aggregationProps = aggregationsTypeAndColumn.keySet()
                                                                       .stream()
                                                                       .map(key -> "\t\t\"" + key + "\": \""
                                                                               + aggregationsTypeAndColumn.get(key) + "\"")
                                                                       .collect(Collectors.joining(",\n", "\t\"aggregationProps\" : {\n",
                                                                               "\n\t},\n"));
                    buff.append(aggregationProps);
                } else {
                    buff.append("\"explicit\",\n");
                }
            }

            String[] pks = idColumns.stream()
                                    .map(TableColumn::getName)
                                    .collect(Collectors.toList())
                                    .toArray(new String[] {});
            buff.append("\t\"_pk_\" : \"")
                .append(String.join(",", pks))
                .append("\"");
            buff.append("\n}");

            result.add(buff.toString());
        }
        return result.toArray(new String[] {});
    }

    /**
     * Check ref section consistency.
     *
     * @param buff the buff
     * @param entity the entity
     * @param groupRelationsByToTableName the group relations by to table name
     * @param assembleRefFromFK the assemble ref from FK
     * @param toSetEntity the to set entity
     * @param principleEntity the principle entity
     * @param fkElement the fk element
     * @param mappingTableName the mapping table name
     * @param mappingTableJoinColumn the mapping table join column
     * @return the string
     */
    private String checkRefSectionConsistency(StringBuilder buff, ODataEntity entity,
            Map<String, List<TableConstraintForeignKey>> groupRelationsByToTableName, List<String> assembleRefFromFK,
            ODataEntity toSetEntity, String principleEntity, String fkElement, String mappingTableName, String mappingTableJoinColumn) {
        String refSection = assembleOdataMRefSection(principleEntity, fkElement, mappingTableName, mappingTableJoinColumn);
        if (groupRelationsByToTableName.get(toSetEntity.getTable()) != null) {
            List<String> match = assembleRefFromFK.stream()
                                                  .filter(el -> el.equals(refSection))
                                                  .collect(Collectors.toList());
            if (match.isEmpty()) {
                throw new OData2TransformerException(
                        String.format("There is inconsistency in odata file from table %s to table %s on joinColumns: %s",
                                entity.getTable(), principleEntity, fkElement));
            }
        }
        if (!buff.toString()
                 .contains("_ref_" + principleEntity + "Type")) {
            return refSection;
        }
        return null;
    }

    /**
     * Validate association properties.
     *
     * @param association the association
     * @param model the model
     */
    private void validateAssociationProperties(ODataAssociation association, OData model) {
        validateAssociationProperty(association.getFrom(), model, association);
        validateAssociationProperty(association.getTo(), model, association);
    }

    /**
     * Validate association property.
     *
     * @param assEnd the ass end
     * @param model the model
     * @param association the association
     */
    private void validateAssociationProperty(ODataAssociationEnd assEnd, OData model, ODataAssociation association) {
        ODataEntity entity = ODataMetadataUtil.getEntity(model, assEnd.getEntity(), association.getName());
        if (!entity.getProperties()
                   .isEmpty()) {
            ArrayList<String> invalidProps = new ArrayList<>();
            assEnd.getProperties()
                  .forEach(assProp -> {
                      List<ODataProperty> consistentProps = entity.getProperties()
                                                                  .stream()
                                                                  .filter(prop -> prop.getName()
                                                                                      .equals(assProp))
                                                                  .collect(Collectors.toList());
                      if (consistentProps.isEmpty()) {
                          invalidProps.add(assProp);
                      }
                  });
            if (!invalidProps.isEmpty()) {
                throw new OData2TransformerException(String.format(
                        "There is inconsistency for entity '%s'. OData entity properties definitions for %s do not match the association properties definition.",
                        assEnd.getEntity(), invalidProps.stream()
                                                        .map(String::valueOf)
                                                        .collect(Collectors.joining(","))));
            }
            if (assEnd.getProperties()
                      .size() > entity.getProperties()
                                      .size()) {
                throw new OData2TransformerException(String.format(
                        "There is inconsistency for entity '%s'. The number of defined OData properties do not match the number of the association properties definition",
                        assEnd.getEntity()));
            }
        }
    }

    /**
     * Assemble odata M ref section.
     *
     * @param dependentEntity the dependent entity
     * @param fkElements the fk elements
     * @param mappingTableName the mapping table name
     * @param mappingTableJoinColumn the mapping table join column
     * @return the string
     */
    private String assembleOdataMRefSection(String dependentEntity, String fkElements, String mappingTableName,
            String mappingTableJoinColumn) {
        String refSection = "\t\"_ref_" + dependentEntity + "Type" + "\": {\n\t\t\"joinColumn\" : " + "[\n\t\t\t" + fkElements + "\n\t\t]";

        if (mappingTableName != null && !mappingTableName.isEmpty() && mappingTableJoinColumn != null
                && !mappingTableJoinColumn.isEmpty()) {
            refSection = refSection + "," + "\n\t\t\"manyToManyMappingTable\" : {" + "\n\t\t\t\"mappingTableName\" : " + "\""
                    + mappingTableName + "\"," + "\n\t\t\t\"mappingTableJoinColumn\" : " + "\"" + mappingTableJoinColumn + "\"" + "\n\t\t}";
        }

        refSection = refSection + "\n\t}";

        return refSection;
    }

}


