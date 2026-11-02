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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

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

    /** The descriptor carries {@code destructive} when it is declared, and only then (#8364). */
    @Test
    void aDestructiveActionCarriesTheFlagOnlyWhenDeclared() {
        String destructive = compact(descriptor(YAML.replace("setStatus: 6", "setStatus: 6\n    destructive: true")));
        assertTrue(destructive.contains("\"destructive\":true"), destructive);
        String plain = compact(descriptor(YAML));
        assertFalse(plain.contains("\"destructive\""), plain);
    }

    /** An invoice its customer owns through a personal relation, so its transition may be personal. */
    private static final String PERSONAL_YAML = """
            name: billing
            entities:
              - name: Customer
                identity: email
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string, unique: true }
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer, personal: true }
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
            transitions:
              - { name: voidInvoice, forEntity: Invoice, from: [3, 4], setStatus: 6, personal: true }
            """;

    /**
     * A personal transition (#8406) contributes a second descriptor for the personal surface: the same
     * action and guard, the personal run's endpoint, and the surface the pages filter by.
     */
    @Test
    void aPersonalTransitionContributesADescriptorForThePersonalSurface() {
        String personal = compact(written(PERSONAL_YAML, "/voidInvoice-transition-my-action.json"));
        assertTrue(personal.contains("\"id\":\"proj-Invoice-voidInvoice-my\""), personal);
        assertTrue(personal.contains("\"endpoint\":\"/services/java/proj/gen/events/billing/VoidInvoiceTransitionMy/run\""), personal);
        assertTrue(personal.contains("\"surface\":\"my\""), personal);
        assertTrue(personal.contains("\"statusProperty\":\"Status\""), personal);
        assertTrue(personal.contains("\"from\":[3,4]"), personal);
        assertTrue(personal.contains("\"kind\":\"transition\""), personal);
        String extension = compact(written(PERSONAL_YAML, "/voidInvoice-transition-my-action.extension"));
        assertTrue(extension.contains("\"module\":\"proj/voidInvoice-transition-my-action.json\""), extension);

        // The power descriptor is the one it always was: no surface, the power run's endpoint.
        String power = compact(written(PERSONAL_YAML, "/voidInvoice-transition-action.json"));
        assertFalse(power.contains("\"surface\""), power);
        assertTrue(power.contains("\"endpoint\":\"/services/java/proj/gen/events/billing/VoidInvoiceTransition/run\""), power);
    }

    /** Without {@code personal: true} the personal surface is offered nothing. */
    @Test
    void aTransitionThatIsNotPersonalContributesNothingToThePersonalSurface() {
        List<String> paths = writtenPaths(YAML);
        assertFalse(paths.stream()
                         .anyMatch(path -> path.contains("-transition-my-action")),
                paths.toString());
    }

    /** The descriptor's JSON with its pretty-printing whitespace removed, for readable assertions. */
    private static String compact(String descriptor) {
        return descriptor.replaceAll("\\s+", "");
    }

    /** The written {@code voidInvoice-transition-action.json} descriptor module. */
    private static String descriptor(String yaml) {
        return written(yaml, "/voidInvoice-transition-action.json");
    }

    /** The content of the generated file whose path ends with {@code suffix}. */
    private static String written(String yaml, String suffix) {
        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contents = ArgumentCaptor.forClass(byte[].class);
        verify(generate(yaml), atLeastOnce()).createResource(paths.capture(), contents.capture());
        for (int i = 0; i < paths.getAllValues()
                                 .size(); i++) {
            if (paths.getAllValues()
                     .get(i)
                     .endsWith(suffix)) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        return fail("[" + suffix + "] was not written: " + paths.getAllValues());
    }

    /** The paths of every file the generator wrote. */
    private static List<String> writtenPaths(String yaml) {
        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        verify(generate(yaml), atLeastOnce()).createResource(paths.capture(), any(byte[].class));
        return paths.getAllValues();
    }

    /** Runs the generator against a repository holding nothing yet, and hands that repository back. */
    private static IRepository generate(String yaml) {
        IntentModel model = IntentParser.parse(yaml);
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(repository.getResource(anyString())).thenReturn(missing);
        when(missing.exists()).thenReturn(false);
        new TransitionsIntentGenerator().generate(new IntentGenerationContext(model, "/proj", "proj", "workspace", "billing", repository));
        return repository;
    }
}
