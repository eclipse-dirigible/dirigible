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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.generates.GeneratesIntentGenerator;
import org.eclipse.dirigible.components.intent.model.GeneratesIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.google.gson.Gson;

/**
 * The guard of a create-from on the source's own values (issue #7992): {@code when:} with its
 * {@code whenMessage:}, parsed in the {@code forbidWhen} vocabulary, rendered for the controller
 * that refuses the click, and mirrored on the button descriptor that stops offering it.
 */
class GeneratesWhenGuardTest {

    private static final String HEAD = """
            name: billing
            entities:
              - name: TemplateStatus
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: InvoiceTemplate
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: name,   type: string }
                  - { name: active, type: boolean }
                relations:
                  - { name: Status, kind: manyToOne, to: TemplateStatus, function: EntityStatus, init: 1 }
              - name: Invoice
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string }
                relations:
                  - { name: Template, kind: manyToOne, to: InvoiceTemplate }
            seeds:
              - name: template-statuses
                entity: TemplateStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: APPROVED }
            generates:
              - name: invoice-from-template
                from: InvoiceTemplate
                to: Invoice
                forEntity: InvoiceTemplate
                map: { Template: id, Note: name }
            """;

    @Test
    void aWhenGuardParsesAndResolvesStatusNames() {
        GeneratesIntent g = IntentParser.parse(HEAD + """
                    when: ["active == true", "Status == APPROVED"]
                    whenMessage: "Activate the template first"
                """)
                                        .getGenerates()
                                        .get(0);

        assertTrue(g.hasWhen());
        assertEquals(List.of("active == true", "Status == 2"), g.getWhen());
        assertEquals("Activate the template first", g.getWhenMessage());
    }

    @Test
    void aCreateFromWithoutTheKeyHasNoGuard() {
        Map<String, Object> e = GlueIntentGenerator.buildGeneratesForTest(IntentParser.parse(HEAD))
                                                   .get(0);

        assertFalse(IntentParser.parse(HEAD)
                                .getGenerates()
                                .get(0)
                                .hasWhen());
        assertEquals(false, e.get("hasWhenGuard"));
        assertEquals("", e.get("whenMessage"));
    }

    @Test
    void rejectsAWhenWithoutAMessage() {
        assertIssue(HEAD + """
                    when: "active == true"
                """, "whenMessage");
    }

    @Test
    void rejectsAMessageWithoutAWhen() {
        assertIssue(HEAD + """
                    whenMessage: "Activate the template first"
                """, "whenMessage with no when");
    }

    @Test
    void rejectsATermThatDoesNotRead() {
        assertIssue(HEAD + """
                    when: "paused == true"
                    whenMessage: "Activate the template first"
                """, "does not read");
    }

    @Test
    void rejectsAWhenOnAPageScopedAction() {
        assertIssue(HEAD + """
                    scope: page
                    when: "active == true"
                    whenMessage: "Activate the template first"
                """, "page");
    }

    @Test
    void rejectsAWhenOnAnEventOnlyCreateFrom() {
        assertIssue(HEAD + """
                    event: { onCreate: InvoiceTemplate }
                    when: "active == true"
                    whenMessage: "Activate the template first"
                """, "no button");
    }

    @Test
    void rejectsAWhenOnAScheduledGeneration() {
        assertIssue("""
                name: billing
                entities:
                  - name: InvoiceTemplate
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: active, type: boolean }
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Template, kind: manyToOne, to: InvoiceTemplate }
                schedules:
                  - name: monthly
                    cron: "0 0 1 1 * ?"
                    entity: InvoiceTemplate
                    generate:
                      to: Invoice
                      map: { Template: id }
                      when: "active == true"
                      whenMessage: "Activate the template first"
                """, "generate declares when");
    }

    @Test
    void theGlueCarriesTheTermsOverTheLockedSourceAndTheMessage() {
        Map<String, Object> e = GlueIntentGenerator.buildGeneratesForTest(IntentParser.parse(HEAD + """
                    when: ["active == true", "Status == APPROVED"]
                    whenMessage: "Activate the template first"
                """))
                                                   .get(0);

        assertEquals(true, e.get("hasWhenGuard"));
        assertEquals("Activate the template first", e.get("whenMessage"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> terms = (List<Map<String, Object>>) e.get("whenGuardTerms");
        assertEquals(2, terms.size());
        assertEquals("source", terms.get(0)
                                    .get("owner"));
        assertEquals("Active", terms.get(0)
                                    .get("property"));
        assertEquals("boolean", terms.get(0)
                                     .get("type"));
        assertEquals("true", terms.get(0)
                                  .get("value"));
        assertEquals("Status", terms.get(1)
                                    .get("property"));
        assertEquals("2", terms.get(1)
                               .get("value"));
        // It combines with the status guard rather than replacing it: no fromStatus, no guard here.
        assertEquals(false, e.get("hasStatusGuard"));
    }

    @Test
    void theButtonDescriptorMirrorsTheCondition() {
        Map<?, ?> descriptor = new Gson().fromJson(descriptor(HEAD + """
                    when: ["active == true", "Status != DRAFT"]
                    whenMessage: "Activate the template first"
                """), Map.class);

        List<?> when = (List<?>) descriptor.get("when");
        assertEquals(2, when.size());
        Map<?, ?> first = (Map<?, ?>) when.get(0);
        assertEquals("Active", first.get("property"));
        assertEquals(true, first.get("equal"));
        assertEquals("true", first.get("value"));
        Map<?, ?> second = (Map<?, ?>) when.get(1);
        assertEquals("Status", second.get("property"));
        assertEquals(false, second.get("equal"));
        assertEquals("1", second.get("value"));
    }

    @Test
    void aNullTestStaysServerSide() {
        Map<?, ?> descriptor = new Gson().fromJson(descriptor(HEAD + """
                    when: "name != null"
                    whenMessage: "Name the template first"
                """), Map.class);

        assertNull(descriptor.get("when"), "text comparison cannot tell null from a value the row does not carry");
        assertEquals(true, GlueIntentGenerator.buildGeneratesForTest(IntentParser.parse(HEAD + """
                    when: "name != null"
                    whenMessage: "Name the template first"
                """))
                                              .get(0)
                                              .get("hasWhenGuard"));
    }

    @Test
    void withoutTheKeyTheDescriptorCarriesNoWhen() {
        Map<?, ?> descriptor = new Gson().fromJson(descriptor(HEAD), Map.class);

        assertFalse(descriptor.containsKey("when"));
    }

    private static void assertIssue(String yaml, String fragment) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(fragment)),
                "got: " + ex.getIssues());
    }

    private static String descriptor(String yaml) {
        IntentModel model = IntentParser.parse(yaml);
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(repository.getResource(anyString())).thenReturn(missing);
        when(missing.exists()).thenReturn(false);
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "billing", repository);

        new GeneratesIntentGenerator().generate(context);

        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contents = ArgumentCaptor.forClass(byte[].class);
        verify(repository, atLeastOnce()).createResource(paths.capture(), contents.capture());
        for (int i = 0; i < paths.getAllValues()
                                 .size(); i++) {
            if (paths.getAllValues()
                     .get(i)
                     .endsWith("/invoice-from-template-generate-action.json")) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        return fail("the action descriptor was not written: " + paths.getAllValues());
    }
}
