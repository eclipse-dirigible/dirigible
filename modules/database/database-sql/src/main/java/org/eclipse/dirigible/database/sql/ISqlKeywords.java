/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The Interface ISqlKeywords.
 */
public interface ISqlKeywords {

    /**
     * The Constant KEYWORD_SELECT.
     */
    String KEYWORD_SELECT = "SELECT"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_DISTINCT.
     */
    String KEYWORD_DISTINCT = "DISTINCT"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_FROM.
     */
    String KEYWORD_FROM = "FROM"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_JOIN.
     */
    String KEYWORD_JOIN = "JOIN"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_INNER.
     */
    String KEYWORD_INNER = "INNER"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_OUTER.
     */
    String KEYWORD_OUTER = "OUTER"; //$NON-NLS-1$

    String KEYWORD_FULL_OUTER = "FULL OUTER"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_LEFT.
     */
    String KEYWORD_LEFT = "LEFT"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_RIGHT.
     */
    String KEYWORD_RIGHT = "RIGHT"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_FULL.
     */
    String KEYWORD_FULL = "FULL"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_WHERE.
     */
    String KEYWORD_WHERE = "WHERE"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_GROUP_BY.
     */
    String KEYWORD_GROUP_BY = "GROUP BY"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_HAVING.
     */
    String KEYWORD_HAVING = "HAVING"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_ORDER_BY.
     */
    String KEYWORD_ORDER_BY = "ORDER BY"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_UNION.
     */
    String KEYWORD_UNION = "UNION"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_ASC.
     */
    String KEYWORD_ASC = "ASC"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_DESC.
     */
    String KEYWORD_DESC = "DESC"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_AND.
     */
    String KEYWORD_AND = "AND"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_OR.
     */
    String KEYWORD_OR = "OR"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_AS.
     */
    String KEYWORD_AS = "AS"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_ON.
     */
    String KEYWORD_ON = "ON"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_LIMIT.
     */
    String KEYWORD_LIMIT = "LIMIT"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_OFFSET.
     */
    String KEYWORD_OFFSET = "OFFSET"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_INSERT.
     */
    String KEYWORD_INSERT = "INSERT"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_INTO.
     */
    String KEYWORD_INTO = "INTO"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_VALUES.
     */
    String KEYWORD_VALUES = "VALUES"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_UPDATE.
     */
    String KEYWORD_UPDATE = "UPDATE"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_SET.
     */
    String KEYWORD_SET = "SET"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_CREATE.
     */
    String KEYWORD_CREATE = "CREATE"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_ALTER.
     */
    String KEYWORD_ALTER = "ALTER"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_TABLE.
     */
    String KEYWORD_TABLE = "TABLE"; //$NON-NLS-1$

    String KEYWORD_TOP = "TOP";

    /**
     * The Constant KEYWORD_PRIMARY.
     */
    String KEYWORD_PRIMARY = "PRIMARY"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_FOREIGN.
     */
    String KEYWORD_FOREIGN = "FOREIGN"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_REFERENCES.
     */
    String KEYWORD_REFERENCES = "REFERENCES"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_KEY.
     */
    String KEYWORD_KEY = "KEY"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_ADD.
     */
    String KEYWORD_ADD = "ADD"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_DROP.
     */
    String KEYWORD_DROP = "DROP"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_DELETE.
     */
    String KEYWORD_DELETE = "DELETE"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_NOT.
     */
    String KEYWORD_NOT = "NOT"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_NULL.
     */
    String KEYWORD_NULL = "NULL"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_SEQUENCE.
     */
    String KEYWORD_SEQUENCE = "SEQUENCE"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_SEQUENCE_START_WITH.
     */
    String KEYWORD_SEQUENCE_START_WITH = "START WITH";

    /**
     * The Constant KEYWORD_SEQUENCE_RESTART_WITH.
     */
    String KEYWORD_SEQUENCE_RESTART_WITH = "RESTART WITH";

    /**
     * The Constant KEYWORD_SEQUENCE_INCREMENT_BY.
     */
    String KEYWORD_SEQUENCE_INCREMENT_BY = "INCREMENT BY";

    /**
     * The Constant KEYWORD_SEQUENCE_MAXVALUE.
     */
    String KEYWORD_SEQUENCE_MAXVALUE = "MAXVALUE";

    /**
     * The Constant KEYWORD_SEQUENCE_NO_MAXVALUE.
     */
    String KEYWORD_SEQUENCE_NO_MAXVALUE = "NO MAXVALUE";

