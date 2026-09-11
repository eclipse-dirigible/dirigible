/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.form;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a task form's COMPLETE catch routes a failed {@code POST /services/inbox/tasks/*}
 * through the shared, safe {@code apiErrors.refusalMessageFor} instead of printing the raw
 * developer-facing {@code error.data.message} a 500 carries (issue #7296, a #7263 follow-up).
 *
 * #7151/#7062 made the shared apiErrors helper safe and wired it into the power form/dialog; #7152
 * reached the personal/partner line dialogs. The one path every process user task actually takes -
 * {@code __completeTask}'s own {@code .catch} - was left printing
 * {@code error && error.data && error.data.message}, a spelling the emission-coverage walk's string
 * checks could not see either (neither "(e && e.message)" nor "String(e.message" matches it), which
 * is why this generator-level unit test exists rather than relying on that walk alone.
 */
class TaskFormApiErrorTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: SalesOrder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: total, type: decimal }
            processes:
              - name: OrderApproval
                trigger: { onCreate: SalesOrder }
                steps:
                  - { name: confirm, kind: userTask, args: { assignee: manager, form: ConfirmOrder } }
                  - { name: end, kind: end }
            forms:
              - { name: ConfirmOrder, forEntity: SalesOrder, fields: [total], actions: [confirm] }
            """;

    private static String codeOf(String form) {
        IntentModel model = IntentParser.parse(YAML);
        Map<String, Map<String, Object>> forms = FormIntentGenerator.buildFormsForTest(model);
        return String.valueOf(forms.get(form)
                                   .get("code"));
    }

    @Test
    void completeTaskCatchRoutesThroughApiErrors() {
        String code = codeOf("ConfirmOrder");

        assertTrue(code.contains("App.services.apiErrors.refusalMessageFor(error, 'Submit failed.')"),
                "the COMPLETE catch must map the failure through the shared apiErrors helper: " + code);
        assertFalse(code.contains("error.data.message"),
                "the COMPLETE catch must never read the developer-facing error.data.message: " + code);
        assertFalse(code.contains("'Unknown error'"), "the raw 'Unknown error' fallback must be gone: " + code);
    }
}
