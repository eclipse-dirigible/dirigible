/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.csvim.processor;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.junit.jupiter.api.Test;

/**
 * The identity statements a seed import emits must carry the schema the platform itself generates.
 * A tenant's schema is its id uppercased and that id is a random UUID, so it has hyphens and, two
 * times out of three, a leading digit - a name no dialect accepts unquoted. These are the two sinks
 * CI never reaches on every engine: MSSQL is not a CI leg at all and MySQL/MariaDB has none either.
 */
class CsvimProcessorIdentitySqlTest {

    /** A tenant schema in the shape {@code DefaultDataSourceProvisioning} derives from a tenant id. */
    private static final String TENANT_SCHEMA = "9E4C1B7A-2F0D-4A55-8C31-6B9D0E5F1A24";

    private static final String TABLE = "ORDERS";

    private static final String COLUMN = "ORDER_ID";

    private static final String STANDARD_QUOTE = "\"";

    @Test
    void h2RestartsTheColumnOnTheQuotedTenantSchema() {
        String expected = "ALTER TABLE \"" + TENANT_SCHEMA + "\".\"ORDERS\" ALTER COLUMN \"ORDER_ID\" RESTART WITH 7";

        assertThat(CsvimProcessor.identityRestartSql(DatabaseSystem.H2, STANDARD_QUOTE, TENANT_SCHEMA, TABLE, COLUMN, 7)).isEqualTo(
                expected);
    }

    @Test
    void postgresPassesTheQuotedNameInsideTheSequenceLookupLiteral() {
        String expected = "SELECT setval(pg_get_serial_sequence('\"" + TENANT_SCHEMA + "\".\"ORDERS\"', 'ORDER_ID'), 7, false)";

        assertThat(CsvimProcessor.identityRestartSql(DatabaseSystem.POSTGRESQL, STANDARD_QUOTE, TENANT_SCHEMA, TABLE, COLUMN, 7)).isEqualTo(
                expected);
    }

    @Test
    void mssqlBracketQuotesTheNameItReseeds() {
        // RESEED stores the current identity, so the next assigned id is the one asked for.
        assertThat(CsvimProcessor.identityRestartSql(DatabaseSystem.MSSQL, STANDARD_QUOTE, TENANT_SCHEMA, TABLE, COLUMN, 7)).isEqualTo(
                "DBCC CHECKIDENT('[" + TENANT_SCHEMA + "].[ORDERS]', RESEED, 6)");
    }

    @Test
    void mysqlAndMariaDbBackquoteTheName() {
        String expected = "ALTER TABLE `" + TENANT_SCHEMA + "`.`ORDERS` AUTO_INCREMENT = 7";

        assertThat(CsvimProcessor.identityRestartSql(DatabaseSystem.MYSQL, STANDARD_QUOTE, TENANT_SCHEMA, TABLE, COLUMN, 7)).isEqualTo(
                expected);
        assertThat(CsvimProcessor.identityRestartSql(DatabaseSystem.MARIADB, STANDARD_QUOTE, TENANT_SCHEMA, TABLE, COLUMN, 7)).isEqualTo(
                expected);
    }

    @Test
    void aDialectWithNoSupportedRestartFormIsTheOnlyRemainingSkip() {
        assertThat(CsvimProcessor.identityRestartSql(DatabaseSystem.HANA, STANDARD_QUOTE, TENANT_SCHEMA, TABLE, COLUMN, 7)).isNull();
    }

    @Test
    void aBareTableIsEmittedWhenNoSchemaIsKnown() {
        assertThat(CsvimProcessor.identityRestartSql(DatabaseSystem.H2, STANDARD_QUOTE, null, TABLE, COLUMN, 7)).isEqualTo(
                "ALTER TABLE \"ORDERS\" ALTER COLUMN \"ORDER_ID\" RESTART WITH 7");
    }

    @Test
    void anEmbeddedDelimiterIsDoubledRatherThanClosingTheQuote() {
        assertThat(CsvimProcessor.identityRestartSql(DatabaseSystem.MSSQL, STANDARD_QUOTE, "SC]HEMA", "OR]DERS", COLUMN, 7)).isEqualTo(
                "DBCC CHECKIDENT('[SC]]HEMA].[OR]]DERS]', RESEED, 6)");
        assertThat(
                CsvimProcessor.identityRestartSql(DatabaseSystem.POSTGRESQL, STANDARD_QUOTE, "SC\"HEMA", "OR\"DERS", "O'ID", 7)).isEqualTo(
                        "SELECT setval(pg_get_serial_sequence('\"SC\"\"HEMA\".\"OR\"\"DERS\"', 'O''ID'), 7, false)");
        assertThat(CsvimProcessor.identityRestartSql(DatabaseSystem.MYSQL, STANDARD_QUOTE, "SC`HEMA", TABLE, COLUMN, 7)).isEqualTo(
                "ALTER TABLE `SC``HEMA`.`ORDERS` AUTO_INCREMENT = 7");
    }

    @Test
    void aLoggedIdentifierCarriesNoLineTerminatorAndNoTab() {
        // CWE-117: these names are logged and are user-influenced, so nothing that can start a new log
        // line or fake a column may survive. \R covers the Unicode terminators an ASCII class misses.
        assertThat(CsvimProcessor.sanitize("OR\r\nDERS")).isEqualTo("OR_DERS");
        assertThat(CsvimProcessor.sanitize("OR\u0085DE\u2028RS\u2029")).isEqualTo("OR_DE_RS_");
        assertThat(CsvimProcessor.sanitize("OR\tDERS")).isEqualTo("OR_DERS");
        assertThat(CsvimProcessor.sanitize(TENANT_SCHEMA)).isEqualTo(TENANT_SCHEMA);
        assertThat(CsvimProcessor.sanitize(null)).isNull();
    }

    @Test
    void theMssqlIdentityInsertToggleAddressesTheTenantSchemaToo() {
        assertThat(CsvimProcessor.identityInsertSql(TENANT_SCHEMA, TABLE, true)).isEqualTo(
                "SET IDENTITY_INSERT [" + TENANT_SCHEMA + "].[ORDERS] ON");
        assertThat(CsvimProcessor.identityInsertSql(TENANT_SCHEMA, TABLE, false)).isEqualTo(
                "SET IDENTITY_INSERT [" + TENANT_SCHEMA + "].[ORDERS] OFF");
    }
}
