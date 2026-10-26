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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.intent.generator.action.ActionIntentGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * The client descriptor of an action that calls the application's own endpoint (issue #8090): the
 * shell's customActions store builds the request, the dialog, the advisory guard and the toast from
 * it alone, and the views aggregation filters it by its roles.
 */
class CallActionDescriptorTest {

    private static final String YAML = """
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
                  body: { kind: MANUAL, level: "{level}", note: "{note}", priority: 2 }
                success: Escalation requested
              - name: ReopenTicket
                forEntity: Ticket
                confirm: Reopen this ticket?
                call: { method: PUT, path: "/services/java/helpdesk/custom/api/TicketApi/tickets/{id}/reopen" }
              - name: OpenNotes
                forEntity: Ticket
                icon: file
                order: 10
                page: /services/web/helpdesk/notes.html
            permissions:
              - role: SupportLead
                can: [Ticket:read, Ticket:escalate]
              - role: Auditor
                can: [Ticket:read]
              - role: Manager
                can: [Ticket:escalate]
            seeds:
              - name: ticket-statuses
                entity: TicketStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: ON_HOLD }
                  - { id: 3, name: CLOSED }
            """;

    @Test
    void theDescriptorCarriesTheCallTheDialogTheGuardAndTheRoles() {
        String descriptor = compact(descriptor(YAML, "EscalateTicket"));

        assertTrue(descriptor.contains(
                "\"call\":{\"method\":\"POST\",\"path\":\"/services/java/helpdesk/custom/api/TicketApi/tickets/{id}/escalations\","
                        + "\"body\":{\"kind\":\"MANUAL\",\"level\":\"{level}\",\"note\":\"{note}\",\"priority\":2}}"),
                descriptor);
        assertTrue(descriptor.contains("\"prompt\":[{\"name\":\"level\",\"required\":true},{\"name\":\"note\",\"required\":false}]"),
                descriptor);
        assertTrue(descriptor.contains("\"promptColumns\":[{\"name\":\"level\",\"label\":\"Level\",\"widget\":\"NUMBER\"},"
                + "{\"name\":\"note\",\"label\":\"Note\",\"widget\":\"TEXT\"}]"), descriptor);
        assertTrue(descriptor.contains("\"guard\":{\"property\":\"Status\",\"allowed\":[1,2]}"), descriptor);
        assertTrue(descriptor.contains("\"roles\":[\"SupportLead\",\"Manager\"]"), descriptor);
        assertTrue(descriptor.contains("\"success\":\"Escalationrequested\""), descriptor);
        assertFalse(descriptor.contains("\"path\":\"/services/web"), "a call action opens no page: " + descriptor);
    }

    @Test
    void aCallWithoutPromptCarriesItsConfirmationAndNoBody() {
        String descriptor = compact(descriptor(YAML, "ReopenTicket"));

        assertTrue(
                descriptor.contains(
                        "\"call\":{\"method\":\"PUT\",\"path\":\"/services/java/helpdesk/custom/api/TicketApi/tickets/{id}/reopen\"}"),
                descriptor);
        assertTrue(descriptor.contains("\"confirm\":\"Reopenthisticket?\""), descriptor);
        assertFalse(descriptor.contains("prompt"), descriptor);
        assertFalse(descriptor.contains("guard"), descriptor);
        assertFalse(descriptor.contains("roles"), "an action bound to no token is offered to everyone: " + descriptor);
    }

    @Test
    void aPageActionIsByteIdenticalToTodays() {
        assertEquals("""
                {
                  "id": "proj-Ticket-OpenNotes",
                  "label": "Open Notes",
                  "path": "/services/web/helpdesk/notes.html",
                  "view": "Ticket",
                  "type": "entity",
                  "icon": "file",
                  "order": 10
                }""", descriptor(YAML, "OpenNotes"));
    }

    /** The descriptor's JSON with its pretty-printing whitespace removed, for readable assertions. */
    /** The descriptor carries {@code primary} when it is declared, and only then (#8119). */
    @Test
    void aPrimaryActionCarriesTheFlagOnlyWhenDeclared() {
        String primary = compact(
                descriptor(YAML.replace("    success: Escalation requested", "    success: Escalation requested\n    primary: true"),
                        "EscalateTicket"));
        assertTrue(primary.contains("\"primary\":true"), primary);
        String plain = compact(descriptor(YAML, "EscalateTicket"));
        assertTrue(!plain.contains("\"primary\""), plain);
    }

    private static String compact(String descriptor) {
        return descriptor.replaceAll("\\s+", "");
    }

    /** The written {@code <action>-action.json} descriptor module. */
    private static String descriptor(String yaml, String action) {
        IntentModel model = IntentParser.parse(yaml);
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(repository.getResource(anyString())).thenReturn(missing);
        when(missing.exists()).thenReturn(false);
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "helpdesk", repository);

        new ActionIntentGenerator().generate(context);

        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contents = ArgumentCaptor.forClass(byte[].class);
        verify(repository, atLeastOnce()).createResource(paths.capture(), contents.capture());
        for (int i = 0; i < paths.getAllValues()
                                 .size(); i++) {
            if (paths.getAllValues()
                     .get(i)
                     .endsWith("/" + action + "-action.json")) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        return fail("the action descriptor was not written: " + paths.getAllValues());
    }
}
