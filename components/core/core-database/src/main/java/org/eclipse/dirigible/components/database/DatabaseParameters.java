/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database;

/**
 * The Interface DatabaseParameters.
 */
public interface DatabaseParameters {

    /** The Constant DIRIGIBLE_DATABASE_DEFAULT_QUERY_LIMIT. */
    String DIRIGIBLE_DATABASE_DEFAULT_QUERY_LIMIT = "DIRIGIBLE_DATABASE_DEFAULT_QUERY_LIMIT";

    /** The Constant DIRIGIBLE_DATABASE_H2_ROOT_FOLDER. */
    String DIRIGIBLE_DATABASE_H2_ROOT_FOLDER = "DIRIGIBLE_DATABASE_H2_ROOT_FOLDER"; //$NON-NLS-1$

    /** The Constant DIRIGIBLE_DATABASE_H2_ROOT_FOLDER_DEFAULT. */
    String DIRIGIBLE_DATABASE_H2_ROOT_FOLDER_DEFAULT = DIRIGIBLE_DATABASE_H2_ROOT_FOLDER + "_DEFAULT"; //$NON-NLS-1$

    /** The Constant SYSTEM_TABLE. */
    String SYSTEM_TABLE = "SYSTEM TABLE"; //$NON-NLS-1$

    /** The Constant LOCAL_TEMPORARY. */
    String LOCAL_TEMPORARY = "LOCAL TEMPORARY"; //$NON-NLS-1$

    /** The Constant GLOBAL_TEMPORARY. */
    String GLOBAL_TEMPORARY = "GLOBAL TEMPORARY"; //$NON-NLS-1$

    /** The Constant SYNONYM. */
    String SYNONYM = "SYNONYM"; //$NON-NLS-1$

    /** The Constant ALIAS. */
    String ALIAS = "ALIAS"; //$NON-NLS-1$

    /** The Constant VIEW. */
    String VIEW = "VIEW"; //$NON-NLS-1$

    /** The Constant TABLE. */
    String TABLE = "TABLE"; //$NON-NLS-1$

    /** The Constant TABLE_TYPES. */
    String[] TABLE_TYPES = {TABLE, VIEW, ALIAS, SYNONYM, GLOBAL_TEMPORARY, LOCAL_TEMPORARY, SYSTEM_TABLE};

    /** The Constant PRCNT. */
    String PRCNT = "%"; //$NON-NLS-1$

    /** The Constant COLUMN_NAME. */
    String COLUMN_NAME = "COLUMN_NAME"; //$NON-NLS-1$

    /** The Constant COLUMN_TYPE. */
    String COLUMN_TYPE = "COLUMN_TYPE"; //$NON-NLS-1$

    /** The Constant TYPE_NAME. */
    String TYPE_NAME = "TYPE_NAME"; //$NON-NLS-1$

    /** The Constant COLUMN_SIZE. */
    String COLUMN_SIZE = "COLUMN_SIZE"; //$NON-NLS-1$

    /** The Constant EMPTY. */
    String EMPTY = ""; //$NON-NLS-1$

    /** The Constant PK. */
    String PK = "PK"; //$NON-NLS-1$

    /** The Constant IS_NULLABLE. */
    String IS_NULLABLE = "IS_NULLABLE"; //$NON-NLS-1$

    /** The Constant INDEX_NAME. */
    String INDEX_NAME = "INDEX_NAME"; //$NON-NLS-1$

    /** The Constant TYPE_INDEX. */
    String TYPE_INDEX = "TYPE"; //$NON-NLS-1$

    /** The Constant NON_UNIQUE. */
    String NON_UNIQUE = "NON_UNIQUE"; //$NON-NLS-1$

    /** The Constant INDEX_QUALIFIER. */
    String INDEX_QUALIFIER = "INDEX_QUALIFIER"; //$NON-NLS-1$

    /** The Constant ORDINAL_POSITION. */
    String ORDINAL_POSITION = "ORDINAL_POSITION"; //$NON-NLS-1$

    /** The Constant ASC_OR_DESC. */
    String ASC_OR_DESC = "ASC_OR_DESC"; //$NON-NLS-1$

