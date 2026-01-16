/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mssql;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.view.CreateViewBuilder;

/**
 * The MSSQL Create View Builder.
 */
public class MSSQLCreateViewBuilder extends CreateViewBuilder {

    /** The values. */
    private String values = null;

    /**
     * Instantiates a new MSSQL create view builder.
     *
     * @param dialect the dialect
     * @param view the view
     */
    public MSSQLCreateViewBuilder(ISqlDialect dialect, String view) {
        super(dialect, view);
    }

    /**
     * Column.
     *
     * @param name the name
     * @return the mssql create view builder
     */
    @Override
    public MSSQLCreateViewBuilder column(String name) {
        super.getColumns().add(name);
        return this;
    }

    /**
     * As select.
     *
     * @param select the select
     * @return the mssql create view builder
     */
    @Override
    public MSSQLCreateViewBuilder asSelect(String select) {

        if (this.values != null) {
            throw new IllegalStateException("Create VIEW can use either AS SELECT or AS VALUES, but not both.");
        }
        setSelect(select);
        return this;
    }

    /**
     * As values.
     *
     * @param values the values
     * @return the MSSQL create view builder
     */
    public MSSQLCreateViewBuilder asValues(String values) {
        if (getSelect() != null) {
            throw new IllegalStateException("Create VIEW can use either AS SELECT or AS VALUES, but not both.");
        }
        this.values = values;
        return this;
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {

        StringBuilder sql = new StringBuilder();

        // CREATE
        generateCreate(sql);

        // VIEW
        generateView(sql);

        // COLUMNS
        generateColumns(sql);

        // SELECT or VALUES
        if (getSelect() != null) {
            generateAsSelect(sql);
        } else if (this.values != null) {
            generateAsValues(sql);
        } else {
            throw new IllegalStateException("Create VIEW must use either AS SELECT or AS VALUES.");
        }

        return sql.toString();
    }

    /**
     * Generate as values.
     *
     * @param sql the sql
     */
    protected void generateAsValues(StringBuilder sql) {
        sql.append(SPACE)
           .append(KEYWORD_AS)
           .append(SPACE);

        String[] differentValues = this.values.split(",");
        for (int idx = 0; idx < differentValues.length; idx++) {
            sql.append(KEYWORD_SELECT)
               .append(SPACE)
               .append(differentValues[idx])
               .append(SPACE);
            if ((idx + 1) != differentValues.length) {
                sql.append("UNION ALL")
                   .append(SPACE);
            }
        }
    }

}
