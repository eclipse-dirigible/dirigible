/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
import { Configurations } from "@aerokit/sdk/core";
import { Base64 } from "@aerokit/sdk/utils";
import { Bytes } from "@aerokit/sdk/io";

export function process(model, parameters) {
    model.entities.forEach(e => {
        if (parameters.dataSource && !e.dataSource) {
            e.dataSource = parameters.dataSource;
        } else {
            const defaultDataSourceName = Configurations.get("DIRIGIBLE_DATABASE_DATASOURCE_NAME_DEFAULT", "DefaultDB");
            e.dataSource = defaultDataSourceName;
            parameters.dataSource = defaultDataSourceName;
        }
        let tablePrefix = parameters.tablePrefix ? parameters.tablePrefix : '';
        if (tablePrefix !== '' && !tablePrefix.endsWith("_")) {
            tablePrefix = `${tablePrefix}_`;
        }
        parameters.tablePrefix = tablePrefix;
        if (e.dataCount) {
            e.dataCount = e.dataCount.replaceAll("${tablePrefix}", parameters.tablePrefix);
        }
        if (e.dataQuery) {
            e.dataQuery = e.dataQuery.replaceAll("${tablePrefix}", parameters.tablePrefix);
        }

        if (e.type === "DEPENDENT" && (e.layoutType === "LIST_DETAILS" || e.layoutType === "MANAGE_DETAILS")) {
            const relationshipEntityName = e.properties.filter(p => p.relationshipType === "COMPOSITION" && p.relationshipCardinality === "1_n").map(p => p.relationshipEntityName)[0];
            if (relationshipEntityName) {
                const projectionEntity = model.entities.filter(entity => entity.name === relationshipEntityName && entity.type === "PROJECTION")[0];
                if (projectionEntity) {
                    e.hasReferencedProjection = true;
                    e.referencedProjectionProjectName = projectionEntity.projectionReferencedModel.split('/')[2];
                    e.referencedProjectionPerspectiveName = projectionEntity.perspectiveName;
                }
            }
        }
        if (e.importsCode && e.importsCode !== "") {
            e.importsCode = Bytes.byteArrayToText(Base64.decode(e.importsCode));
        }

        e.referencedProjections = [];

        const dataOrderByProperties = e.properties.filter(p => p.dataOrderBy !== undefined);
        if (dataOrderByProperties.length > 0) {
            e.dataOrderBy = dataOrderByProperties[0].dataOrderBy;
            e.dataOrderBySort = dataOrderByProperties.map(p => p.name).join(",");
        }

        e.properties.forEach(p => {
            p.dataNotNull = p.dataNullable === "false";
            p.dataAutoIncrement = p.dataAutoIncrement === "true";
            p.dataNullable = p.dataNullable === "true";
            p.dataPrimaryKey = p.dataPrimaryKey === "true";
            p.dataUnique = p.dataUnique === "true";
            p.isRequiredProperty = p.isRequiredProperty === "true";
            p.isCalculatedProperty = p.isCalculatedProperty === "true";
            p.widgetIsMajor = p.widgetIsMajor === "true";
            p.widgetLabel = p.widgetLabel ? p.widgetLabel : p.name;
            p.widgetDropdownUrl = "";
            p.widgetDropdownControllerUrl = "";

            const parsedDataType = parseDataTypes(p.dataType);
            p.dataTypeJava = parsedDataType.java;
            p.dataTypeTypescript = parsedDataType.ts;

            if (p.dataPrimaryKey) {
                if (e.primaryKeys === undefined) {
                    e.primaryKeys = [];
                }
                e.primaryKeys.push(p.name);
                e.primaryKeysString = e.primaryKeys.join(", ");
            }
            if (p.relationshipType === "COMPOSITION" && p.relationshipCardinality === "1_n") {
                e.masterEntity = p.relationshipEntityName;
                e.masterEntityId = p.name;
                p.widgetIsMajor = false;
                // e.masterEntityPrimaryKey = model.entities.filter(m => m.name === e.masterEntity)[0].properties.filter(k => k.dataPrimaryKey)[0].name;
            }

            if (p.dataTypeTypescript === "string") {
                // TODO minLength is not available in the model and can't be determined
                p.minLength = 0;
                p.maxLength = -1;
                let widgetLength = parseInt(p.widgetLength ? p.widgetLength : '0');
                let dataLength = parseInt(p.dataLength ? p.dataLength : '0')
                p.maxLength = dataLength > widgetLength ? widgetLength : dataLength;
            } else if (p.dataTypeTypescript === "Date") {
                p.isDateType = true;
                e.hasDates = true;
            }
            p.inputRule = p.widgetPattern ? p.widgetPattern : "";

            if ((e.layoutType === "MANAGE_MASTER" || e.layoutType === "LIST_MASTER") && p.widgetIsMajor) {
                if (e.masterProperties == null) {
                    e.masterProperties = {
                        title: null,
                        properties: []
                    };
                }
                if (!p.dataAutoIncrement) {
                    if (e.masterProperties.title == null) {
                        e.masterProperties.title = p;
                    } else {
                        e.masterProperties.properties.push(p);
                    }
                }
            }

            model.entities.forEach(ep => {
                if (p.relationshipEntityName === ep.name) {
                    if (ep.projectionReferencedModel) {
                        const tokens = ep.projectionReferencedModel.split('/');
                        e.referencedProjections.push({
                            name: ep.name,
                            project: tokens[2],
                            genFolderName: tokens[3].substring(0, tokens[3].indexOf('.'))
                        })
                    }
                }
            })

            if (p.widgetType == "DROPDOWN") {
                let projectNameString = `/services/ts/${parameters.projectName}/gen/${parameters.genFolderName}/api/${p.relationshipEntityPerspectiveName}/${p.relationshipEntityName}Service.ts`;
                let projectNameControllerString = `/services/ts/${parameters.projectName}/gen/${parameters.genFolderName}/api/${p.relationshipEntityPerspectiveName}/${p.relationshipEntityName}Controller.ts`;

                e.hasDropdowns = true;

                if (e.referencedProjections.length !== 0) {
                    let foundReferenceProjection = false;
                    e.referencedProjections.forEach(referencedProjection => {
                        if (referencedProjection.name === p.relationshipEntityName && !foundReferenceProjection) {
                            p.widgetDropdownUrl = `/services/ts/${referencedProjection.project}/gen/${referencedProjection.genFolderName}/api/${p.relationshipEntityPerspectiveName}/${p.relationshipEntityName}Service.ts`;
                            p.widgetDropdownControllerUrl = `/services/ts/${referencedProjection.project}/gen/${referencedProjection.genFolderName}/api/${p.relationshipEntityPerspectiveName}/${p.relationshipEntityName}Controller.ts`;
                            foundReferenceProjection = true;
                        }
                    });
                    if (!foundReferenceProjection) {
                        p.widgetDropdownUrl = projectNameString;
                        p.widgetDropdownControllerUrl = projectNameControllerString;
                    }
                } else {
                    p.widgetDropdownUrl = projectNameString;
                    p.widgetDropdownControllerUrl = projectNameControllerString;
                }
            }
        });
    });

    parameters.perspectives = {};

    model.entities.forEach(e => {
        if (e.perspectiveName) {
            if (parameters.perspectives[e.perspectiveName] == null) {
                parameters.perspectives[e.perspectiveName] = {
                    views: []
                };
            }
            parameters.perspectives[e.perspectiveName].name = e.perspectiveName;
            parameters.perspectives[e.perspectiveName].label = e.perspectiveName;
            parameters.perspectives[e.perspectiveName].header = e.perspectiveHeader;
            parameters.perspectives[e.perspectiveName].order = e.perspectiveOrder;
            parameters.perspectives[e.perspectiveName].navId = e.perspectiveNavId;
            parameters.perspectives[e.perspectiveName].icon = e.perspectiveIcon;
            parameters.perspectives[e.perspectiveName].role = e.perspectiveRole;
            parameters.perspectives[e.perspectiveName].views.push(e.name);
        }
    });

    parameters.roles = [];

    model.entities.forEach(e => {
        if (e.generateDefaultRoles === "true") {
            if (e.type != "PROJECTION") {

                const rolePair = {};
                rolePair["entityName"] = e.name;

                if (e.roleRead && e.roleRead != "") {
                    rolePair["roleRead"] = e.roleRead;
                }

                if (e.type != "REPORT" && e.type != "FILTER") {
                    if (e.roleWrite && e.roleWrite != "") {
                        rolePair["roleWrite"] = e.roleWrite;
                    }
                }

                parameters.roles.push(rolePair);
            }
        }
    })
}

