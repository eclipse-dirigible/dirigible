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
 * Verifies that the non-completing {@code close} action the {@link FormIntentGenerator} appends to
 * a task form closes with a DISTINGUISHABLE outcome (issue #7149).
 *
 * The host shell announces every outcome an action page reports, and a plain
 * {@code DialogHub.closeWindow()} carries no outcome at all - so the user who opened a contributed
 * action, decided not to proceed and clicked the page's own Cancel button got a green
 * "&lt;label&gt; completed" toast plus a permanent notification-centre entry for work that was
 * explicitly abandoned. The close handler therefore closes through {@code cancelWindow()} (which
 * the form runtime maps to {@code status: 'cancelled'}), keeping {@code closeWindow()} only as the
 * fallback for a host runtime that predates it.
 */
class FormCancelActionTest {

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

    /**
     * The auto-appended Cancel button says the action was abandoned, so the host stays quiet.
     */
    @Test
    void theAppendedCloseActionCancelsRatherThanCompletes() {
        String code = codeOf("ConfirmOrder");

        assertTrue(code.contains("$scope.onCloseClicked = function () {"), "the close action should be wired: " + code);
        assertTrue(code.contains("__dialogs.cancelWindow ? __dialogs.cancelWindow() : __dialogs.closeWindow()"),
                "close should report a cancellation, falling back to a plain close on an older runtime: " + code);
        assertTrue(code.contains("$scope.onCloseClicked = function () { (__dialogs.cancelWindow ? __dialogs.cancelWindow()"
                + " : __dialogs.closeWindow()); window.close(); };"), "unexpected close handler: " + code);
        // Closing is NOT completing: the task stays open in the inbox.
        assertFalse(code.contains("onCloseClicked = function () { __completeTask("), "close must not complete the task: " + code);
    }

    /**
     * A real action still completes the task and still announces itself - only the abandonment is
     * silent.
     */
    @Test
    void aCompletingActionIsUnchanged() {
        String code = codeOf("ConfirmOrder");

        assertTrue(code.contains("$scope.onConfirmClicked = function () { __completeTask('confirm'); };"),
                "the declared action should still complete the task: " + code);
        assertFalse(code.contains("onConfirmClicked = function () { (__dialogs.cancelWindow"),
                "a completing action must not cancel: " + code);
    }
}
