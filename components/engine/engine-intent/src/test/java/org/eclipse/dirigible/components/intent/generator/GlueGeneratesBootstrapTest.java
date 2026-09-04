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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.GeneratesIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;

/**
 * A MUTUAL cross-model {@code generates} pair has no leaf to generate first (dirigible #6539): the
 * opportunity model mints a quotation into the quotations model, which holds a foreign key back to
 * the opportunity - so each pass needs the other project's {@code .model}. The declared bootstrap
 * pass emits everything except that create-from; a default pass says so, with the recipe.
 */
class GlueGeneratesBootstrapTest {

    private static final String OPPORTUNITIES = """
            name: opportunities
            uses:
              - { model: quotations }
            entities:
              - name: Opportunity
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: subject, type: string }
            generates:
              - name: quotation-from-opportunity
                from: Opportunity
                to: Quotation
                uses: quotations
                map:
                  Subject: subject
                  Opportunity: id
            """;

    /** The owner model, generated: enough of {@code Quotation} for the create-from to resolve. */
    private static final String QUOTATIONS_MODEL = """
            {
              "model": {
                "entities": [
                  {
                    "name": "Quotation",
                    "perspectiveName": "Quotation",
                    "dataName": "QUOTATIONS_QUOTATION",
                    "properties": [
                      { "name": "Id", "dataName": "ID", "dataType": "INTEGER", "dataPrimaryKey": "true" },
                      { "name": "Subject", "dataName": "SUBJECT", "dataType": "VARCHAR" },
                      { "name": "Opportunity", "dataName": "OPPORTUNITY", "dataType": "INTEGER",
                        "widgetType": "DROPDOWN", "relationshipEntityName": "Opportunity" }
                    ]
                  }
                ]
              }
            }
            """;

    private static final String OWNER_MODEL_PATH = "/users/admin/workspace/quotations/quotations.model";
    private static final String PROJECT_ROOT = "/users/admin/workspace/opportunities";

    /**
     * A repository serving the given content at {@link #OWNER_MODEL_PATH}, or nothing at all when it is
     * {@code null} - the fresh-bootstrap state, where the quotations project has never been generated.
     */
    private static IRepository repositoryWithOwnerModel(String content) {
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(missing.exists()).thenReturn(false);
        when(repository.getResource(anyString())).thenReturn(missing);
        if (content != null) {
            IResource owner = mock(IResource.class);
            when(owner.exists()).thenReturn(true);
            when(owner.getContent()).thenReturn(content.getBytes(StandardCharsets.UTF_8));
            when(repository.getResource(OWNER_MODEL_PATH)).thenReturn(owner);
        }
        return repository;
    }

    @Test
    void aDefaultPassFailsWithTheBootstrapRecipe() {
        IntentModel model = IntentParser.parse(OPPORTUNITIES);
        IntentGenerationContext context = TestContexts.context(model, repositoryWithOwnerModel(null), PROJECT_ROOT, "app");

        BootstrapRequiredException ex =
                assertThrows(BootstrapRequiredException.class, () -> GlueIntentGenerator.buildGeneratesForTest(model, context));

        // The generic "generate the dependency first" is advice a mutual pair cannot follow, so the
        // message has to name the way out - the whole point of the dedicated exception type.
        String issue = ex.getIssues()
                         .get(0);
        assertTrue(issue.contains("quotation-from-opportunity"), "the message must name the create-from: " + issue);
        assertTrue(issue.contains("BOOTSTRAP"), "the message must name the escape from the cycle: " + issue);
        assertTrue(issue.contains("mutual cross-model cycle"), "the message must name the shape: " + issue);
    }

    @Test
    void aBootstrapPassSkipsTheCreateFromAndSaysSo() {
        IntentModel model = IntentParser.parse(OPPORTUNITIES);
        IntentGenerationContext context = TestContexts.bootstrapContext(model, repositoryWithOwnerModel(null), PROJECT_ROOT);

        assertTrue(GlueIntentGenerator.buildGeneratesForTest(model, context)
                                      .isEmpty(),
                "the create-from must not be emitted against a model that does not exist yet");
        // Skipped is not dropped: the pass succeeds, and the response has to carry what it did not emit.
        assertEquals(1, context.getIssues()
                               .size(),
                "the skip must be reported: " + context.getIssues());
        String issue = context.getIssues()
                              .get(0);
        assertTrue(issue.contains("quotation-from-opportunity") && issue.contains("regenerate this project"),
                "the warning must name the create-from and the remaining step: " + issue);
    }

    @Test
    void aBootstrapPassEmitsTheCreateFromOnceTheOwnerModelExists() {
        IntentModel model = IntentParser.parse(OPPORTUNITIES);
        IntentGenerationContext context = TestContexts.bootstrapContext(model, repositoryWithOwnerModel(QUOTATIONS_MODEL), PROJECT_ROOT);

        List<Map<String, Object>> generates = GlueIntentGenerator.buildGeneratesForTest(model, context);
        assertEquals(1, generates.size(), "the resolvable create-from must be emitted, bootstrap or not");
        assertEquals("Quotation", generates.get(0)
                                           .get("toPerspective"));
        assertTrue(context.getIssues()
                          .isEmpty(),
                "nothing to report once the dependency is there: " + context.getIssues());
    }

    /**
     * The two halves of a create-from - the server controller from the {@code .glue} and the client
     * button from its own descriptor - must make the SAME decision, or the bootstrap pass ships a
     * button whose endpoint does not exist.
     */
    @Test
    void theClientButtonIsSkippedWithTheControllerItCalls() {
        IntentModel model = IntentParser.parse(OPPORTUNITIES);
        GeneratesIntent action = model.getGenerates()
                                      .get(0);

        assertTrue(
                GeneratesBootstrap.skipped(action, model,
                        TestContexts.bootstrapContext(model, repositoryWithOwnerModel(null), PROJECT_ROOT)),
                "nothing is emitted for a create-from whose owner model is not there yet");
        assertFalse(
                GeneratesBootstrap.skipped(action, model,
                        TestContexts.bootstrapContext(model, repositoryWithOwnerModel(QUOTATIONS_MODEL), PROJECT_ROOT)),
                "a resolvable create-from is emitted whole");
        assertFalse(
                GeneratesBootstrap.skipped(action, model, TestContexts.context(model, repositoryWithOwnerModel(null), PROJECT_ROOT, "app")),
                "outside a bootstrap the pass fails rather than skipping - the glue generator decides that");
    }

    /**
     * The narrowness of the flag: it tolerates a missing owner MODEL, never a reference the owner model
     * contradicts. "The dependency is not generated yet" and "the reference is wrong" want opposite
     * answers, and a bootstrap flag that hid the second would hide it forever.
     */
    @Test
    void aBootstrapPassStillFailsOnAnEntityTheOwnerModelDoesNotDeclare() {
        IntentModel model = IntentParser.parse(OPPORTUNITIES.replace("to: Quotation", "to: Estimate"));
        IntentGenerationContext context = TestContexts.bootstrapContext(model, repositoryWithOwnerModel(QUOTATIONS_MODEL), PROJECT_ROOT);

        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> GlueIntentGenerator.buildGeneratesForTest(model, context));

        assertFalse(ex instanceof BootstrapRequiredException, "a wrong reference is not a bootstrap case");
        assertTrue(ex.getIssues()
                     .get(0)
                     .contains("Cross-model relation target [Estimate]"),
                "the ordinary loud failure must stand: " + ex.getIssues());
    }
}
