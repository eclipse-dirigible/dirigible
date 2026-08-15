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

import org.junit.jupiter.api.Test;

/**
 * Parse-time validation of a resolver-path user-task assignee ({@code assignee: { path:
 * employee.manager, fallback: manager }}). Every hop is checked here so a dangling segment fails
 * Generate rather than the running process.
 */
class AssigneePathIntentTest {

    private static final String YAML = """
            name: expenses
            entities:
              - name: Employee
                identity: email
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: email, type: string, unique: true }
                relations:
                  - { name: manager, kind: manyToOne, to: Employee }
                  - { name: expenses, kind: oneToMany, to: Expense }
              - name: Department
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Expense
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: employee, kind: manyToOne, to: Employee }
                  - { name: department, kind: manyToOne, to: Department }
            processes:
              - name: ExpenseApproval
                trigger: { onCreate: Expense }
                steps:
                  - name: approve
                    kind: userTask
                    args:
                      assignee: { path: employee.manager, fallback: manager }
                      next: done
                  - { name: done, kind: end }
            """;

    @Test
    void aWalkEndingAtAnIdentityBearingEntityParses() {
        assertEquals("expenses", IntentParser.parse(YAML)
                                             .getName());
    }

    @Test
    void aSingleHopToTheIdentityBearingEntityParses() {
        assertEquals("expenses", IntentParser.parse(YAML.replace("path: employee.manager", "path: employee"))
                                             .getName());
    }

    @Test
    void aDanglingSegmentFails() {
        assertTrue(issues(YAML.replace("path: employee.manager", "path: employee.supervisor")).contains(
                "[Employee] has no to-one relation [supervisor]"));
    }

    @Test
    void aCollectionSegmentIsNotAWalk() {
        assertTrue(
                issues(YAML.replace("path: employee.manager", "path: expenses")).contains("[Expense] has no to-one relation [expenses]"));
    }

    @Test
    void aTargetWithoutIdentityFails() {
        String issues = issues(YAML.replace("path: employee.manager", "path: department"));
        assertTrue(issues.contains("[Department] declares no identity"), issues);
    }

    @Test
    void aMissingFallbackFails() {
        String issues = issues(YAML.replace(", fallback: manager", ""));
        assertTrue(issues.contains("declares an assignee path but no fallback candidate group"), issues);
    }

    @Test
    void anUnknownAssigneeKeyFails() {
        String issues = issues(YAML.replace("fallback: manager", "fallback: manager, group: manager"));
        assertTrue(issues.contains("unknown assignee key(s) [group]"), issues);
    }

    @Test
    void aProcessWithoutATriggerHasNothingToWalkOff() {
        String issues = issues(YAML.replace("    trigger: { onCreate: Expense }\n", ""));
        assertTrue(issues.contains("the process has no trigger entity to walk it off"), issues);
    }

    /**
     * A projection carries the target's own properties but not its relations, so a cross-model hop can
     * only be the last one - the walk has nothing left to traverse from there.
     */
    @Test
    void aCrossModelHopMustBeTheLastOne() {
        String yaml = YAML.replace("name: expenses\n", "name: expenses\nuses:\n  - { model: hr }\n")
                          .replace("      - { name: employee, kind: manyToOne, to: Employee }",
                                  "      - { name: employee, kind: manyToOne, to: Employee, model: hr }");
        String issues = issues(yaml);
        assertTrue(issues.contains("walks on past a cross-model relation"), issues);
    }

    private static String issues(String yaml) {
        return assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml)).getMessage();
    }
}
