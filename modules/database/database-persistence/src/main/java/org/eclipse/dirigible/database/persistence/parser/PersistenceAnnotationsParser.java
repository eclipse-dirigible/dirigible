/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence.parser;

import static java.text.MessageFormat.format;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.database.persistence.PersistenceException;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableColumnModel;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.DataTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * The Persistence Annotations Parser.
 */
public class PersistenceAnnotationsParser {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(PersistenceAnnotationsParser.class);

    /** The Constant MODELS_CACHE. */
    private static final Map<Class, PersistenceTableModel> MODELS_CACHE =
            Collections.synchronizedMap(new HashMap<Class, PersistenceTableModel>());

    /**
     * Parses the pojo.
     *
     * @param pojo the pojo
     * @return the persistence table model
     * @throws PersistenceException the persistence exception
     */
    public PersistenceTableModel parsePojo(Object pojo) throws PersistenceException {
        Class<? extends Object> clazz = pojo.getClass();
        PersistenceTableModel persistenceTableModel = parseTable(clazz);
        return persistenceTableModel;
    }

    /**
     * Parses the pojo.
     *
     * @param clazz the clazz
     * @return the persistence table model
     * @throws PersistenceException the persistence exception
     */
    public PersistenceTableModel parsePojo(Class<? extends Object> clazz) throws PersistenceException {
        PersistenceTableModel persistenceTableModel = parseTable(clazz);
        return persistenceTableModel;
    }

    /**
     * Parses the table.
     *
     * @param clazz the clazz
     * @return the persistence table model
     */
    private PersistenceTableModel parseTable(Class<? extends Object> clazz) {

        PersistenceTableModel persistenceTableModel = MODELS_CACHE.get(clazz);

        if (persistenceTableModel != null) {
            return persistenceTableModel;
        }

        Annotation annotation = getTableAnnotation(clazz);
        if (annotation == null) {
            throw new PersistenceException(format("No Table annotation found in Class {0}", clazz));
        }
        Table table = (Table) annotation;
        persistenceTableModel = new PersistenceTableModel();
        persistenceTableModel.setClassName(clazz.getCanonicalName());
        if (table.schema() == null) {
            throw new PersistenceException(
                    format("Table Name is mandatory, but it is not present in Class [{0}]", clazz.getCanonicalName()));
        }
        persistenceTableModel.setTableName(table.name());
        if (table.schema() != null) {
            persistenceTableModel.setSchemaName(table.schema());
        }

        parseColumns(clazz, persistenceTableModel);

        MODELS_CACHE.put(clazz, persistenceTableModel);

        return persistenceTableModel;
    }

    /**
     * Gets the table annotation.
     *
     * @param clazz the clazz
     * @return the table annotation
     */
    private Annotation getTableAnnotation(Class<? extends Object> clazz) {
        Annotation annotation = clazz.getAnnotation(Table.class);
        if ((annotation == null) && (clazz.getSuperclass() != null)) {
            return getTableAnnotation(clazz.getSuperclass());
        }
        return annotation;
    }

