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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.PermissionSupport;
import org.eclipse.dirigible.components.intent.model.ActionIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * An action that calls the application's own endpoint (issue #8090): the request, its dialog, the
 * advisory from-status guard and the permission token that decides who is offered the button.
 */
class CallActionIntentTest {

    static final String YAML = """
            name: helpdesk
            entities:
              - name: TicketStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Ticket
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: subject, type: string }
                relations:
                  - { name: Status, kind: manyToOne, to: TicketStatus, function: EntityStatus, init: 1 }
              - name: Note
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: text, type: string }
            actions:
              - name: EscalateTicket
                forEntity: Ticket
                label: Escalate
                icon: arrow-up
                fromStatus: [OPEN, ON_HOLD]
                can: Ticket:escalate
                prompt:
                  - { name: level, type: integer, label: Level, required: true }
                  - { name: note,  type: text,    label: Note }
                call:
                  method: POST
                  path: /services/java/helpdesk/custom/api/TicketApi/tickets/{id}/escalations
                  body: { kind: MANUAL, level: "{level}", note: "{note}", reason: "escalated: {note}" }
                success: Escalation requested
            permissions:
              - role: SupportLead
                can: [Ticket:read, Ticket:escalate]
            seeds:
              - name: ticket-statuses
                entity: TicketStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: ON_HOLD }
                  - { id: 3, name: CLOSED }
            """;

    @Test
    void aCallActionParses() {
        ActionIntent action = IntentParser.parse(YAML)
                                          .getActions()
                                          .get(0);

        assertEquals("POST", action.getCall()
                                   .getMethod());
        assertEquals("/services/java/helpdesk/custom/api/TicketApi/tickets/{id}/escalations", action.getCall()
                                                                                                    .getPath());
        assertEquals(Map.of("kind", "MANUAL", "level", "{level}", "note", "{note}", "reason", "escalated: {note}"), action.getCall()
                                                                                                                          .getBody());
        assertEquals(List.of("level", "note"), action.getPrompt()
                                                     .stream()
                                                     .map(p -> p.getName())
                                                     .toList());
        assertEquals("Ticket:escalate", action.getCan());
        assertEquals("Escalation requested", action.getSuccess());
    }

    @Test
    void theMethodDefaultsToPost() {
        ActionIntent action = IntentParser.parse(YAML.replace("      method: POST\n", ""))
                                          .getActions()
                                          .get(0);

        assertEquals("POST", action.getCall()
                                   .getMethod());
    }

    @Test
    void anUnknownMethodIsRefused() {
        assertIssue(YAML.replace("method: POST", "method: PATCH"), "call method [PATCH] is not one of [DELETE, POST, PUT]");
    }

    @Test
    void callAndPageAreExclusive() {
        assertIssue(YAML.replace("    success: Escalation requested", "    success: Escalation requested\n    page: /services/web/x.html"),
                "declares both page and call");
    }

    @Test
    void anUnknownPathPlaceholderIsRefused() {
        assertIssue(YAML.replace("tickets/{id}/escalations", "tickets/{ticketId}/escalations"),
                "call path names [{ticketId}] - {id} (the clicked record's key) is the only placeholder");
    }

    @Test
    void aPathOutsideServicesIsRefused() {
        assertIssue(YAML.replace("path: /services/java/helpdesk", "path: https://evil.example/java/helpdesk"),
                "must be an absolute same-origin path under /services/");
    }

    @Test
    void aBodyPlaceholderMustBeAPrompt() {
        assertIssue(YAML.replace("kind: MANUAL", "kind: \"{kind}\""), "call body names [{kind}], which is not a prompt input");
    }

    @Test
    void aPathIdNeedsEntityScope() {
        assertIssue(YAML.replace("    fromStatus: [OPEN, ON_HOLD]\n", "    scope: page\n"),
                "call path names {id} but the action has scope [page]");
    }

    @Test
    void fromStatusNamesAreResolved() {
        assertEquals(List.of(1, 2), IntentParser.parse(YAML)
                                                .getActions()
                                                .get(0)
                                                .getFromStatus());
        assertIssue(YAML.replace("[OPEN, ON_HOLD]", "[OPEN, PENDING]"), "names [PENDING], which is not a seeded status of [TicketStatus]");
    }

    @Test
    void fromStatusOnAnEntityWithoutStatusIsRefused() {
        String onNote = YAML.replace("forEntity: Ticket", "forEntity: Note");
        assertIssue(onNote, "action [EscalateTicket] fromStatus names the status [OPEN]");
        assertIssue(onNote.replace("[OPEN, ON_HOLD]", "[1, 2]"), "declares fromStatus but [Note] has no `function: EntityStatus` relation");
    }

    @Test
    void aTokenNoRoleListsIsAnIssue() {
        assertIssue(YAML.replace("can: Ticket:escalate", "can: Ticket:escalte"),
                "can [Ticket:escalte] is listed by no role under permissions - the button would be shown to nobody");
    }

    @Test
    void aPromptInputNeedsAKnownType() {
        assertIssue(YAML.replace("type: integer", "type: number"), "prompt input [level] has type [number]");
    }

    @Test
    void promptAndConfirmAreExclusive() {
        assertIssue(YAML.replace("    success: Escalation requested", "    success: Escalation requested\n    confirm: Sure?"),
                "declares both prompt and confirm");
    }

    @Test
    void aPageActionRefusesTheKeysOnlyACallUses() {
        String page = """
                name: helpdesk
                entities:
                  - name: Note
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                actions:
                  - { name: OpenNote, forEntity: Note, page: /services/web/helpdesk/note.html, confirm: "Sure?", success: Done }
                """;
        assertIssue(page, "action [OpenNote] declares [confirm, success], which only a call: action uses");
    }

    @Test
    void aTokenBoundByAnActionIsNoAdvisory() {
        IntentModel model = IntentParser.parse(YAML);

        assertTrue(PermissionSupport.gates(model)
                                    .advisories()
                                    .isEmpty(),
                "Ticket:escalate gates the Escalate button: " + PermissionSupport.gates(model)
                                                                                 .advisories());
        assertTrue(PermissionSupport
                                    .gates(IntentParser.parse(YAML.replace("can: [Ticket:read, Ticket:escalate]",
                                            "can: [Ticket:read, Ticket:escalate, Ticket:close]")))
                                    .advisories()
                                    .stream()
                                    .anyMatch(advisory -> advisory.contains("Ticket:close")),
                "a token no action binds is still reported");
    }

    /** {@code primary: true} (#8119): opt-in, and kept on the model for the generators. */
    @Test
    void aPrimaryEntityActionParses() {
        assertTrue(IntentParser
                               .parse(YAML.replace("    success: Escalation requested",
                                       "    success: Escalation requested\n    primary: true"))
                               .getActions()
                               .get(0)
                               .isPrimary());
        assertFalse(IntentParser.parse(YAML)
                                .getActions()
                                .get(0)
                                .isPrimary(),
                "primary is opt-in");
    }

    /** The filled button is a record's, so a page-scope action cannot be primary (#8119). */
    @Test
    void aPrimaryPageActionIsRefused() {
        assertIssue(
                YAML.replace("    success: Escalation requested", "    success: Escalation requested\n    primary: true\n    scope: page"),
                "declares primary but has scope [page]");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException thrown = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(thrown.getIssues()
                         .stream()
                         .anyMatch(issue -> issue.contains(expected)),
                "expected an issue containing [" + expected + "] but got " + thrown.getIssues());
    }
}