    /**
     * The Constant KEYWORD_SEQUENCE_MINVALUE.
     */
    String KEYWORD_SEQUENCE_MINVALUE = "MINVALUE";

    /**
     * The Constant KEYWORD_SEQUENCE_NO_MINVALUE.
     */
    String KEYWORD_SEQUENCE_NO_MINVALUE = "NO MINVALUE";

    /**
     * The Constant KEYWORD_SEQUENCE_CYCLE.
     */
    String KEYWORD_SEQUENCE_CYCLE = "CYCLE";

    /**
     * The Constant KEYWORD_SEQUENCE_RESET_BY.
     */
    String KEYWORD_SEQUENCE_RESET_BY = "RESET BY";

    /**
     * The Constant KEYWORD_DATABASE_DROP_RESTRICT.
     */
    String KEYWORD_DATABASE_DROP_RESTRICT = "RESTRICT";

    String KEYWORD_DATABASE_DROP_CASCADE = "CASCADE";

    /**
     * The Constant KEYWORD_SEQUENCE_EMPTY_STRING_FOR_BOOLEAN_TYPES.
     */
    String KEYWORD_SEQUENCE_EMPTY_STRING_FOR_BOOLEAN_TYPES = "";

    /**
     * The Constant KEYWORD_SYNONYM.
     */
    String KEYWORD_SYNONYM = "SYNONYM"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_NEXT_VALUE_FOR.
     */
    String KEYWORD_NEXT_VALUE_FOR = "NEXT VALUE FOR"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_UNIQUE.
     */
    String KEYWORD_UNIQUE = "UNIQUE"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_INDEX.
     */
    String KEYWORD_INDEX = "INDEX"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_CHECK.
     */
    String KEYWORD_CHECK = "CHECK"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_COLUMN.
     */
    String KEYWORD_COLUMN = "COLUMN"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_ROW.
     */
    String KEYWORD_ROW = "ROW"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_ROWS.
     */
    String KEYWORD_ROWS = "ROWS"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_VIEW.
     */
    String KEYWORD_VIEW = "VIEW"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_FOR_UPDATE.
     */
    String KEYWORD_FOR_UPDATE = "FOR UPDATE"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_CONSTRAINT.
     */
    String KEYWORD_CONSTRAINT = "CONSTRAINT"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_START.
     */
    String KEYWORD_START = "START"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_WITH.
     */
    String KEYWORD_WITH = "WITH"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_FETCH.
     */
    String KEYWORD_FETCH = "FETCH"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_NEXT.
     */
    String KEYWORD_NEXT = "NEXT"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_ONLY.
     */
    String KEYWORD_ONLY = "ONLY"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_PUBLIC.
     */
    String KEYWORD_PUBLIC = "PUBLIC"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_FOR.
     */
    String KEYWORD_FOR = "FOR"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_IDENTITY.
     */
    String KEYWORD_IDENTITY = "IDENTITY"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_SCHEMA.
     */
    String KEYWORD_SCHEMA = "SCHEMA"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_TABLE_TYPE.
     */
    String KEYWORD_TABLE_TYPE = "TYPE"; //$NON-NLS-1$

    /**
     * The Constant FUNCTION_CURRENT_DATE.
     */
    String FUNCTION_CURRENT_DATE = "CURRENT_DATE"; //$NON-NLS-1$

    /**
     * The Constant FUNCTION_CURRENT_TIME.
     */
    String FUNCTION_CURRENT_TIME = "CURRENT_TIME"; //$NON-NLS-1$

    /**
     * The Constant FUNCTION_CURRENT_TIMESTAMP.
     */
    String FUNCTION_CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP"; //$NON-NLS-1$

    /**
     * The Constant COMMA.
     */
    String COMMA = ","; //$NON-NLS-1$

    /**
     * The Constant DOT.
     */
    String DOT = "."; //$NON-NLS-1$

    /**
     * The Constant SPACE.
     */
    String SPACE = " "; //$NON-NLS-1$

    /**
     * The Constant OPEN.
     */
    String OPEN = "("; //$NON-NLS-1$

    /**
     * The Constant CLOSE.
     */
    String CLOSE = ")"; //$NON-NLS-1$

    /**
     * The Constant QUESTION.
     */
    String QUESTION = "?"; //$NON-NLS-1$

    /**
     * The Constant EQUALS.
     */
    String EQUALS = "="; //$NON-NLS-1$

    /**
     * The Constant UNDERSCROE.
     */
    String UNDERSCROE = "_"; //$NON-NLS-1$

    /**
     * The Constant STAR.
     */
    String STAR = "*"; //$NON-NLS-1$

