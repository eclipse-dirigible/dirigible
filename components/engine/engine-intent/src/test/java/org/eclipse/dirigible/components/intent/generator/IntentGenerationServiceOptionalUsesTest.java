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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.dirigible.components.intent.generator.edm.EdmIntentGenerator;
import org.eclipse.dirigible.components.intent.generator.report.ReportIntentGenerator;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.eclipse.dirigible.repository.local.LocalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * An optional {@code uses:} (dirigible #7979) is decided per Generate: without the owner model in
 * the workspace the pass generates the model without it and says what it left out; once the owner
 * is generated next to it, the same intent generates as a mandatory {@code uses:} does.
 */
class IntentGenerationServiceOptionalUsesTest {

    private static final String OWNER = """
            name: projects
            entities:
              - name: Project
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            """;

    private static final String CONSUMER = """
            name: expenses
            uses:
              - { model: projects, optional: true }
            entities:
              - name: ExpenseLine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Project, kind: manyToOne, to: Project, model: projects }
            reports:
              - { name: LinesByProject, source: ExpenseLine, dimensions: [Project], measures: ["sum(amount)"] }
            """;

    @Test
    void theOwnerDecidesPerGenerate(@TempDir Path root) {
        LocalRepository repository = new LocalRepository(root.toString(), true);
        IntentGenerationService service =
                new IntentGenerationService(List.of(new EdmIntentGenerator(), new ReportIntentGenerator()), repository, null);
        String consumerRoot = "/users/admin/workspace/expenses";
        // A plain uses: cannot be generated without its owner.
        IntentValidationException refused = assertThrows(IntentValidationException.class,
                () -> service.generate(CONSUMER.replace(", optional: true", ""), consumerRoot, "expenses", "workspace", "expenses"));
        assertTrue(refused.getIssues()
                          .toString()
                          .contains("cannot be resolved"),
                refused.getIssues()
                       .toString());

        List<String> without = service.generate(CONSUMER, consumerRoot, "expenses", "workspace", "expenses")
                                      .issues();
        assertEquals(List.of("uses [projects] is optional and its model is neither in this workspace nor published, so it is generated"
                + " without it - left out: relation [ExpenseLine.Project], report [LinesByProject]"), without);
        assertFalse(content(repository, consumerRoot + "/expenses.model").contains("\"Project\""));
        assertFalse(repository.getResource(consumerRoot + "/LinesByProject.report")
                              .exists());

        service.generate(OWNER, "/users/admin/workspace/projects", "projects", "workspace", "projects");
        List<String> with = service.generate(CONSUMER, consumerRoot, "expenses", "workspace", "expenses")
                                   .issues();
        assertTrue(with.stream()
                       .noneMatch(warning -> warning.contains("optional")),
                with.toString());
        assertTrue(content(repository, consumerRoot + "/expenses.model").contains("\"Project\""));
        assertTrue(repository.getResource(consumerRoot + "/LinesByProject.report")
                             .exists());
    }

    private static String content(LocalRepository repository, String path) {
        return new String(repository.getResource(path)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }
}
