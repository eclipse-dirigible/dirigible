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

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

class IntentParserTest {

    private static final String HEAD = """
            name: lib
            entities:
              - name: Member
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string }
              - name: Loan
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: member, kind: manyToOne, to: Member }
            notifications:
              - name: loanUpdated
                event: { onUpdate: Loan }
                subject: "x"
            """;

    @Test
    void braceRecipientReportsACleanIssueInsteadOfA500() {
        // `to: {member.email}` is YAML flow-mapping (an object), not a string. The typed mapping used
        // to throw a raw Gson JsonSyntaxException (-> 500); it must now be a clean validation issue.
        String yaml = HEAD.stripTrailing() + "\n    to: {member.email}\n";
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("wrong type")),
                "a wrong-typed scalar should be reported as a validation issue, got: " + ex.getIssues());
    }

    @Test
    void bareOneHopRelationFieldRecipientParses() {
        // `to: member.email` (bare, no braces) is the supported one-hop relation.field recipient.
        String yaml = HEAD.stripTrailing() + "\n    to: member.email\n";
        IntentModel model = IntentParser.parse(yaml);
        assertEquals("member.email", model.getNotifications()
                                          .get(0)
                                          .getTo());
    }
}
