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
 * A process can take back a field it wrote - dirigible #7386.
 *
 * <p>
 * A {@code setField} value is refused when it is blank, so the DSL could write any string into a
 * string field except an empty one and a process had no declarative erasure: the generated error
 * route writes the failure text, and an instance re-driven to success ended in a success status
 * still carrying the previous failure's explanation, because nothing could take the column back.
 * The workaround was a {@code delegate:} step whose whole body was one
 * {@code updateProperty(id, field, null)} - Java for something the model otherwise expresses
 * completely, written once per process sharing the pattern.
 *
 * <p>
 * {@code clearField:} is that erasure, and it names the field and nothing else: a blank {@code
 * value} stays refused, because it reads as "I forgot to fill this in" rather than as an intention.
 */
class ClearFieldIntentTest {

    private static final String YAML = """
            name: provisioning
            entities:
              - name: ProvisioningStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: TenantApplication
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: errorMessage, type: string }
                  - { name: attempts, type: integer }
                relations:
                  - { name: Status, kind: manyToOne, to: ProvisioningStatus, function: EntityStatus, init: 1 }
            processes:
              - name: TenantProvisioning
                trigger: { onCreate: TenantApplication }
                steps:
                  - { name: resetError, kind: serviceTask, args: { clearField: errorMessage, next: provision } }
                  - { name: provision, kind: serviceTask, args: { delegate: custom.Provisioner, onError: recordFailure, next: done } }
                  - { name: recordFailure, kind: serviceTask, args: { setField: errorMessage, value: "{error}", next: done } }
                  - { name: done, kind: end }
            """;

    @Test
    void aClearFieldStepParses() {
        assertDoesNotThrow(() -> IntentParser.parse(YAML));
    }

    /** The refusal the erasure exists to work around stays exactly as it was. */
    @Test
    void aBlankSetFieldValueIsStillRefused() {
        assertIssue(YAML.replace("clearField: errorMessage", "setField: errorMessage, value: \"\""),
                "setField [errorMessage] must declare a value");
    }

    @Test
    void aClearFieldWithAValueIsRefused() {
        assertIssue(YAML.replace("clearField: errorMessage", "clearField: errorMessage, value: x"),
                "clearField [errorMessage] takes no value");
    }

    @Test
    void aClearFieldNamingAnUnknownFieldIsRefused() {
        assertIssue(YAML.replace("clearField: errorMessage", "clearField: errorMesage"), "clearField [errorMesage] is not a field of");
    }

    /** Only a literal write has an erasure: a number or a date says nothing about what empty means. */
    @Test
    void aClearFieldOnANonStringFieldIsRefused() {
        assertIssue(YAML.replace("clearField: errorMessage", "clearField: attempts"), "clearField [attempts] must be a string/text field");
    }

    @Test
    void aClearFieldOnAUserTaskIsRefused() {
        assertIssue(YAML.replace("name: resetError, kind: serviceTask", "name: resetError, kind: userTask"),
                "uses clearField but is not a serviceTask");
    }

    /** A step writes one field, one way - two of them would race for the same generated setter. */
    @Test
    void aClearFieldCombinedWithASetFieldIsRefused() {
        assertIssue(YAML.replace("clearField: errorMessage", "clearField: errorMessage, setField: errorMessage, value: x"),
                "clearField cannot be combined with setField/setRelationField");
    }

    @Test
    void aClearFieldCombinedWithADelegateIsRefused() {
        assertIssue(YAML.replace("clearField: errorMessage,", "clearField: errorMessage, delegate: custom.Resetter,"),
                "delegate cannot be combined with setField/clearField/setRelationField/call");
    }

    /** A typo in the key itself is a typo, not a new vocabulary. */
    @Test
    void aMisspelledClearFieldNamesTheRealOne() {
        String issue =
                assertIssue(YAML.replace("clearField: errorMessage", "clearFeild: errorMessage"), "declares unknown arg [clearFeild]");
        assertTrue(issue.contains("did you mean [clearField]?"), "the message must name the nearest arg: " + issue);
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
