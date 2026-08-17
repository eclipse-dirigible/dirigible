/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Declared step data - dirigible #6762: process-level {@code vars:} named by the steps'
 * {@code produces:}/{@code uses:} lists, with {@code clearAfter} removing a value once its step
 * completes. An undeclared name in either list is a parse error - step data is always written down,
 * never invented ad hoc in a delegate.
 */
class ProcessVarsIntentTest {

    private static final String YAML =
            """
                    name: provisioning
                    entities:
                      - name: TenantApplication
                        fields:
                          - { name: id, type: integer, primaryKey: true, generated: true }
                          - { name: failureMessage, type: string }
                    processes:
                      - name: TenantProvisioning
                        trigger: { onCreate: TenantApplication }
                        vars:
                          - { name: dbPassword, clearAfter: provisionApp }
                        steps:
                          - { name: createSchema, kind: serviceTask, args: { delegate: custom.SchemaProvisioner, produces: [dbPassword], next: provisionApp } }
                          - { name: provisionApp, kind: serviceTask, args: { delegate: custom.AppProvisioner, uses: [dbPassword], next: done } }
                          - { name: done, kind: end }
                    """;

    @Test
    void theShowcaseParses() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML));
    }

    @Test
    void anUndeclaredProducedVarIsRejected() {
        String issue =
                assertIssue(YAML.replace("produces: [dbPassword]", "produces: [dbPasword]"), "produces names undeclared var [dbPasword]");
        assertTrue(issue.contains("step [createSchema]"), "the message must locate the step: " + issue);
        assertTrue(issue.contains("declare it under the process `vars:`"), "the message must say where to declare it: " + issue);
    }

    @Test
    void anUndeclaredUsedVarIsRejected() {
        assertIssue(YAML.replace("uses: [dbPassword]", "uses: [adminToken]"), "uses names undeclared var [adminToken]");
    }

    @Test
    void producesWithNoVarsBlockIsRejected() {
        String yaml = YAML.replace("    vars:\n      - { name: dbPassword, clearAfter: provisionApp }\n", "");
        assertIssue(yaml, "produces names undeclared var [dbPassword]");
        assertIssue(yaml, "uses names undeclared var [dbPassword]");
    }

    @Test
    void aDuplicateVarIsRejected() {
        assertIssue(
                YAML.replace("- { name: dbPassword, clearAfter: provisionApp }",
                        "- { name: dbPassword, clearAfter: provisionApp }\n      - { name: dbPassword }"),
                "declares var [dbPassword] twice");
    }

    @Test
    void aVarWithNoNameIsRejected() {
        assertIssue(YAML.replace("name: dbPassword, clearAfter: provisionApp", "clearAfter: provisionApp"), "declares a var with no name");
    }

    /** The name becomes a process variable, cleared through an expression - it must stay plain. */
    @Test
    void aVarNameThatIsNoIdentifierIsRejected() {
        assertIssue(YAML.replace("name: dbPassword,", "name: \"db password\","),
                "var [db password] must be a plain identifier (letters, digits, _)");
    }

    @Test
    void clearAfterAnUnknownStepIsRejected() {
        assertIssue(YAML.replace("clearAfter: provisionApp", "clearAfter: provisionapp"),
                "var [dbPassword] clearAfter references unknown step [provisionapp]");
    }

    /** A gateway / end step emits no listener-bearing element - the clear would silently never fire. */
    @Test
    void clearAfterANonTaskStepIsRejected() {
        assertIssue(YAML.replace("clearAfter: provisionApp", "clearAfter: done"),
                "var [dbPassword] clearAfter [done] must name a serviceTask or userTask");
    }

    @Test
    void aNonListProducesIsRejected() {
        assertIssue(YAML.replace("produces: [dbPassword]", "produces: dbPassword"), "produces must be a list of declared var names");
    }

    /** The typed {@code vars:} rows ride the reflected unknown-key walk like every other model node. */
    @Test
    void anUnknownKeyInAVarIsRejected() {
        String issue = assertIssue(YAML.replace("clearAfter: provisionApp }", "clearAfte: provisionApp }"), "unknown key [clearAfte]");
        assertTrue(issue.contains("did you mean [clearAfter]?"), "the message must name the nearest key: " + issue);
    }

    private static String assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        String issue = ex.getIssues()
                         .stream()
                         .filter(i -> i.contains(expected))
                         .findFirst()
                         .orElse(null);
        assertEquals(true, issue != null, "expected an issue containing [" + expected + "] but got " + ex.getIssues());
        return issue;
    }
}
