/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.migrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import org.eclipse.dirigible.components.data.migrations.MigrationScript.Scope;
import org.junit.jupiter.api.Test;

class MigrationScriptTest {

    private static MigrationScript parse(String location, String content) throws ParseException {
        return MigrationScript.parse(location, content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void theFileNameCarriesProjectVersionAndDescription() throws ParseException {
        MigrationScript script = parse("/orders/migrations/V1.2__backfill_status.migration", "UPDATE ORDERS SET STATUS = 'OPEN';");

        assertThat(script.project()).isEqualTo("orders");
        assertThat(script.version()).isEqualTo("1.2");
        assertThat(script.description()).isEqualTo("backfill_status");
        assertThat(script.sql()).isEqualTo("UPDATE ORDERS SET STATUS = 'OPEN';");
    }

    @Test
    void withoutHeadersAMigrationAppliesToEachTenantAndIsNotIdempotent() throws ParseException {
        MigrationScript script = parse("/orders/001__backfill.migration", "-- recomputes the totals\nUPDATE ORDERS SET TOTAL = 0;");

        assertThat(script.scope()).isEqualTo(Scope.EACH);
        assertThat(script.idempotent()).isFalse();
    }

    @Test
    void theHeadersDeclareScopeAndIdempotence() throws ParseException {
        MigrationScript script = parse("/orders/001__backfill.migration", """
                -- Moves the legacy rows.
                --   tenant: system
                -- IDEMPOTENT: True

                UPDATE ORDERS SET TOTAL = 0;
                -- tenant: each (a comment after the first statement is SQL, not a header)
                """);

        assertThat(script.scope()).isEqualTo(Scope.SYSTEM);
        assertThat(script.idempotent()).isTrue();
    }

    @Test
    void anUnknownHeaderValueIsRejected() {
        assertThatThrownBy(
                () -> parse("/orders/001__backfill.migration", "-- tenant: some\nUPDATE ORDERS SET TOTAL = 0;")).isInstanceOf(
                        ParseException.class)
                                                                                                                .hasMessageContaining(
                                                                                                                        "[tenant: some]");
        assertThatThrownBy(() -> parse("/orders/001__backfill.migration",
                "-- idempotent: yes\nUPDATE ORDERS SET TOTAL = 0;")).isInstanceOf(ParseException.class)
                                                                    .hasMessageContaining("[idempotent: yes]");
    }

    @Test
    void aFileNameWithoutAVersionIsRejected() {
        assertThatThrownBy(() -> parse("/orders/backfill.migration", "UPDATE ORDERS SET TOTAL = 0;")).isInstanceOf(ParseException.class)
                                                                                                     .hasMessageContaining(
                                                                                                             "<version>__<description>.migration");
        assertThatThrownBy(() -> parse("/orders/1a__backfill.migration", "UPDATE ORDERS SET TOTAL = 0;")).isInstanceOf(
                ParseException.class);
    }

    @Test
    void aMigrationOutsideAProjectIsRejected() {
        assertThatThrownBy(() -> parse("/001__backfill.migration", "UPDATE ORDERS SET TOTAL = 0;")).isInstanceOf(ParseException.class)
                                                                                                   .hasMessageContaining(
                                                                                                           "inside a project");
    }

    @Test
    void aMigrationWithoutAStatementIsRejected() {
        assertThatThrownBy(
                () -> parse("/orders/001__backfill.migration", "-- tenant: each\n\n-- nothing yet\n")).isInstanceOf(ParseException.class)
                                                                                                      .hasMessageContaining(
                                                                                                              "no SQL statement");
    }

    @Test
    void convertedLineEndingsAreNotAnEdit() throws ParseException {
        MigrationScript unix = parse("/orders/001__backfill.migration", "UPDATE ORDERS\nSET TOTAL = 0;\n");
        MigrationScript windows = parse("/orders/001__backfill.migration", "UPDATE ORDERS\r\nSET TOTAL = 0;\r\n");
        MigrationScript edited = parse("/orders/001__backfill.migration", "UPDATE ORDERS\nSET TOTAL = 1;\n");

        assertThat(windows.checksum()).isEqualTo(unix.checksum());
        assertThat(edited.checksum()).isNotEqualTo(unix.checksum());
    }

    @Test
    void versionsCompareNumericallySegmentBySegment() {
        assertThat(MigrationScript.compareVersions("2", "10")).isNegative();
        assertThat(MigrationScript.compareVersions("1.2", "1.10")).isNegative();
        assertThat(MigrationScript.compareVersions("001", "1")).isZero();
        assertThat(MigrationScript.compareVersions("1", "1.0")).isNegative();
        assertThat(MigrationScript.compareVersions("20261005120000", "20261005115959")).isPositive();
        assertThat(MigrationScript.compareVersions("99999999999999999999", "100000000000000000000")).isNegative();
    }
}
