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
 * {@code single:} on a boolean (dirigible #8101) - the default / base flag at most one row holds,
 * fleet-wide or per a to-one scope - reaches the repository template as one descriptor per flag,
 * and the parser refuses a flag it could not keep single.
 */
class EdmSingleFlagTest {

    private static final String YAML = """
            name: banking
            entities:
              - name: Company
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 50 }
                  - { name: base, type: boolean, single: true }
                relations:
                  - { name: accounts, kind: oneToMany, to: BankAccount }
              - name: BankAccount
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: iban, type: string, length: 34 }
                  - { name: base, type: boolean, single: { per: Company } }
                  - { name: preferred, type: boolean, single: { per: Company, onConflict: refuse } }
                relations:
                  - { name: Company, kind: manyToOne, to: Company }
            """;

    @Test
    void flagDescriptorsCarryPropertyScopeAndRefusal() {
        assertEquals(List.of(Map.of("property", "Base")), entity(YAML, "Company").get("singleFlags"));
        assertEquals(
                List.of(Map.of("property", "Base", "per", "Company"),
                        Map.of("property", "Preferred", "per", "Company", "refuse", "true", "message",
                                "Another Bank Account is already Preferred for this Company")),
                entity(YAML, "BankAccount").get("singleFlags"));
    }

    @Test
    void entityWithoutFlagsHasNoFlagVariable() {
        assertFalse(entity(YAML.replace(", single: true", ""), "Company").containsKey("singleFlags"));
    }

    @Test
    void singleFalseIsNoFlag() {
        assertFalse(entity(YAML.replace("single: true", "single: false"), "Company").containsKey("singleFlags"));
    }

    @Test
    void refusesANonBooleanField() {
        assertIssue(YAML.replace("{ name: name, type: string, length: 50 }", "{ name: name, type: string, length: 50, single: true }"),
                "entity [Company] field [name] single needs a boolean field, not [string]");
    }

    @Test
    void refusesAScopeThatIsNotAToOne() {
        assertIssue(YAML.replace("single: true", "single: { per: accounts }"),
                "entity [Company] field [base] single per [accounts] is not a manyToOne/oneToOne relation of [Company]");
    }

    @Test
    void refusesAnUnknownConflictPolicy() {
        assertIssue(YAML.replace("onConflict: refuse", "onConflict: ignore"),
                "entity [BankAccount] field [preferred] single onConflict [ignore] is not one of: demote, refuse");
    }

    @Test
    void refusesAScalarThatIsNotABoolean() {
        assertIssue(YAML.replace("single: true", "single: Company"), "single [Company] is neither true/false nor a mapping");
    }

    @Test
    void refusesAnUnknownKeyInsideSingle() {
        assertIssue(YAML.replace("single: { per: Company }", "single: { scope: Company }"), "unknown key [scope]");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains(expected)),
                "expected an issue containing [" + expected + "] but got " + ex.getIssues());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(String yaml, String name) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "banking");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        return entities.stream()
                       .filter(entity -> name.equals(entity.get("name")))
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no entity [" + name + "]"));
    }
}