export function getUniqueParameters(...parameters) {
    const uniqueTemplateParameters = [];
    const parametersMap = new Map();

    for (const templateParameters of parameters) {
        for (const parameter of templateParameters) {
            parametersMap.set(parameter.name, parameter);
        }
    }

    for (const next of parametersMap.values()) {
        uniqueTemplateParameters.push(next);
    }
    return uniqueTemplateParameters;
}

export function parseDataTypes(dataType) {
    const parsedDataType = {
        java: '',
        ts: ''
    };
    switch (dataType.toUpperCase()) {
        case "TINYINT":
        case "INT1":
        case "SMALLINT":
        case "INT2":
        case "SMALLSERIAL":
            parsedDataType.java = "short";
            parsedDataType.ts = "number";
            break;
        case "MEDIUMINT":
        case "INT3":
        case "INT":
        case "INT4":
        case "INTEGER":
        case "SERIAL":
            parsedDataType.java = "int";
            parsedDataType.ts = "number";
            break;
        case "BIGINT":
        case "INT8":
        case "BIGSERIAL":
            parsedDataType.java = "long";
            parsedDataType.ts = "number";
            break;
        case "DECIMAL":
        case "DEC":
        case "NUMERIC":
        case "FIXED":
        case "DOUBLE":
        case "DOUBLE PRECISION":
        case "REAL":
            parsedDataType.java = "double";
            parsedDataType.ts = "number";
            break;
        case "FLOAT":
        case "MONEY":
            parsedDataType.java = "float";
            parsedDataType.ts = "number";
            break;
        case "CHAR":
        case "ENUM":
        case "INET4":
        case "INET6":
        case "TEXT":
        case "TINYTEXT":
        case "MEDIUMTEXT":
        case "LONGTEXT":
        case "VARCHAR":
        case "LONG VARCHAR":
        case "CHARACTER VARYING":
        case "CHARACTER":
        case "BPCHAR":
            parsedDataType.java = "string";
            parsedDataType.ts = "string";
            break;
        case "DATE":
            parsedDataType.java = "date";
            parsedDataType.ts = "Date";
            break;
        case "TIME":
        case "TIME WITH TIME ZONE":
            parsedDataType.java = "time";
            parsedDataType.ts = "Date";
            break;
        case "DATETIME":
        case "TIMESTAMP":
        case "TIMESTAMP WITH TIME ZONE":
            parsedDataType.java = "timestamp";
            parsedDataType.ts = "Date";
            break;
        case "BOOLEAN":
        case "BIT":
            parsedDataType.java = "boolean";
            parsedDataType.ts = "boolean";
            break;
        case "NULL":
            parsedDataType.java = "null";
            parsedDataType.ts = "null";
            break;
        default:
            parsedDataType.ts = "unknown";
    }

    return parsedDataType;
}
