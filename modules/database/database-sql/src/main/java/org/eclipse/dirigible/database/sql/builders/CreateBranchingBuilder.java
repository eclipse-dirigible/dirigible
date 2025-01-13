/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.SqlException;
import org.eclipse.dirigible.database.sql.builders.schema.CreateSchemaBuilder;
import org.eclipse.dirigible.database.sql.builders.sequence.CreateSequenceBuilder;
import org.eclipse.dirigible.database.sql.builders.synonym.CreateSynonymBuilder;
import org.eclipse.dirigible.database.sql.builders.table.CreateTableBuilder;
import org.eclipse.dirigible.database.sql.builders.table.CreateTemporaryTableBuilder;
import org.eclipse.dirigible.database.sql.builders.tableType.CreateTableTypeBuilder;
import org.eclipse.dirigible.database.sql.builders.user.CreateUserBuilder;
import org.eclipse.dirigible.database.sql.builders.view.CreateViewBuilder;

/**
 * The Create Branching Builder.
 */
public class CreateBranchingBuilder extends AbstractSqlBuilder {

    /**
     * Instantiates a new creates the branching builder.
     *
     * @param dialect the dialect
     */
    public CreateBranchingBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * Table branch.
     *
     * @param table the table
     * @return the creates the table builder
     */
    public CreateTableBuilder table(String table) {
        return new CreateTableBuilder(getDialect(), table);
    }

    /**
     * Table.
     *
     * @param table the table
     * @param tableType the table type
     * @return the creates the table builder
     */
    public CreateTableBuilder table(String table, String tableType) {
        return new CreateTableBuilder(getDialect(), table);
    }

    /**
     * Temporary table branch.
     *
     * @param table the table
     * @return the creates the table builder
     */
    public CreateTemporaryTableBuilder temporaryTable(String table) {
        return new CreateTemporaryTableBuilder(getDialect(), table);
    }

    /**
     * View branch.
     *
     * @param view the view
     * @return the creates the view builder
     */
    public CreateViewBuilder view(String view) {
        return new CreateViewBuilder(getDialect(), view);
    }

    /**
     * Sequence branch.
     *
     * @param sequence the sequence
     * @return the creates the sequence builder
     */
    public CreateSequenceBuilder sequence(String sequence) {
        return new CreateSequenceBuilder(getDialect(), sequence);
    }

    /**
     * Synonym branch.
     *
     * @param synonym the synonym
     * @return the creates the synonym builder
     */
    public CreateSynonymBuilder synonym(String synonym) {
        return new CreateSynonymBuilder(getDialect(), synonym);
    }

    /**
     * Public synonym branch.
     *
     * @param synonym the synonym
     * @return the creates the synonym builder
     */
    public CreateSynonymBuilder publicSynonym(String synonym) {
        return synonym(synonym);
    }

    /**
     * Schema branch.
     *
     * @param schema the schema
     * @return the creates the schema builder
     */
    public CreateSchemaBuilder schema(String schema) {
        return new CreateSchemaBuilder(getDialect(), schema);
    }

    /**
     * User.
     *
     * @param userId the user id
     * @param password the password
     * @return the creates the user builder
     */
    public CreateUserBuilder user(String userId, String password) {
        return new CreateUserBuilder(getDialect(), userId, password);
    }

    /**
     * Table Type branch.
     *
     * @param tableType the tableType
     * @return the creates the table type builder
     */
    public CreateTableTypeBuilder tableType(String tableType) {
        return new CreateTableTypeBuilder(getDialect(), tableType);
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        throw new SqlException("Invalid method invocation of generate() for Create Branching Builder");
    }

}