    /**
     * Parses the columns.
     *
     * @param clazz the clazz
     * @param persistenceModel the persistence model
     */
    private void parseColumns(Class<? extends Object> clazz, PersistenceTableModel persistenceModel) {
        Field[] fields = collectFields(clazz);
        for (Field field : fields) {
            // use annotation @Transient always to show it is not a persistent column
            // boolean isTransient = Modifier.isTransient(field.getModifiers());
            // if (isTransient) {
            // continue;
            // }
            // @Column
            Annotation annotationColumn = field.getAnnotation(Column.class);
            if (annotationColumn == null) {
                Annotation annotationTransient = field.getAnnotation(Transient.class);
                if (annotationTransient == null) {
                    if (logger.isWarnEnabled()) {
                        logger.warn(format("No Column nor Transient annotation found in Class {0} and Field {1}", clazz, field.getName()));
                    }
                }
                continue;
            }
            Column column = (Column) annotationColumn;
            if (column.name() == null) {
                throw new PersistenceException(format("Column Name is mandatory, but it is not present in Class [{0}] and Field [{1}]",
                        clazz.getCanonicalName(), field.getName()));
            }
            String type = column.columnDefinition();
            if (type == null) {
                type = DataTypeUtils.getDatabaseTypeNameByJavaType(field.getType()
                                                                        .getClass());
            }
            int length = column.length();
            if ((length == 0) && (DataTypeUtils.getDatabaseTypeByJavaType(field.getType()
                                                                               .getClass()) == Types.VARCHAR)) {
                length = DataTypeUtils.VARCHAR_DEFAULT_LENGTH;
            }
            if ((length == 0) && (DataTypeUtils.getDatabaseTypeByJavaType(field.getType()
                                                                               .getClass()) == Types.DECIMAL)) {
                length = DataTypeUtils.DECIMAL_DEFAULT_LENGTH;
            }
            boolean unique = column.unique();
            // @Id
            Annotation annotationId = field.getAnnotation(Id.class);
            boolean primaryKey = annotationId != null;
            // @GeneratedValue
            String generated = null;
            boolean identity = false;
            Annotation annotationGeneratedValue = field.getAnnotation(GeneratedValue.class);
            if (annotationGeneratedValue != null) {
                GeneratedValue generatedValue = (GeneratedValue) annotationGeneratedValue;
                if ((generatedValue.strategy() == null) || GenerationType.AUTO.equals(generatedValue.strategy())) {
                    generated = GenerationType.TABLE.name();
                } else {
                    if (!GenerationType.SEQUENCE.equals(generatedValue.strategy())
                            && !GenerationType.TABLE.equals(generatedValue.strategy())
                            && !GenerationType.IDENTITY.equals(generatedValue.strategy())) {
                        throw new IllegalArgumentException(format("Generation Type: [{0}] not supported.", generatedValue.strategy()
                                                                                                                         .name()));
                    }
                    if (GenerationType.IDENTITY.equals(generatedValue.strategy())) {
                        if (DataTypeUtils.isBigint(type) || DataTypeUtils.isVarchar(type) || DataTypeUtils.isNvarchar(type)
                                || DataTypeUtils.isChar(type) || DataTypeUtils.isInteger(type) || DataTypeUtils.isDecimal(type)) {
                            identity = true;
                        } else {
                            throw new IllegalArgumentException(
                                    "Identity columns must of type CHAR, VARCHAR, NVARCHAR, INTEGER, BIGINT or DECIMAL");
                        }
                    }
                    generated = generatedValue.strategy()
                                              .name();
                }
            }

            // @Enumerated
            String enumerated = null;
            Annotation annotationEnumerated = field.getAnnotation(Enumerated.class);
            if (annotationEnumerated != null) {
                Enumerated enumeratedAnnotation = (Enumerated) annotationEnumerated;
                EnumType enumType = enumeratedAnnotation.value();
                enumerated = enumType.name();
                if (EnumType.ORDINAL.equals(enumType)) {
                    type = DataType.INTEGER.name();
                } else {
                    type = DataType.VARCHAR.name();
                    length = DataTypeUtils.VARCHAR_DEFAULT_LENGTH;
                }
            }

            PersistenceTableColumnModel persistenceTableColumnModel = new PersistenceTableColumnModel(field.getName(), column.name(), type,
                    length, column.nullable(), primaryKey, column.scale(), generated, unique, identity, enumerated);
            persistenceModel.getColumns()
                            .add(persistenceTableColumnModel);
        }

    }

    /**
     * Collect fields.
     *
     * @param clazz the clazz
     * @return the field[]
     */
    public static Field[] collectFields(Class<? extends Object> clazz) {
        List<Field> fields = new ArrayList<Field>();
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        if (clazz.getSuperclass() != null) {
            collectFieldsFromSuperclass(clazz.getSuperclass(), fields);
        }
        return fields.toArray(new Field[] {});
    }

    /**
     * Collect fields from superclass.
     *
     * @param clazz the clazz
     * @param fields the fields
     */
    private static void collectFieldsFromSuperclass(Class<?> clazz, List<Field> fields) {
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        if (clazz.getSuperclass() != null) {
            collectFieldsFromSuperclass(clazz.getSuperclass(), fields);
        }
    }

}
