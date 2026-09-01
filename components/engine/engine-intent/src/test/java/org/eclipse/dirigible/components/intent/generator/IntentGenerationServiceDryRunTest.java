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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.dirigible.components.intent.generator.action.ActionIntentGenerator;
import org.eclipse.dirigible.components.intent.generator.apptest.AppTestIntentGenerator;
import org.eclipse.dirigible.components.intent.generator.bpmn.BpmnIntentGenerator;
import org.eclipse.dirigible.components.intent.generator.csvim.CsvimIntentGenerator;
import org.eclipse.dirigible.components.intent.generator.edm.EdmIntentGenerator;
import org.eclipse.dirigible.components.intent.generator.form.FormIntentGenerator;
import org.eclipse.dirigible.components.intent.generator.generates.GeneratesIntentGenerator;
import org.eclipse.dirigible.components.intent.generator.permission.PermissionIntentGenerator;
import org.eclipse.dirigible.components.intent.generator.print.PrintIntentGenerator;
import org.eclipse.dirigible.components.intent.generator.report.ReportIntentGenerator;
import org.eclipse.dirigible.components.intent.generator.transition.TransitionsIntentGenerator;
import org.eclipse.dirigible.repository.local.LocalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The write-nothing generation pass behind the assistant's extended validation and the editor's
 * {@code /validate} endpoint (dirigible #6956): the FULL generator chain runs and reports exactly
 * as a real Generate would, but the repository is left byte-identical - and a document with no
 * project context never produces a false cross-model "cannot be resolved" issue.
 */
class IntentGenerationServiceDryRunTest {

    /**
     * Parses cleanly, generates cleanly - and its generation-time warning is REMOVABLE: strip the
     * {@code stage:} classifications and the event-driven create-from warns that its at-most-once guard
     * cannot recognize a retired target. Exactly the band the parser cannot see.
     */
    private static final String CLEAN_YAML = """
            name: fines
            entities:
              - name: FineStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: DeclarationState
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Fine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string }
                relations:
                  - { name: Status, kind: manyToOne, to: FineStatus, function: EntityStatus, init: 1 }
              - name: Declaration
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string }
                relations:
                  - { name: Fine, kind: manyToOne, to: Fine }
                  - { name: State, kind: manyToOne, to: DeclarationState, function: EntityStatus, init: 1 }
            generates:
              - name: declaration-from-fine
                from: Fine
                to: Declaration
                forEntity: Fine
                event: { onTransition: Fine, when: "Status == POSTED" }
                map:
                  Fine: id
                  Note: note
            seeds:
              - name: fine-statuses
                entity: FineStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: POSTED }
              - name: declaration-states
                entity: DeclarationState
                rows:
                  - { id: 1, name: DRAFT,  stage: draft }
                  - { id: 2, name: FILED,  stage: live }
                  - { id: 3, name: VOIDED, stage: void }
            """;

    /** The same document with the {@code stage:} classifications stripped - generation must object. */
    private static final String UNSTAGED_YAML = CLEAN_YAML.replaceAll(",\\s+stage: \\w+", "");

    /** A document leaning on another model - which no project context can resolve. */
    private static final String CROSS_MODEL_YAML = """
            name: timesheets
            uses:
              - { model: projects }
            entities:
              - name: DayAllocation
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: hours, type: decimal }
                relations:
                  - { name: Project, kind: manyToOne, to: Project, model: projects }
            rollups:
              - { name: projectActualHours, entity: DayAllocation, via: Project, field: actualHours,
                  op: sum, of: hours }
            """;

    private static IntentGenerationService service(LocalRepository repository) {
        return new IntentGenerationService(List.of(new EdmIntentGenerator(), new GlueIntentGenerator(), new BpmnIntentGenerator(),
                new FormIntentGenerator(), new ReportIntentGenerator(), new PrintIntentGenerator(), new CsvimIntentGenerator(),
                new PermissionIntentGenerator(), new TransitionsIntentGenerator(), new GeneratesIntentGenerator(),
                new ActionIntentGenerator(), new AppTestIntentGenerator(), new CalculatedActionStubGenerator(),
                new ServiceTaskHandlerGenerator()), repository, null);
    }

    @Test
    void aDryRunWithoutAProjectReportsWhatGenerationWouldObjectTo(@TempDir Path root) {
        IntentGenerationService service = service(new LocalRepository(root.toString(), true));

        List<String> issues = service.dryRun(UNSTAGED_YAML);

        assertTrue(issues.stream()
                         .anyMatch(issue -> issue.contains("declaration-from-fine") && issue.contains("stage:")),
                "the generation-layer warning must surface through the dry run: " + issues);
    }

    @Test
    void aCleanDocumentRaisesNoIssues(@TempDir Path root) {
        IntentGenerationService service = service(new LocalRepository(root.toString(), true));

        assertEquals(List.of(), service.dryRun(CLEAN_YAML));
    }

    @Test
    void aDryRunWritesNothingAndScrubsNothing(@TempDir Path root) {
        LocalRepository repository = new LocalRepository(root.toString(), true);
        // A project that already exists, with a previously generated file the intent no longer
        // declares - the exact thing a real pass would scrub - and no .settings, the exact thing a
        // first real pass would scaffold.
        String projectRoot = "/users/admin/workspace/fines";
        repository.createResource(projectRoot + "/stale.form", "{}".getBytes(StandardCharsets.UTF_8));
        repository.createResource(projectRoot + "/fines.intent", CLEAN_YAML.getBytes(StandardCharsets.UTF_8));
        IntentGenerationService service = service(repository);
        Map<String, byte[]> before = snapshot(root);

        service.dryRun(CLEAN_YAML, projectRoot, "fines", "workspace", "fines");

        Map<String, byte[]> after = snapshot(root);
        assertEquals(before.keySet(), after.keySet(), "a dry run must create and delete nothing");
        before.forEach((file, content) -> assertTrue(Arrays.equals(content, after.get(file)), "a dry run must not rewrite [" + file + "]"));
    }

    @Test
    void aCrossModelDocumentWithoutAProjectProducesNoFalseResolutionIssues(@TempDir Path root) {
        // The load-bearing filter (dirigible #6956): with no project context, a `uses:` dependency
        // resolves by naming convention instead of failing "no model found in the workspace or the
        // registry" - a false positive would teach the repair loop to "fix" correct YAML.
        IntentGenerationService service = service(new LocalRepository(root.toString(), true));

        List<String> issues = service.dryRun(CROSS_MODEL_YAML);

        assertTrue(issues.stream()
                         .noneMatch(issue -> issue.contains("cannot be resolved")),
                "an unresolvable-only-because-there-is-no-project reference is not an issue: " + issues);
    }

    /** Every file under the repository root, relative path to content. */
    private static Map<String, byte[]> snapshot(Path root) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        try (Stream<Path> tree = Files.walk(root)) {
            tree.filter(Files::isRegularFile)
                .sorted()
                .forEach(file -> {
                    try {
                        files.put(root.relativize(file)
                                      .toString(),
                                Files.readAllBytes(file));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }
}
