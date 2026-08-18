/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.apptest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.TestContexts;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.local.LocalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

/**
 * The {@code <name>.test} manifest is written ONCE and belongs to the developer afterwards
 * (dirigible #6755). A behavioural test re-derived from the intent on every Generate inherits the
 * generator's blind spots and passes exactly when the generator is consistently wrong; it is worth
 * something only when a human can state independently what the module must do, which requires the
 * file to survive regeneration.
 *
 * <p>
 * Exercises the write surface itself against a real repository - what the generator creates,
 * overwrites, or leaves alone - which the manifest-content tests
 * ({@link AppTestIntentGeneratorTest}) deliberately do not touch.
 */
class AppTestManifestWriteModeTest {

    private static final String PROJECT_ROOT = "/proj";
    private static final String BASE_NAME = "countries";
    private static final String MANIFEST = BASE_NAME + ".test";

    private static final String INTENT = """
            name: countries
            entities:
              - name: Country
                group: master-data
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true }
            """;

    /** The slice of the {@code .model} this generator reads back, as the EDM generator writes it. */
    private static final String MODEL = """
            {
              "model": {
                "entities": [
                  {
                    "name": "Country",
                    "dataName": "COUNTRIES_COUNTRY",
                    "perspectiveName": "Settings",
                    "perspectiveNavId": "master-data",
                    "properties": [ { "name": "Id", "dataPrimaryKey": "true" } ]
                  }
                ]
              }
            }
            """;

    /** What a developer adds by hand and must not lose - the seed expectation from the issue. */
    private static final String HAND_AUTHORED = """
            {
              "module": "countries",
              "entities": [
                {
                  "name": "Country",
                  "table": "COUNTRIES_COUNTRY",
                  "expectSeedData": true,
                  "seedSample": [ { "Id": 1, "Name": "My Company" } ]
                }
              ]
            }
            """;

    private final AppTestIntentGenerator generator = new AppTestIntentGenerator();
    private final IntentModel model = IntentParser.parse(INTENT);

    private IRepository repository;

    @BeforeEach
    void setUp(@TempDir Path root) {
        repository = new LocalRepository(root.toString(), true);
        repository.createResource(PROJECT_ROOT + "/" + BASE_NAME + ".model", MODEL.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void scaffoldsTheManifestForAModuleThatHasNone() {
        IntentGenerationContext context = context();

        generator.generate(context);

        assertTrue(repository.getResource(PROJECT_ROOT + "/" + MANIFEST)
                             .exists(),
                "a new module gets its scaffold");
        assertTrue(content().contains("\"module\": \"countries\""), "the scaffold is the generated manifest");
    }

    @Test
    void regenerationKeepsHandAuthoredAssertions() {
        generator.generate(context());
        // the developer enhances the scaffold - the assertions that make the test worth running
        repository.getResource(PROJECT_ROOT + "/" + MANIFEST)
                  .setContent(HAND_AUTHORED.getBytes(StandardCharsets.UTF_8));

        generator.generate(context());

        assertEquals(HAND_AUTHORED, content(), "Generate must not overwrite a manifest the developer owns");
    }

    @Test
    void anExistingManifestIsKeptOutOfTheStaleOutputScrub() {
        repository.createResource(PROJECT_ROOT + "/" + MANIFEST, HAND_AUTHORED.getBytes(StandardCharsets.UTF_8));
        IntentGenerationContext context = context();

        generator.generate(context);

        // the scrub removes every intent-owned root file the pass did not claim, and it owns `.test`
        assertTrue(context.getWrittenFileNames()
                          .contains(MANIFEST),
                "an untouched manifest must still be claimed, or the scrub deletes it");
    }

    /**
     * The generator bails out before reading the {@code .model}, so a module whose model cannot be
     * described keeps its manifest instead of having it scrubbed - the case that would otherwise slip
     * through the early return.
     */
    @Test
    void anExistingManifestSurvivesAnUndescribableModel() {
        repository.removeResource(PROJECT_ROOT + "/" + BASE_NAME + ".model");
        repository.createResource(PROJECT_ROOT + "/" + MANIFEST, HAND_AUTHORED.getBytes(StandardCharsets.UTF_8));
        IntentGenerationContext context = context();

        generator.generate(context);

        assertEquals(HAND_AUTHORED, content());
        assertTrue(context.getWrittenFileNames()
                          .contains(MANIFEST));
    }

    @Test
    void nothingIsScaffoldedWhenTheModelDescribesNoEntities() {
        repository.removeResource(PROJECT_ROOT + "/" + BASE_NAME + ".model");
        IntentGenerationContext context = context();

        generator.generate(context);

        assertFalse(repository.getResource(PROJECT_ROOT + "/" + MANIFEST)
                              .exists());
        assertFalse(context.getWrittenFileNames()
                           .contains(MANIFEST));
    }

    private IntentGenerationContext context() {
        return TestContexts.context(model, repository, PROJECT_ROOT, BASE_NAME);
    }

    private String content() {
        return new String(repository.getResource(PROJECT_ROOT + "/" + MANIFEST)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }
}
