/*
 * Copyright (c) 2010-2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2010-2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.transformers;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableColumnModel;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableRelationModel;
import org.eclipse.dirigible.engine.odata2.definition.ODataAssociationDefinition;
import org.eclipse.dirigible.engine.odata2.definition.ODataDefinition;
import org.eclipse.dirigible.engine.odata2.definition.ODataEntityDefinition;
import org.eclipse.dirigible.engine.odata2.definition.ODataProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
public class OData2ODataMTransformer {

    private static final Logger logger = LoggerFactory.getLogger(OData2ODataMTransformer.class);

    @Inject
    private DBMetadataUtil dbMetadataUtil;

    public String[] transform(ODataDefinition model) throws SQLException {

        List<String> result = new ArrayList<>();

        for (ODataEntityDefinition entity : model.getEntities()) {
            StringBuilder buff = new StringBuilder();
            buff.append("{\n")
                    .append("\t\"edmType\": \"").append(entity.getName()).append("Type").append("\",\n")
                    .append("\t\"edmTypeFqn\": \"").append(model.getNamespace()).append(".").append(entity.getName()).append("Type").append("\",\n")
                    .append("\t\"sqlTable\": \"").append(entity.getTable()).append("\",\n");

            boolean isPretty = Boolean.parseBoolean(Configuration.get(DBMetadataUtil.DIRIGIBLE_GENERATE_PRETTY_NAMES, "true"));

            PersistenceTableModel tableMetadata = dbMetadataUtil.getTableMetadata(entity.getTable());
            List<PersistenceTableColumnModel> idColumns = tableMetadata.getColumns().stream().filter(PersistenceTableColumnModel::isPrimaryKey).collect(Collectors.toList());

            if (idColumns == null || idColumns.isEmpty()) {
                logger.error("Table {} not available for entity {}, so it will be skipped.", entity.getTable(), entity.getName());
                continue;
            }

            List<ODataProperty> entityProperties = entity.getProperties();
            validateProperties(tableMetadata.getColumns(), entityProperties, entity.getName());
            //expose all Db columns in case no entity props are defined
            if (entityProperties.isEmpty()) {
                tableMetadata.getColumns().forEach(column -> {
                    String columnValue = DBMetadataUtil.getPropertyNameFromDbColumnName(column.getName(), entityProperties, isPretty);
                    buff.append("\t\"").append(columnValue).append("\": \"").append(column.getName()).append("\",\n");
                });
            } else {
                //in case entity props are defined expose only them
                entityProperties.forEach(prop -> {
                    List<PersistenceTableColumnModel> dbColumnName = tableMetadata.getColumns().stream().filter(x -> x.getName().equals(prop.getColumn())).collect(Collectors.toList());
                    buff.append("\t\"").append(prop.getName()).append("\": \"").append(dbColumnName.get(0).getName()).append("\",\n");
                });
            }

            final List<String> assembleRef;

            //Process FK relations from DB
            Map<String, List<PersistenceTableRelationModel>> groupRelationsByToTableName = tableMetadata.getRelations().stream()
                    .collect(Collectors.groupingBy(PersistenceTableRelationModel::getToTableName));
            //In case there is FK on DB side, but the entity and it's navigation are not defined in the .odata file -> then the relation will not be exposed
            List<Map.Entry<String, List<PersistenceTableRelationModel>>> relationsThatExistInOdataFile
                    = groupRelationsByToTableName.entrySet().stream().filter(x -> {
                for (ODataEntityDefinition e : model.getEntities()) {
                    if (x.getKey().equals(e.getTable())) {
                        return true;
                    }
                }
                return false;
            }).collect(Collectors.toList());

            assembleRef = relationsThatExistInOdataFile.stream().map(rel -> {
            String fkElement = rel.getValue().stream().map(x -> "\"" + x.getFkColumnName() + "\"").collect(Collectors.joining(","));
                ODataEntityDefinition toSetEntity = ODataMetadataUtil.getEntityByTableName(model, rel.getKey());
                String dependentEntity = toSetEntity.getName();
                return assembleOdataMRefSection(dependentEntity, fkElement);
            }).collect(Collectors.toList());
            if (!assembleRef.isEmpty()) {
                buff.append(String.join(",\n", assembleRef)).append(",\n");
            }

            //Process Associations from .odata file
            List<String> assembleRefNav = entity.getNavigations().stream().map(rel -> {
                ODataAssociationDefinition association = ODataMetadataUtil.getAssociation(model, rel.getAssociation(), rel.getName());
                String toRole = association.getTo().getEntity();
                ODataEntityDefinition toSetEntity = ODataMetadataUtil.getEntity(model, toRole, rel.getName());
                String dependentEntity = toSetEntity.getName();
                String fkElement = association.getFrom().getProperty().stream().map(x -> "\"" + x + "\"").collect(Collectors.joining(","));
                String refSection = assembleOdataMRefSection(dependentEntity, fkElement);
                if (groupRelationsByToTableName.get(toSetEntity.getTable()) != null) {
                    List<String> match = assembleRef.stream().filter(el -> el.equals(refSection)).collect(Collectors.toList());
                    if (match.isEmpty()) {
                        throw new OData2TransformerException(String.format("There is inconsistency in odata file from table %s to table %s on joinColumns: %s", entity.getTable(), dependentEntity, fkElement));
                    }
                }
                if (!buff.toString().contains("_ref_" + dependentEntity + "Type")) {
                    return refSection;
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            if (!assembleRefNav.isEmpty()) {
                buff.append(String.join(",\n", assembleRefNav)).append(",\n");
            }

            String[] pks = idColumns.stream().map(PersistenceTableColumnModel::getName).collect(Collectors.toList()).toArray(new String[]{});
            buff.append("\t\"_pk_\" : \"").append(String.join(",", pks)).append("\"");
            buff.append("\n}");

            result.add(buff.toString());
        }
        return result.toArray(new String[]{});
    }

    /**
     * Check if the provided ODataProperty properties are the same as the one defined in the DB
     */
    private void validateProperties(List<PersistenceTableColumnModel> dbColumnNames, List<ODataProperty> entityProperties, String entityName) {
        if (!entityProperties.isEmpty()) {
            ArrayList<String> invalidProps = new ArrayList<>();
            entityProperties.forEach(column -> {
                List<PersistenceTableColumnModel> consistentProps = dbColumnNames.stream().filter(prop -> prop.getName().equals(column.getColumn())).collect(Collectors.toList());
                if (consistentProps.isEmpty()) {
                    invalidProps.add(consistentProps.stream().map(String::valueOf).collect(Collectors.joining(",")));
                }
            });
            if (!invalidProps.isEmpty()) {
                throw new OData2TransformerException(String.format("There is inconsistency for entity '%s'. Odata column definitions do not match the DB table column definitions.", entityName));
            }
            if (entityProperties.size() > dbColumnNames.size()) {
                throw new OData2TransformerException(String.format("There is inconsistency for entity '%s'. The number of defined odata columns do not match the number of DB table columns", entityName));
            }
        }
    }

    private String assembleOdataMRefSection(String dependentEntity, String fkElements) {
        return "\t\"_ref_" + dependentEntity + "Type" + "\": {\n\t\t\"joinColumn\" : " + "[\n\t\t\t" +
                fkElements + "\n\t\t]" + "\n\t}";
    }
}