    /**
     * The Constant SEMICOLON.
     */
    String SEMICOLON = ";"; //$NON-NLS-1$

    /**
     * The Constant ALTER.
     */
    String ALTER = "ALTER"; //$NON-NLS-1$

    /**
     * The Constant TABLE.
     */
    String TABLE = "TABLE"; //$NON-NLS-1$

    /**
     * The Constant METADATA_SYSTEM_TABLE.
     */
    String METADATA_SYSTEM_TABLE = "SYSTEM TABLE"; //$NON-NLS-1$

    /**
     * The Constant METADATA_BASE_TABLE.
     */
    String METADATA_BASE_TABLE = "BASE TABLE"; //$NON-NLS-1$

    /**
     * The Constant METADATA_LOCAL_TEMPORARY.
     */
    String METADATA_LOCAL_TEMPORARY = "LOCAL TEMPORARY"; //$NON-NLS-1$

    /**
     * The Constant METADATA_GLOBAL_TEMPORARY.
     */
    String METADATA_GLOBAL_TEMPORARY = "GLOBAL TEMPORARY"; //$NON-NLS-1$

    /**
     * The Constant METADATA_GLOBAL_TEMPORARY_COLUMN.
     */
    String METADATA_GLOBAL_TEMPORARY_COLUMN = "GLOBAL TEMPORARY COLUMN"; //$NON-NLS-1$

    /**
     * The Constant METADATA_SYNONYM.
     */
    String METADATA_SYNONYM = "SYNONYM"; //$NON-NLS-1$

    /**
     * The Constant METADATA_ALIAS.
     */
    String METADATA_ALIAS = "ALIAS"; //$NON-NLS-1$

    /**
     * The Constant METADATA_VIEW.
     */
    String METADATA_VIEW = "VIEW"; //$NON-NLS-1$

    /**
     * The Constant METADATA_VIEW.
     */
    String METADATA_CALC_VIEW = "CALC VIEW"; //$NON-NLS-1$

    /**
     * The Constant METADATA_TABLE.
     */
    String METADATA_TABLE = "TABLE"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_DYNAMIC.
     */
    String KEYWORD_DYNAMIC = "DYNAMIC"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_EVENT.
     */
    String KEYWORD_EVENT = "EVENT"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_EXTERNAL.
     */
    String KEYWORD_EXTERNAL = "EXTERNAL"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_HYBRID.
     */
    String KEYWORD_HYBRID = "HYBRID"; //$NON-NLS-1$

    /**
     * The Constant KEYWORD_ICEBERG.
     */
    String KEYWORD_ICEBERG = "ICEBERG"; //$NON-NLS-1$

    /**
     * The Constant METADATA_TABLE_TYPES.
     */
    List<String> METADATA_TABLE_TYPES = Collections.unmodifiableList(Arrays.asList(METADATA_TABLE, METADATA_VIEW, METADATA_ALIAS,
            METADATA_SYNONYM, METADATA_GLOBAL_TEMPORARY, METADATA_LOCAL_TEMPORARY, METADATA_SYSTEM_TABLE, METADATA_BASE_TABLE));

    /**
     * The Constant METADATA_TABLE_STRUCTURES.
     */
    List<String> METADATA_TABLE_STRUCTURES = Collections.unmodifiableList(
            Arrays.asList(METADATA_TABLE, METADATA_GLOBAL_TEMPORARY, METADATA_LOCAL_TEMPORARY, METADATA_SYSTEM_TABLE, METADATA_BASE_TABLE));

    /**
     * The Constant KEYWORD_COLUMNSTORE.
     */
    String KEYWORD_COLUMNSTORE = "COLUMNSTORE";

    /**
     * The Constant KEYWORD_ROWSTORE.
     */
    String KEYWORD_ROWSTORE = "ROWSTORE";

    /**
     * The Constant KEYWORD_GLOBAL_TEMPORARY.
     */
    String KEYWORD_GLOBAL_TEMPORARY = "GLOBAL_TEMPORARY";

    /**
     * The Constant KEYWORD_GLOBAL_TEMPORARY_COLUMN.
     */
    String KEYWORD_GLOBAL_TEMPORARY_COLUMN = "GLOBAL_TEMPORARY_COLUMN";

    /** The Constant KEYWORD_LIKE. */
    String KEYWORD_LIKE = "LIKE";

    /** The Constant KEYWORD_NO. */
    String KEYWORD_NO = "NO";

    /** The Constant KEYWORD_DATA. */
    String KEYWORD_DATA = "DATA";
}
