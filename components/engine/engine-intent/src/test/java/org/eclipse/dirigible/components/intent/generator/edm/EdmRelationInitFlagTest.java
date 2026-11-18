/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.edm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * A to-one relation's {@code init: { flag: <field> }} (#8683): the FK defaults to the target row
 * holding a fleet-wide {@code single:} flag, so it carries {@code relationshipInitFlag} and no
 * column DEFAULT.
 */
class EdmRelationInitFlagTest {

    private static final String YAML = """
            name: banking
            entities:
              - name: Company
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 50 }
                  - { name: base, type: boolean, single: true }
                  - { name: active, type: boolean }
              - name: BankAccount
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: base, type: boolean, single: { per: Company } }
                relations:
                  - { name: Company, kind: manyToOne, to: Company }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Company, kind: manyToOne, to: Company, required: true, init: { flag: base } }
                  - { name: Account, kind: manyToOne, to: BankAccount }
            """;

    @Test
    void theForeignKeyNamesTheFlagAndCarriesNoDefault() {
        Map<String, Object> company = property(YAML, "Invoice", "Company");
        assertEquals("Base", company.get("relationshipInitFlag"));
        assertFalse(company.containsKey("dataDefaultValue"), "a flag default is no column DEFAULT: " + company);
    }

    @Test
    void theFlagMayBeSpelledAsTheProperty() {
        assertEquals("Base",
                property(YAML.replace("init: { flag: base }", "init: { flag: Base }"), "Invoice", "Company").get("relationshipInitFlag"));
    }

    @Test
    void aScalarInitIsUnchanged() {
        Map<String, Object> company = property(YAML.replace("init: { flag: base }", "init: 1"), "Invoice", "Company");
        assertEquals("1", company.get("dataDefaultValue"));
        assertFalse(company.containsKey("relationshipInitFlag"));
    }

    @Test
    void refusesAFieldThatIsNotASingleBoolean() {
        assertIssue(YAML.replace("init: { flag: base }", "init: { flag: active }"),
                "entity [Invoice] relation [Company] init flag [active] names [Company.active], which is not a boolean with single:");
    }

    @Test
    void refusesAnUnknownField() {
        assertIssue(YAML.replace("init: { flag: base }", "init: { flag: main }"),
                "entity [Invoice] relation [Company] init flag [main] is not a field of [Company]");
    }

    @Test
    void refusesAFlagScopedPerRelation() {
        assertIssue(
                YAML.replace("{ name: Account, kind: manyToOne, to: BankAccount }",
                        "{ name: Account, kind: manyToOne, to: BankAccount, init: { flag: base } }"),
                "entity [Invoice] relation [Account] init flag [base] names [BankAccount.base], whose single: is scoped per [Company]");
    }

    @Test
    void refusesAMappingWithoutAFlag() {
        assertIssue(YAML.replace("init: { flag: base }", "init: { flag: '' }"),
                "entity [Invoice] relation [Company] init must name the target's single: boolean");
    }

    @Test
    void refusesAnUnknownKeyInsideInit() {
        assertIssue(YAML.replace("init: { flag: base }", "init: { holder: base }"), "unknown key [holder]");
    }

    @Test
    void initFlagIsNotAKeyOfItsOwn() {
        assertIssue(YAML.replace("init: { flag: base }", "initFlag: base"), "unknown key [initFlag]");
    }

    @Test
    void refusesAStatusRelation() {
        assertIssue(YAML.replace("init: { flag: base }", "function: EntityStatus, init: { flag: base }"),
                "entity [Invoice] relation [Company] init flag [base] cannot default a function: EntityStatus relation");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains(expected)),
                "expected an issue containing [" + expected + "] but got " + ex.getIssues());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(String yaml, String entityName, String propertyName) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "banking");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        Map<String, Object> entity = entities.stream()
                                             .filter(candidate -> entityName.equals(candidate.get("name")))
                                             .findFirst()
                                             .orElseThrow(() -> new AssertionError("no entity [" + entityName + "]"));
        return ((List<Map<String, Object>>) entity.get("properties")).stream()
                                                                     .filter(candidate -> propertyName.equals(candidate.get("name")))
                                                                     .findFirst()
                                                                     .orElseThrow(() -> new AssertionError(
                                                                             "no property [" + propertyName + "]"));
    }
}
