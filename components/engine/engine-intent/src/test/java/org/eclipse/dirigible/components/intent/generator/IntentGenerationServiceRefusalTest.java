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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.eclipse.dirigible.repository.local.LocalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A generation pass that is REFUSED must leave the workspace exactly as it found it (dirigible
 * #7227).
 *
 * <p>
 * The generators run in {@code @Order}, so a generation-time check is reached only after the
 * earlier generators have written their model files - and the loop rethrows the
 * {@link IntentValidationException} straight to the caller, past the stale-output scrub. The
 * reported case is the cross-model {@code relation.field} check (#7093, at {@code @Order(350)}):
 * its whole justification is that skipping the resolver would leave the BPMN with a
 * {@code Resolve<...>} service task whose handler nothing generated - which is precisely what the
 * 422 itself used to leave behind, since {@code BpmnIntentGenerator} (300) emits that task from the
 * lookup-free convention regardless of whether the field exists. Refusing at generation must cost
 * the developer no more than refusing at parse did.
 */
class IntentGenerationServiceRefusalTest {

    private static final String PROJECT_ROOT = "/users/admin/workspace/sales-invoices";

    /** The reported document: a task form showing a field of a cross-model to-one. */
    private static final String YAML = """
            name: sales-invoices
            uses:
              - { model: customers }
            entities:
              - name: SalesInvoice
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer, model: customers, required: true }
            processes:
              - name: Send
                trigger: { onCreate: SalesInvoice }
                steps:
                  - { name: send, kind: userTask, args: { assignee: clerk, form: SendSalesInvoice } }
                  - { name: done, kind: end }
            forms:
              - name: SendSalesInvoice
                forEntity: SalesInvoice
                fields: [number, Customer, Customer.email]
                actions: [send]
            """;

    /** The same document naming a field the owner model does not declare - refused at generation. */
    private static final String BROKEN_YAML = YAML.replace("Customer.email", "Customer.mobile");

    /** The owner model as the customers project generated it. */
    private static final String OWNER_MODEL = """
            {
              "model": {
                "entities": [
                  {
                    "name": "Customer",
                    "perspectiveName": "Customer",
                    "dataName": "CUSTOMERS_CUSTOMER",
                    "properties": [
                      { "name": "Id", "dataName": "ID", "dataType": "INTEGER", "dataPrimaryKey": "true" },
                      { "name": "Name", "dataName": "NAME", "dataType": "VARCHAR" },
                      { "name": "Email", "dataName": "EMAIL", "dataType": "VARCHAR" }
                    ]
                  }
                ]
              }
            }
            """;

    @Test
    void aRefusedFirstPassLeavesNothingBehind(@TempDir Path root) {
        LocalRepository repository = seed(root, BROKEN_YAML);
        IntentGenerationService service = service(repository);
        Map<String, byte[]> before = snapshot(root);

        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> service.generate(BROKEN_YAML, PROJECT_ROOT, "sales-invoices", "workspace", "sales-invoices"));

        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("Mobile")),
                "the refusal must still name the unknown property, got: " + ex.getIssues());
        // The point of the issue: no .edm/.model from @Order(200) and - above all - no .bpmn from
        // @Order(300) carrying a ResolveCustomerMobile service task whose handler was never generated.
        Map<String, byte[]> after = snapshot(root);
        assertEquals(before.keySet(), after.keySet(), "a refused pass must leave no generated file behind");
        after.forEach((file, content) -> assertFalse(new String(content, StandardCharsets.UTF_8).contains("esolveCustomerMobile"),
                "[" + file + "] must not carry the dangling resolver the refusal exists to prevent"));
    }

    @Test
    void aRefusedRegenerationRestoresThePreviousOutput(@TempDir Path root) {
        LocalRepository repository = seed(root, YAML);
        IntentGenerationService service = service(repository);
        // A first pass that succeeds - the workspace now holds a full, consistent model set.
        service.generate(YAML, PROJECT_ROOT, "sales-invoices", "workspace", "sales-invoices");
        Map<String, byte[]> generated = snapshot(root);
        assertTrue(generated.keySet()
                            .stream()
                            .anyMatch(file -> file.endsWith("Send.bpmn")),
                "the successful pass must have written the process: " + generated.keySet());

        assertThrows(IntentValidationException.class,
                () -> service.generate(BROKEN_YAML, PROJECT_ROOT, "sales-invoices", "workspace", "sales-invoices"));

        Map<String, byte[]> after = snapshot(root);
        assertEquals(generated.keySet(), after.keySet(), "a refused regeneration must neither add nor remove a file");
        generated.forEach((file, content) -> assertArrayEquals(content, after.get(file),
                "a refused regeneration must leave [" + file + "] as the last good pass wrote it"));
    }

    /** The project as the developer has it: the intent, and the sibling owner project's model. */
    private static LocalRepository seed(Path root, String yaml) {
        LocalRepository repository = new LocalRepository(root.toString(), true);
        repository.createResource(PROJECT_ROOT + "/sales-invoices.intent", yaml.getBytes(StandardCharsets.UTF_8));
        repository.createResource("/users/admin/workspace/customers/customers.model", OWNER_MODEL.getBytes(StandardCharsets.UTF_8));
        return repository;
    }

    /**
     * The generators in their real {@code @Order} - the ordering IS the defect's mechanism, so the
     * harness must not quietly fix it: EDM (200) and BPMN (300) write before the glue pass (350)
     * refuses.
     */
    private static IntentGenerationService service(LocalRepository repository) {
        return new IntentGenerationService(List.of(new EdmIntentGenerator(), new BpmnIntentGenerator(), new GlueIntentGenerator(),
                new ServiceTaskHandlerGenerator(), new CalculatedActionStubGenerator(), new FormIntentGenerator(),
                new ActionIntentGenerator(), new GeneratesIntentGenerator(), new TransitionsIntentGenerator(), new ReportIntentGenerator(),
                new PermissionIntentGenerator(), new CsvimIntentGenerator(), new PrintIntentGenerator(), new AppTestIntentGenerator()),
                repository, null);
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
