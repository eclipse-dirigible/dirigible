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

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * What a task's row says it is ABOUT (issue #7077). The subject is derived from what the entity
 * already declares - the property records are labeled by, the first major to-one, the aggregated
 * total - so the Inbox stops reading {@code Ref 6}, the record's primary key.
 */
class TaskSubjectSupportTest {

    private static final String DOCUMENT = """
            name: billing
            entities:
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: SalesInvoiceStatus
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
                  - { name: issuedOn, type: date }
                  - { name: total, type: decimal, aggregate: true }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer }
                  - { name: status, kind: manyToOne, to: SalesInvoiceStatus, function: EntityStatus }
              - name: SalesInvoiceItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: salesInvoice, kind: manyToOne, to: SalesInvoice, composition: true }
            processes:
              - name: SalesInvoiceApproval
                trigger: { onCreate: SalesInvoice }
                steps:
                  - { name: approve, kind: userTask, args: { assignee: Manager, next: done } }
                  - { name: done, kind: end }
            """;

    @Test
    void aDocumentIsIdentifiedByItsNumberCounterpartyAndTotal() {
        List<Map<String, Object>> triggers = GlueIntentGenerator.buildTriggersForTest(IntentParser.parse(DOCUMENT));

        assertEquals(1, triggers.size());
        assertEquals("Number:text,Customer:relation,Total:number", triggers.get(0)
                                                                           .get("subjectFields"));
    }

    /** The status badge is never the counterparty - it says nothing about WHICH record this is. */
    @Test
    void theStatusRelationIsNotTheParty() {
        IntentModel model = IntentParser.parse("""
                name: requests
                entities:
                  - name: RequestStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Employee
                    identity: email
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: email, type: string, unique: true }
                  - name: LeaveRequest
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: fromDate, type: date }
                      - { name: days, type: integer }
                    relations:
                      - { name: status, kind: manyToOne, to: RequestStatus, function: EntityStatus }
                      - { name: employee, kind: manyToOne, to: Employee, personal: true }
                processes:
                  - name: LeaveApproval
                    trigger: { onCreate: LeaveRequest }
                    steps:
                      - { name: approve, kind: userTask, args: { assignee: Manager, next: done } }
                      - { name: done, kind: end }
                """);

        // No label property of its own, so the subject is the owner plus the record's leading list
        // columns - still the answer to "which request is this", which the bare id never was.
        assertEquals("Employee:relation,FromDate:date,Days:integer", GlueIntentGenerator.buildTriggersForTest(model)
                                                                                        .get(0)
                                                                                        .get("subjectFields"));
    }

    /**
     * An entity with no {@code name} field is labeled by the stored {@code Name} its {@code label:}
     * expression keeps - the same property a relation to it would show. This is the shape
     * {@code IntentEmissionCoverageIT} pins on the generated trigger.
     */
    @Test
    void aLabelExpressionIsTheRecordsName() {
        IntentModel model = IntentParser.parse("""
                name: emission
                entities:
                  - name: Person
                    identity: email
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: email, type: string, unique: true }
                  - name: Claim
                    label: "{note} ({Person.name})"
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: note, type: string, length: 200 }
                      - { name: rate, type: decimal, sensitive: true }
                      - { name: bonus, type: decimal, visibleTo: [Payroll] }
                    relations:
                      - { name: Person, kind: manyToOne, to: Person, required: true, personal: true }
                permissions:
                  - { role: Payroll, can: [Claim:read] }
                processes:
                  - name: ClaimConfirm
                    trigger: { onCreate: Claim }
                    steps:
                      - { name: confirm, kind: userTask, args: { assignee: personal, next: done } }
                      - { name: done, kind: end }
                """);

        assertEquals("Name:text,Person:relation,Note:text", GlueIntentGenerator.buildTriggersForTest(model)
                                                                               .get(0)
                                                                               .get("subjectFields"));
    }

    /** A role-restricted value is never part of a line read by whoever happens to hold the task. */
    @Test
    void aSensitiveFieldIsNeverPartOfTheSubject() {
        IntentModel model = IntentParser.parse("""
                name: payroll
                entities:
                  - name: Payslip
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: period, type: string }
                      - { name: netPay, type: decimal, sensitive: true }
                      - { name: costCentre, type: string }
                processes:
                  - name: PayslipRelease
                    trigger: { onCreate: Payslip }
                    steps:
                      - { name: release, kind: userTask, args: { assignee: Payroll, next: done } }
                      - { name: done, kind: end }
                """);

        assertEquals("Period:text,CostCentre:text", GlueIntentGenerator.buildTriggersForTest(model)
                                                                       .get(0)
                                                                       .get("subjectFields"));
    }
}
