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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * A YAML syntax error is a validation issue carrying the line and column, never a raw parser
 * exception (dirigible #8185): the editor parses every half-typed bracket.
 */
class YamlSyntaxIntentTest {

    @Test
    void unclosedFlowMappingIsAnIssueWithLineAndColumn() {
        String yaml = """
                name: QA-HUNT
                entities:
                  - name: A
                    fields: [ {bad
                """;

        List<String> issues = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml)).getIssues();

        assertEquals(1, issues.size(), issues.toString());
        assertEquals("YAML syntax error at line 5, column 1: expected ',' or '}', but got <stream end>"
                + " (while parsing a flow mapping that started at line 4, column 15)", issues.get(0));
    }

    @Test
    void lineAndColumnAreOneBasedAndPointAtTheProblem() {
        String yaml = """
                name: Broken
                entities:
                  - name: A
                   bad: indentation
                """;

        String issue = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml)).getIssues()
                                                                                                    .get(0);

        assertTrue(issue.startsWith("YAML syntax error at line 4, column 4: "), issue);
    }

    @Test
    void tabIndentationIsAnIssueWithLineAndColumn() {
        String yaml = "name: Tabs\nentities:\n\t- name: A\n";

        String issue = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml)).getIssues()
                                                                                                    .get(0);

        assertTrue(issue.startsWith("YAML syntax error at line 3, column 1: "), issue);
    }

    @Test
    void wellFormedYamlStillParses() {
        assertEquals("Fine", IntentParser.parse("name: Fine\n")
                                         .getName());
    }
}
