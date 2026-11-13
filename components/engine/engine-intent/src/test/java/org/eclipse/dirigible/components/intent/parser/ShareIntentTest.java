/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ShareIntent;
import org.junit.jupiter.api.Test;

/**
 * An entity's {@code share:} (dirigible #8613): the issued copy of its records may be handed to a
 * reader without an account. Everything that decides what is served and who may hand it out is
 * declared, and the parser refuses the block when any of it is missing.
 */
class ShareIntentTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: InvoiceStatus
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Employee
                identity: email
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string, required: true, unique: true, length: 320 }
              - name: SalesInvoice
                share:
                  when: "status == ISSUED"
                  copy: SalesInvoiceCopy
                  roles: [Accountant]
                  expiresAfterDays: 30
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: total, type: decimal }
                relations:
                  - { name: status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus }
                  - { name: issuer, kind: manyToOne, to: Employee }
              - name: SalesInvoiceItem
                function: DocumentItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
              - name: SalesInvoiceCopy
                function: Snapshot
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
              - name: Memo
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: text, type: string }
            permissions:
              - { role: Accountant }
            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: ISSUED }
                  - { id: 3, name: VOIDED }
            """;

    @Test
    void aCompleteDeclarationParsesAndResolvesItsStatusName() {
        ShareIntent share = invoice(IntentParser.parse(YAML)).getShare();
        assertEquals("status == 2", share.getWhen());
        assertEquals("SalesInvoiceCopy", share.getCopy());
        assertEquals(List.of("Accountant"), share.getRoles());
        assertEquals(30, share.expiresAfterDaysOrNull());
    }

    @Test
    void aLinkIsPermanentUnlessALifeIsDeclared() {
        ShareIntent share = invoice(IntentParser.parse(YAML.replace("      expiresAfterDays: 30\n", ""))).getShare();
        assertEquals(0, share.expiresAfterDaysOrNull());
    }

    @Test
    void anEmptyRolesListIsLegalAndAdmitsNobody() {
        ShareIntent share = invoice(IntentParser.parse(YAML.replace("roles: [Accountant]", "roles: []"))).getShare();
        assertTrue(share.getRoles()
                        .isEmpty());
    }

    @Test
    void shareWithoutAWhenIsRejected() {
        assertIssue(YAML.replace("      when: \"status == ISSUED\"\n", ""),
                "entity [SalesInvoice] share needs a when - only a record in a declared state can get a link");
    }

    @Test
    void aWhenThatNamesNoPropertyOfTheRecordIsRejected() {
        assertIssue(YAML.replace("status == ISSUED", "phase == 2"),
                "entity [SalesInvoice] share when when [phase == 2] guards [phase], which is not a field or to-one relation of [SalesInvoice]");
    }

    @Test
    void shareWithoutACopyIsRejected() {
        assertIssue(YAML.replace("      copy: SalesInvoiceCopy\n", ""),
                "entity [SalesInvoice] share needs a copy - the `function: Snapshot` child whose stored file a link serves");
    }

    @Test
    void aCopyThatIsNoSnapshotIsRejected() {
        assertIssue(YAML.replace("copy: SalesInvoiceCopy", "copy: SalesInvoiceItem"),
                "entity [SalesInvoice] share copy [SalesInvoiceItem] is not a `function: Snapshot` entity");
        assertIssue(YAML.replace("copy: SalesInvoiceCopy", "copy: Nothing"),
                "entity [SalesInvoice] share copy [Nothing] is not an entity of this model");
    }

    @Test
    void aCopyOfAnotherEntityIsRejected() {
        String yaml = YAML.replace("""
                  - name: Memo
                """, """
                  - name: Memo
                    share: { when: "text == 'x'", copy: SalesInvoiceCopy, roles: [Accountant] }
                """);
        assertIssue(yaml, "entity [Memo] share copy [SalesInvoiceCopy] is not a composition child of [Memo]");
    }

    @Test
    void aCopyNobodyMintsIsRejected() {
        assertIssue(YAML.replace("    function: DocumentItem\n", "")
                        .replace("- name: SalesInvoiceItem", "- name: SalesInvoiceLine"),
                "entity [SalesInvoice] share copy [SalesInvoiceCopy] is never minted");
    }

    @Test
    void shareWithoutRolesIsRejected() {
        assertIssue(YAML.replace("      roles: [Accountant]\n", ""),
                "entity [SalesInvoice] share needs roles - who may create and revoke links");
    }

    @Test
    void aRoleTheIntentDoesNotDeclareIsRejected() {
        assertIssue(YAML.replace("roles: [Accountant]", "roles: [Accountant, Clerk]"),
                "entity [SalesInvoice] share roles names role [Clerk], which the intent does not declare - add it to `permissions`"
                        + " (declared: Accountant)");
    }

    @Test
    void aPersonalEntityIsRejected() {
        assertIssue(
                YAML.replace("{ name: issuer, kind: manyToOne, to: Employee }",
                        "{ name: issuer, kind: manyToOne, to: Employee, required: true, personal: true }"),
                "entity [SalesInvoice] share is not allowed on a personal entity - [SalesInvoice] belongs to the signed-in person through"
                        + " its relation [issuer], and a link is a bearer credential");
    }

    @Test
    void aLifeThatIsNoWholeNumberOfDaysIsRejected() {
        assertIssue(YAML.replace("expiresAfterDays: 30", "expiresAfterDays: -1"),
                "entity [SalesInvoice] share expiresAfterDays [-1] must be a whole number of days from 0 (permanent) to 3650");
        assertIssue(YAML.replace("expiresAfterDays: 30", "expiresAfterDays: soon"),
                "entity [SalesInvoice] share expiresAfterDays [soon] must be a whole number of days");
    }

    @Test
    void anUnknownKeyUnderShareIsRejected() {
        assertIssue(YAML.replace("copy: SalesInvoiceCopy", "copy: SalesInvoiceCopy\n      anyone: true"), "anyone");
    }

    private static EntityIntent invoice(IntentModel model) {
        return model.getEntities()
                    .stream()
                    .filter(entity -> "SalesInvoice".equals(entity.getName()))
                    .findFirst()
                    .orElseThrow();
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException thrown = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(thrown.getIssues()
                         .stream()
                         .anyMatch(issue -> issue.contains(expected)),
                "expected an issue containing [" + expected + "] but got " + thrown.getIssues());
    }
}
