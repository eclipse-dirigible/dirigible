/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The {@code setters} glue collection's {@code {error}} handling - dirigible #6762: a
 * {@code setField} whose value is the whole-value {@code {error}} token carries
 * {@code errorMessage: true} (the template then reads the failure-message process variable instead
 * of assigning a literal), and every other setter's descriptor stays exactly as before, so
 * already-written {@code .glue} files render unchanged.
 */
class GlueSettersTest {

    private static final String YAML =
            """
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
                          - { name: failureMessage, type: string }
                          - { name: state, type: string }
                        relations:
                          - { name: Status, kind: manyToOne, to: ProvisioningStatus, function: EntityStatus, init: 1 }
                    processes:
                      - name: TenantProvisioning
                        trigger: { onCreate: TenantApplication }
                        steps:
                          - { name: provisionApp, kind: serviceTask, args: { delegate: custom.AppProvisioner, onError: recordFailure, next: markDone } }
                          - { name: markDone, kind: serviceTask, args: { setField: state, value: DONE, next: end } }
                          - { name: recordFailure, kind: serviceTask, args: { setField: failureMessage, value: "{error}", next: markFailed } }
                          - { name: markFailed, kind: serviceTask, args: { setRelationField: Status, value: 3, next: end } }
                    """;

    @Test
    void anErrorValueSetterCarriesTheErrorMessageFlag() {
        Map<String, Object> setter = setter("TenantProvisioningRecordFailure");

        assertEquals("true", setter.get("errorMessage"), "the {error} setter must be flagged: " + setter);
        assertEquals("{error}", setter.get("value"));
        assertEquals("FailureMessage", setter.get("field"));
    }

    /** A literal setter's descriptor is untouched, so pre-existing .glue files render unchanged. */
    @Test
    void aLiteralSetterCarriesNoErrorMessageKey() {
        Map<String, Object> setter = setter("TenantProvisioningMarkDone");

        assertFalse(setter.containsKey("errorMessage"), "a literal setter must stay exactly as before: " + setter);
        assertEquals("DONE", setter.get("value"));
    }

    @Test
    void aRelationSetterCarriesNoErrorMessageKey() {
        Map<String, Object> setter = setter("TenantProvisioningMarkFailed");

        assertFalse(setter.containsKey("errorMessage"), "a relation setter assigns a seed id, never a message: " + setter);
        assertEquals("true", setter.get("relation"));
    }

    private static Map<String, Object> setter(String className) {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> setters = GlueIntentGenerator.buildSettersForTest(model);
        return setters.stream()
                      .filter(entry -> className.equals(entry.get("className")))
                      .findFirst()
                      .orElseThrow(() -> new AssertionError("expected a setter [" + className + "] but got " + setters));
    }
}
