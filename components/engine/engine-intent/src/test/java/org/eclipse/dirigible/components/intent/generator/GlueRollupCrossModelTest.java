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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;

/**
 * A roll-up whose PARENT is owned by another model: the child owns the event the handler binds to,
 * while the parent's package and perspective come from the owner's model. This was impossible
 * before - the parser rejected the roll-up because the parent entity is not in the local document,
 * so the canonical case (actual hours summed from a time-tracking model onto a project the projects
 * model owns) had no declarative form at all.
 *
 * The MIRROR case is a cross-model CHILD (#6930): the counted rows are owned by another model and
 * the total lands on a local parent this roll-up names outright - the direction an n:m allocation
 * needs, whose link rows live with one side of the pairing (the invoice) while the other side's
 * total (the payment's allocated amount) belongs to the module that owns the payment.
 *
 * With a null generation context the cross-model resolution falls back to the naming-convention
 * defaults, which is deterministic and enough to assert the emitted coordinates.
 */
class GlueRollupCrossModelTest {

    private static final String YAML = """
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

    @SuppressWarnings("unchecked")
    @Test
    void aCrossModelParentResolvesToTheOwnerModelsPackage() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> rollups = GlueIntentGenerator.buildRollupsForTest(model);

        // create + update + delete variants of the same roll-up.
        assertTrue(rollups.size() >= 1, "a cross-model roll-up must be emitted, got: " + rollups.size());
        Map<String, Object> first = rollups.get(0);
        assertEquals("DayAllocation", first.get("childEntity"));
        assertEquals("Project", first.get("parentEntity"));
        assertEquals("Project", first.get("fkProperty"));
        assertEquals("ActualHours", first.get("countField"));
        assertEquals("sum", first.get("op"));
        assertEquals("Hours", first.get("sumField"));
        // The parent is reached through `uses: projects`, so the handler must import from THAT model's
        // generated package - an unset parentModel would silently emit the local one and fail to compile.
        assertEquals(Boolean.TRUE, first.get("parentCrossModel"));
        assertEquals("projects", first.get("parentModel"));
    }

    @Test
    void aLocalParentCarriesNoCrossModelCoordinates() {
        String local = """
                name: library
                entities:
                  - name: Member
                    fields:
                      - { name: id,        type: integer, primaryKey: true, generated: true }
                      - { name: loanCount, type: integer }
                  - name: Loan
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Member, kind: manyToOne, to: Member }
                rollups:
                  - { name: memberLoanCount, entity: Loan, via: Member, field: loanCount }
                """;
        Map<String, Object> first = GlueIntentGenerator.buildRollupsForTest(IntentParser.parse(local))
                                                       .get(0);
        assertEquals(Boolean.FALSE, first.get("parentCrossModel"));
        assertEquals("", first.get("parentModel"), "a local parent must leave the model empty so the local gen folder is used");
    }

    @Test
    void capacityBalanceAndStatusStayLocalOnly() {
        // These stamp a capacity guard that reads the parent's table and reference the parent's own
        // status seeds; supporting them across models is a deeper change than resolving coordinates, so
        // they are rejected with a message that says where to put them instead.
        String withCapacity = YAML.replace("op: sum, of: hours }", "op: sum, of: hours, capacity: budgetHours, balance: remainingHours }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(withCapacity));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("capacity / balance / status are not supported")),
                "expected a cross-model capacity issue, got: " + ex.getIssues());
    }

    @Test
    void aParentModelThatIsNotDeclaredInUsesIsRejected() {
        String undeclared = YAML.replace("uses:\n  - { model: projects }\n", "");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(undeclared));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("projects")),
                "an undeclared parent model must be reported, got: " + ex.getIssues());
    }

    /** The accounts-receivable driver: the allocation rows are owned by the invoices model. */
    private static final String CROSS_MODEL_CHILD = """
            name: customer-payments
            uses:
              - { model: sales-invoices }
            entities:
              - name: CustomerPayment
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: amount,    type: decimal }
                  - { name: allocated, type: decimal }
            rollups:
              - { name: paymentAllocated, entity: SalesInvoiceCustomerPayment, model: sales-invoices,
                  parent: CustomerPayment, via: CustomerPayment, field: allocated, op: sum, of: amount }
            """;

    @Test
    void aCrossModelChildBindsTheOwnerProjectsTopicAndPackage() {
        List<Map<String, Object>> rollups = GlueIntentGenerator.buildRollupsForTest(IntentParser.parse(CROSS_MODEL_CHILD));
        // create + update + delete + rekey, as for a local child.
        assertEquals(4, rollups.size(), "a cross-model child roll-up must bind every child event, got: " + rollups.size());
        Map<String, Object> first = rollups.get(0);
        assertEquals("SalesInvoiceCustomerPayment", first.get("childEntity"));
        assertEquals(Boolean.TRUE, first.get("childCrossModel"));
        assertEquals("sales-invoices", first.get("childModel"));
        // The topic and the imports must name the OWNER's project / gen folder - this project publishes
        // nothing about that entity, so a local topic would subscribe to silence.
        assertEquals("sales-invoices", first.get("childProject"));
        assertEquals("SalesInvoiceCustomerPayment", first.get("childPerspective"));
        // The parent is LOCAL, so it carries no cross-model coordinates and is written through this
        // project's own repository.
        assertEquals("CustomerPayment", first.get("parentEntity"));
        assertEquals(Boolean.FALSE, first.get("parentCrossModel"));
        assertEquals("", first.get("parentModel"));
        assertEquals("CustomerPayment", first.get("fkProperty"));
        assertEquals("Allocated", first.get("countField"));
        assertEquals("sum", first.get("op"));
        assertEquals("Amount", first.get("sumField"));
        // The class name is qualified by the owner model: a local child of the same name rolling up
        // through the same relation is a DIFFERENT handler, and one name for both would have the
        // pipeline write one file over the other.
        assertEquals("SalesInvoicesSalesInvoiceCustomerPaymentCustomerPaymentRollupOnCreate", first.get("className"));
    }

    @Test
    void aLocalChildCarriesNoCrossModelChildCoordinates() {
        Map<String, Object> first = GlueIntentGenerator.buildRollupsForTest(IntentParser.parse(YAML))
                                                       .get(0);
        assertEquals(Boolean.FALSE, first.get("childCrossModel"));
        assertEquals("", first.get("childModel"), "a local child must leave the model empty so the local gen folder and topic are used");
        assertEquals("", first.get("childProject"));
    }

    @Test
    void aCrossModelChildWithoutALocalParentIsRejected() {
        // `via` cannot point anywhere resolvable here (the foreign child's relations are not in this
        // document), so the parent has to be named - and it has to be local.
        String noParent = CROSS_MODEL_CHILD.replace("parent: CustomerPayment, ", "");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(noParent));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("must declare parent")),
                "expected a missing-parent issue, got: " + ex.getIssues());

        String foreignParent = CROSS_MODEL_CHILD.replace("parent: CustomerPayment,", "parent: SalesInvoice,");
        IntentValidationException other = assertThrows(IntentValidationException.class, () -> IntentParser.parse(foreignParent));
        assertTrue(other.getIssues()
                        .stream()
                        .anyMatch(i -> i.contains("must be local")),
                "expected a non-local parent issue, got: " + other.getIssues());
    }

    @Test
    void aChildModelThatIsNotDeclaredInUsesIsRejected() {
        String undeclared = CROSS_MODEL_CHILD.replace("uses:\n  - { model: sales-invoices }\n", "");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(undeclared));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("not a declared uses: alias")),
                "an undeclared child model must be reported, got: " + ex.getIssues());
    }

    @Test
    void aForeignChildKeepsTheBalanceAndSaysTheGuardIsNotInstalled() {
        // The driver's second figure: unapplied = amount - allocated. Both the balance and the status are
        // writes on the LOCAL parent, so they are maintained - what a foreign child cannot carry is the
        // overdraw GUARD (it lives in the child's own repository, which the owner model generates), and
        // that is reported rather than left to look like an enforced limit.
        String withCapacity = """
                name: customer-payments
                uses:
                  - { model: sales-invoices }
                entities:
                  - name: CustomerPayment
                    fields:
                      - { name: id,        type: integer, primaryKey: true, generated: true }
                      - { name: amount,    type: decimal }
                      - { name: allocated, type: decimal }
                      - { name: unapplied, type: decimal }
                rollups:
                  - { name: paymentAllocated, entity: SalesInvoiceCustomerPayment, model: sales-invoices,
                      parent: CustomerPayment, via: CustomerPayment, field: allocated, op: sum, of: amount,
                      capacity: amount, balance: unapplied }
                """;
        Map<String, Object> first = GlueIntentGenerator.buildRollupsForTest(IntentParser.parse(withCapacity))
                                                       .get(0);
        assertEquals("Amount", first.get("capacityField"));
        assertEquals("Unapplied", first.get("balanceField"));
    }

    @Test
    void aForeignChildsCapacityStatusMustBeARelationOfTheLocalParent() {
        String badStatus = CROSS_MODEL_CHILD.replace("op: sum, of: amount }", "op: sum, of: amount, capacity: amount, status: Nope }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(badStatus));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("status [Nope] is not a to-one relation of [CustomerPayment]")),
                "the capacity extras must be checked against the LOCAL parent, got: " + ex.getIssues());
    }

    @Test
    void aParentOnALocalRollupIsRejected() {
        // Declaring it there would be a second, silently ignored, statement of what `via` already says -
        // and if the two disagreed nothing would say so.
        String localWithParent = """
                name: library
                entities:
                  - name: Member
                    fields:
                      - { name: id,        type: integer, primaryKey: true, generated: true }
                      - { name: loanCount, type: integer }
                  - name: Loan
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Member, kind: manyToOne, to: Member }
                rollups:
                  - { name: memberLoanCount, entity: Loan, via: Member, field: loanCount, parent: Member }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(localWithParent));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("belongs to a cross-model child only")),
                "expected a stray-parent issue, got: " + ex.getIssues());
    }

    /**
     * The owner model as it is actually generated: the allocation entity with its two FK properties and
     * the amount. Only what {@code CrossModelSupport.readTarget} reads is present.
     */
    private static final String OWNER_MODEL = """
            {
              "model": {
                "entities": [
                  {
                    "name": "SalesInvoiceCustomerPayment",
                    "perspectiveName": "SalesInvoice",
                    "dataName": "SALES_INVOICES_SALES_INVOICE_CUSTOMER_PAYMENT",
                    "properties": [
                      { "name": "Id", "dataName": "ID", "dataType": "INTEGER", "dataPrimaryKey": "true" },
                      { "name": "SalesInvoice", "dataName": "SALES_INVOICE", "widgetType": "DROPDOWN",
                        "relationshipEntityName": "SalesInvoice" },
                      { "name": "CustomerPayment", "dataName": "CUSTOMER_PAYMENT", "widgetType": "DROPDOWN",
                        "relationshipEntityName": "CustomerPayment" },
                      { "name": "Amount", "dataName": "AMOUNT", "dataType": "DECIMAL" }
                    ]
                  }
                ]
              }
            }
            """;

    /**
     * A context whose repository serves {@link #OWNER_MODEL} as the sibling project's generated model.
     */
    private static IntentGenerationContext contextWithOwnerModel(IntentModel model) {
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(missing.exists()).thenReturn(false);
        IResource owner = mock(IResource.class);
        when(owner.exists()).thenReturn(true);
        when(owner.getContent()).thenReturn(OWNER_MODEL.getBytes(StandardCharsets.UTF_8));
        when(repository.getResource(anyString())).thenReturn(missing);
        when(repository.getResource("/users/admin/workspace/sales-invoices/sales-invoices.model")).thenReturn(owner);
        return TestContexts.context(model, repository, "/users/admin/workspace/customer-payments", "app");
    }

    @Test
    void aResolvedOwnerModelSuppliesTheChildsPerspective() {
        IntentGenerationContext context = contextWithOwnerModel(IntentParser.parse(CROSS_MODEL_CHILD));
        Map<String, Object> first = GlueIntentGenerator.buildRollupsForTest(context.getModel(), context)
                                                       .get(0);
        // The allocation is a composition child of the invoice, so it lives under the INVOICE's
        // perspective - which only the owner's model knows. Guessing the entity name here would import a
        // package that does not exist and fail the whole client-Java batch.
        assertEquals("SalesInvoice", first.get("childPerspective"));
        assertTrue(context.getIssues()
                          .isEmpty(),
                "a fully resolvable roll-up must report nothing: " + context.getIssues());
    }

    @Test
    void aViaThatPointsAtAnotherEntityIsDroppedLoudly() {
        // The invoice FK exists on the foreign child - it is simply not this parent's. Summing its rows
        // and looking the ids up as CustomerPayments would produce wrong totals with nothing saying so.
        IntentGenerationContext context =
                contextWithOwnerModel(IntentParser.parse(CROSS_MODEL_CHILD.replace("via: CustomerPayment,", "via: SalesInvoice,")));
        assertTrue(GlueIntentGenerator.buildRollupsForTest(context.getModel(), context)
                                      .isEmpty(),
                "a roll-up keyed on the wrong relation must not be generated");
        assertTrue(context.getIssues()
                          .stream()
                          .anyMatch(i -> i.contains("references [SalesInvoice], not the parent [CustomerPayment]")),
                "the drop must name both entities, got: " + context.getIssues());
    }

    @Test
    void anOfFieldTheOwnerDoesNotDeclareIsDroppedLoudly() {
        IntentGenerationContext context =
                contextWithOwnerModel(IntentParser.parse(CROSS_MODEL_CHILD.replace("of: amount }", "of: netAmount }")));
        assertTrue(GlueIntentGenerator.buildRollupsForTest(context.getModel(), context)
                                      .isEmpty(),
                "a roll-up summing a field the owner does not declare must not be generated");
        assertTrue(context.getIssues()
                          .stream()
                          .anyMatch(i -> i.contains("of [netAmount]")),
                "the drop must name the unresolvable field, got: " + context.getIssues());
    }
}
