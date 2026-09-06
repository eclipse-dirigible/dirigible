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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.intent.generator.transition.TransitionsIntentGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * A transition's client descriptor must carry the {@code from:} guard the server enforces
 * (dirigible #7073). Without it the generated UI offered Void on a paid invoice, POSTed it, and
 * dropped the resulting 409 on the floor - the user was left believing the invoice had been voided.
 */
class TransitionActionDescriptorTest {

    private static final String YAML = """
            name: billing
            entities:
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: total, type: decimal }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
            transitions:
              - name: voidInvoice
                forEntity: Invoice
                from: [3, 4]
                setStatus: 6
            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: DRAFT,     stage: draft }
                  - { id: 3, name: ISSUED,    stage: live }
                  - { id: 4, name: PARTIAL,   stage: live }
                  - { id: 6, name: VOID,      stage: void }
            """;

    @Test
    void theDescriptorMirrorsTheFromGuardTheServerEnforces() {
        String descriptor = compact(descriptor(YAML));

        assertTrue(descriptor.contains("\"statusProperty\":\"Status\""),
                "the client needs the property the record carries its status in: " + descriptor);
        assertTrue(descriptor.contains("\"from\":[3,4]"), "the allowed source statuses must reach the button: " + descriptor);
        // A status flip is not a create - the shell words its toast from this.
        assertTrue(descriptor.contains("\"kind\":\"transition\""), descriptor);
    }

    @Test
    void theGuardNamesTheEntitysOwnStatusRelation() {
        // The property is READ off the record in the browser, so it must be the relation this entity
        // actually declares - not the conventional "Status" every model happens to use.
        String descriptor = compact(descriptor(YAML.replace("name: Status,", "name: State,")));

        assertTrue(descriptor.contains("\"statusProperty\":\"State\""), descriptor);
    }

    @Test
    void aWhenGuardIsNotMirroredOntoTheButton() {
        // `when:` is a Calc expression evaluated server-side over the stored record. A client
        // re-implementation of it would drift from the server's and start hiding buttons that work,
        // so only `from:` - a plain status comparison - crosses over. The 409 covers the rest.
        // The text block is already dedented at compile time, so the added key is indented to match
        // its siblings there (4 spaces), not as it reads in the source above.
        String descriptor = compact(descriptor(YAML.replace("setStatus: 6", "setStatus: 6\n    when: \"total == 0\"")));

        assertTrue(descriptor.contains("\"from\":[3,4]"), descriptor);
        assertFalse(descriptor.contains("when"), "a when: guard has no client half: " + descriptor);
    }

    /** The descriptor's JSON with its pretty-printing whitespace removed, for readable assertions. */
    private static String compact(String descriptor) {
        return descriptor.replaceAll("\\s+", "");
    }

    /** The written {@code voidInvoice-transition-action.js} descriptor module. */
    private static String descriptor(String yaml) {
        IntentModel model = IntentParser.parse(yaml);
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(repository.getResource(anyString())).thenReturn(missing);
        when(missing.exists()).thenReturn(false);
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "billing", repository);

        new TransitionsIntentGenerator().generate(context);

        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contents = ArgumentCaptor.forClass(byte[].class);
        verify(repository, atLeastOnce()).createResource(paths.capture(), contents.capture());
        for (int i = 0; i < paths.getAllValues()
                                 .size(); i++) {
            if (paths.getAllValues()
                     .get(i)
                     .endsWith("/voidInvoice-transition-action.js")) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        return fail("the transition descriptor module was not written: " + paths.getAllValues());
    }
}