    /** The Constant CARDINALITY. */
    String CARDINALITY = "CARDINALITY"; //$NON-NLS-1$

    /** The Constant PAGES_INDEX. */
    String PAGES_INDEX = "PAGES"; //$NON-NLS-1$

    /** The Constant FILTER_CONDITION. */
    String FILTER_CONDITION = "FILTER_CONDITION"; //$NON-NLS-1$

    /** The Constant PRECISION. */
    String PRECISION = "PRECISION"; //$NON-NLS-1$

    /** The Constant LENGTH. */
    String LENGTH = "LENGTH"; //$NON-NLS-1$

    /** The Constant SCALE. */
    String SCALE = "SCALE"; //$NON-NLS-1$

    /** The Constant RADIX. */
    String RADIX = "RADIX"; //$NON-NLS-1$

    /** The Constant NULLABLE. */
    String NULLABLE = "NULLABLE"; //$NON-NLS-1$

    /** The Constant REMARKS. */
    String REMARKS = "REMARKS"; //$NON-NLS-1$

    /** The Constant DECIMAL_DIGITS. */
    String DECIMAL_DIGITS = "DECIMAL_DIGITS"; //$NON-NLS-1$

    /** The Constant FK_NAME. */
    String FK_NAME = "FK_NAME"; //$NON-NLS-1$

    /** The Constant JDBC_TABLE_NAME_PROPERTY. */
    String JDBC_TABLE_NAME_PROPERTY = "TABLE_NAME";

    /** The Constant JDBC_TABLE_SCHEME_PROPERTY. */
    String JDBC_TABLE_SCHEME_PROPERTY = "TABLE_SCHEM";

    /** The Constant JDBC_TABLE_TYPE_PROPERTY. */
    String JDBC_TABLE_TYPE_PROPERTY = "TABLE_TYPE";

    /** The Constant JDBC_COLUMN_NAME_PROPERTY. */
    String JDBC_COLUMN_NAME_PROPERTY = "COLUMN_NAME";

    /** The Constant JDBC_COLUMN_TYPE_PROPERTY. */
    String JDBC_COLUMN_TYPE_PROPERTY = "TYPE_NAME";

    /** The Constant JDBC_COLUMN_NULLABLE_PROPERTY. */
    String JDBC_COLUMN_NULLABLE_PROPERTY = "NULLABLE";

    String JDBC_COLUMN_DEFAULT_VALUE_PROPERTY = "COLUMN_DEF";

    /** The Constant JDBC_COLUMN_SIZE_PROPERTY. */
    String JDBC_COLUMN_SIZE_PROPERTY = "COLUMN_SIZE";

    /** The Constant JDBC_COLUMN_DECIMAL_DIGITS_PROPERTY. */
    String JDBC_COLUMN_DECIMAL_DIGITS_PROPERTY = "DECIMAL_DIGITS";

    /** The Constant JDBC_FK_TABLE_NAME_PROPERTY. */
    String JDBC_FK_TABLE_NAME_PROPERTY = "FKTABLE_NAME";

    /** The Constant JDBC_FK_NAME_PROPERTY. */
    String JDBC_FK_NAME_PROPERTY = "FK_NAME";

    /** The Constant JDBC_PK_NAME_PROPERTY. */
    String JDBC_PK_NAME_PROPERTY = "PK_NAME";

    /** The Constant JDBC_PK_TABLE_NAME_PROPERTY. */
    String JDBC_PK_TABLE_NAME_PROPERTY = "PKTABLE_NAME";

    /** The Constant JDBC_PK_TABLE_NAME_PROPERTY. */
    String JDBC_PK_SCHEMA_NAME_PROPERTY = "PKTABLE_SCHEM";

    /** The Constant JDBC_FK_COLUMN_NAME_PROPERTY. */
    String JDBC_FK_COLUMN_NAME_PROPERTY = "FKCOLUMN_NAME";

    /** The Constant JDBC_PK_COLUMN_NAME_PROPERTY. */
    String JDBC_PK_COLUMN_NAME_PROPERTY = "PKCOLUMN_NAME";

    /** The Constant JDBC_FILTER_CONDITION_PROPERTY. */
    String JDBC_FILTER_CONDITION_PROPERTY = "FILTER_CONDITION";

}
