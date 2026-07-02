/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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
    parameters.javaGenFolderName = sanitizeJavaIdentifier(parameters.genFolderName);

    model.entities.forEach(e => {
        if (parameters.dataSource && !e.dataSource) {
            e.dataSource = parameters.dataSource;
        } else {
            const defaultDataSourceName = Configurations.get("DIRIGIBLE_DATABASE_DATASOURCE_NAME_DEFAULT", "DefaultDB");
            e.dataSource = defaultDataSourceName;
            parameters.dataSource = defaultDataSourceName;
        }
        e.javaPerspectiveName = sanitizeJavaIdentifier(e.perspectiveName);
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
			p.isReadOnlyProperty = p.isReadOnlyProperty === "true";
            p.widgetIsMajor = p.widgetIsMajor === "true";
            p.widgetLabel = p.widgetLabel ? p.widgetLabel : p.name;

            if (p.name === "ProcessId") {
                e.hasProcess = true;
            }
            p.widgetDropdownUrl = "";
            p.widgetDropdownControllerUrl = "";

            const parsedDataType = parseDataTypes(p.dataType);
            p.dataTypeJava = parsedDataType.java;
            p.dataTypeTypescript = parsedDataType.ts;
            p.dataTypeJavaClass = resolveJavaClass(parsedDataType.javaClass, p.auditType);

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
            } else if (p.dataTypeTypescript === "number") {
                // All numeric values are right-aligned in tables. Floats are additionally formatted with
                // the field's display pattern (the model's Pattern/widgetPattern), defaulting to grouped
                // thousands with two decimals.
                p.isNumberType = true;
                const dt = (p.dataType || "").toUpperCase();
                if (dt === "DECIMAL" || dt === "DOUBLE" || dt === "FLOAT" || dt === "REAL") {
                    p.isFloatType = true;
                    p.formatPattern = (p.widgetPattern && p.widgetPattern.trim()) ? p.widgetPattern : "### ### ### ##0.00";
                    e.hasFloats = true;
                }
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

            // DOCUMENT_STATUS is a dropdown-backed FK rendered as a status pill: it still needs the
            // lookup controller URL built here so the UI can resolve the FK id to the status name.
            if (p.widgetType == "DROPDOWN" || p.widgetType == "DOCUMENT_STATUS") {
                e.hasDropdowns = true;

                let targetProject = parameters.projectName;
                let targetGenFolder = parameters.genFolderName;
                if (e.referencedProjections.length !== 0) {
                    const referencedProjection = e.referencedProjections.find(rp => rp.name === p.relationshipEntityName);
                    if (referencedProjection) {
                        targetProject = referencedProjection.project;
                        targetGenFolder = referencedProjection.genFolderName;
                    }
                }

                if (parameters.javaRuntime) {
                    const javaGen = sanitizeJavaIdentifier(targetGenFolder);
                    const javaPerspective = sanitizeJavaIdentifier(p.relationshipEntityPerspectiveName);
                    const javaUrl = `/services/java/${targetProject}/gen/${javaGen}/api/${javaPerspective}/${p.relationshipEntityName}Controller`;
                    p.widgetDropdownUrl = javaUrl;
                    p.widgetDropdownControllerUrl = javaUrl;
                } else {
                    p.widgetDropdownUrl = `/services/ts/${targetProject}/gen/${targetGenFolder}/api/${p.relationshipEntityPerspectiveName}/${p.relationshipEntityName}Service.ts`;
                    p.widgetDropdownControllerUrl = `/services/ts/${targetProject}/gen/${targetGenFolder}/api/${p.relationshipEntityPerspectiveName}/${p.relationshipEntityName}Controller.ts`;
                }
            }
        });
    });

    // A dependsOn dependent widget needs its TRIGGER property's controller URL at runtime (the
    // generated form loads the trigger's selected record to read widgetDependsOnValueFrom). Resolved
    // in a second pass so it works regardless of property order, sparing the templates a sibling
    // lookup. The trigger is always a dropdown FK, so its controller URL was built above.
    model.entities.forEach(e => {
        e.properties.forEach(p => {
            if (p.widgetDependsOnProperty) {
                const trigger = e.properties.find(t => t.name === p.widgetDependsOnProperty);
                if (trigger && trigger.widgetDropdownControllerUrl) {
                    p.widgetDependsOnControllerUrl = trigger.widgetDropdownControllerUrl;
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
        if (e && e.generateDefaultRoles === "true") {
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
        ts: '',
        javaClass: 'Object'
    };
    switch (dataType.toUpperCase()) {
        case "TINYINT":
        case "INT1":
        case "SMALLINT":
        case "INT2":
        case "SMALLSERIAL":
            parsedDataType.java = "short";
            parsedDataType.ts = "number";
            parsedDataType.javaClass = "Short";
            break;
        case "MEDIUMINT":
        case "INT3":
        case "INT":
        case "INT4":
        case "INTEGER":
        case "SERIAL":
            parsedDataType.java = "int";
            parsedDataType.ts = "number";
            parsedDataType.javaClass = "Integer";
            break;
        case "BIGINT":
        case "INT8":
        case "BIGSERIAL":
            parsedDataType.java = "long";
            parsedDataType.ts = "number";
            parsedDataType.javaClass = "Long";
            break;
        case "DECIMAL":
        case "DEC":
        case "NUMERIC":
        case "FIXED":
            parsedDataType.java = "double";
            parsedDataType.ts = "number";
            parsedDataType.javaClass = "java.math.BigDecimal";
            break;
        case "DOUBLE":
        case "DOUBLE PRECISION":
        case "REAL":
            parsedDataType.java = "double";
            parsedDataType.ts = "number";
            parsedDataType.javaClass = "Double";
            break;
        case "FLOAT":
        case "MONEY":
            parsedDataType.java = "float";
            parsedDataType.ts = "number";
            parsedDataType.javaClass = "Float";
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
        case "CLOB":
            parsedDataType.java = "string";
            parsedDataType.ts = "string";
            parsedDataType.javaClass = "String";
            break;
        case "DATE":
            parsedDataType.java = "date";
            parsedDataType.ts = "Date";
            parsedDataType.javaClass = "java.time.LocalDate";
            break;
        case "TIME":
        case "TIME WITH TIME ZONE":
            parsedDataType.java = "time";
            parsedDataType.ts = "string";
            parsedDataType.javaClass = "java.time.LocalTime";
            break;
        case "DATETIME":
        case "TIMESTAMP":
        case "TIMESTAMP WITH TIME ZONE":
            parsedDataType.java = "timestamp";
            parsedDataType.ts = "Date";
            parsedDataType.javaClass = "java.time.Instant";
            break;
        case "BOOLEAN":
        case "BIT":
            parsedDataType.java = "boolean";
            parsedDataType.ts = "boolean";
            parsedDataType.javaClass = "Boolean";
            break;
        case "BLOB":
            parsedDataType.java = "blob";
            parsedDataType.ts = "string";
            parsedDataType.javaClass = "byte[]";
            break;
        case "NULL":
            parsedDataType.java = "null";
            parsedDataType.ts = "null";
            parsedDataType.javaClass = "Object";
            break;
        default:
            parsedDataType.ts = "unknown";
            parsedDataType.javaClass = "Object";
    }

    return parsedDataType;
}

/**
 * Resolves the Java class name to emit for a property, applying the audit-field overrides demanded
 * by org.eclipse.dirigible.sdk.db.{CreatedAt,UpdatedAt,CreatedBy,UpdatedBy}.
 */
export function resolveJavaClass(baseJavaClass, auditType) {
    if (auditType === "CREATED_AT" || auditType === "UPDATED_AT") {
        return "java.time.Instant";
    }
    if (auditType === "CREATED_BY" || auditType === "UPDATED_BY") {
        return "String";
    }
    return baseJavaClass || "Object";
}

/**
 * Sanitises an arbitrary name (perspective, model folder) into a lower-case Java identifier safe
 * for use both as a path segment and a package fragment.
 */
export function sanitizeJavaIdentifier(name) {
    if (name === undefined || name === null || name === "") {
        return "_";
    }
    let s = String(name).toLowerCase().replace(/[^a-z0-9_]/g, '_');
    if (/^[0-9]/.test(s)) {
        s = '_' + s;
    }
    return s === "" ? "_" : s;
}
