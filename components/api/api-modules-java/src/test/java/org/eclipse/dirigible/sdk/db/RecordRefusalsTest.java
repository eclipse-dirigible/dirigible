/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.eclipse.dirigible.sdk.db.GenerateRefusals.Label;
import org.junit.jupiter.api.Test;

/**
 * dirigible #8775: a refusal about a record as a whole - an action from the wrong status, a status
 * move its lifecycle does not allow - is answered in the language of the request, the action, the
 * entity and the statuses named by their translated labels, with the key and the parameters a
 * client renders it again by; a sentence the project's catalog does not translate comes from the
 * platform's.
 */
class RecordRefusalsTest {

    private static final String CATALOG = "invoices:invoices-model";
    private static final Label INVOICE = new Label("Invoice", CATALOG + ".t.INVOICES_INVOICE");
    private static final Label APPROVE = new Label("Approve", CATALOG + ".actions.Approve");

    private static final Map<String, String> PROJECT_BG = Map.of( //
            CATALOG + ".t.INVOICES_INVOICE", "фактура", //
            CATALOG + ".actions.Approve", "Одобри", //
            CATALOG + ".checks.lifecycleMove", "Фактурата не може от {from} в {to}");

    private static final Map<String, String> PLATFORM_BG = Map.of( //
            "application-core:generated.checks.actionStatusNotAllowed",
            "„{action}“ е разрешено само от статус {allowed} - този запис „{entity}“ е в статус {status}", //
            "application-core:generated.checks.lifecycleMove", "Записът „{entity}“ не може да премине от {from} към {to}");

    private static final CheckMessages.CatalogReader CATALOGS = (language, key) -> {
        if (!"bg".equals(language)) {
            return null;
        }
        return key.startsWith(CheckMessages.PLATFORM_CATALOG) ? PLATFORM_BG.get(key) : PROJECT_BG.get(key);
    };

    @Test
    void anActionFromAWrongStatusIsRefusedInTheReadersLanguageFromThePlatformCatalog() {
        ValidationException refusal = RecordRefusals.refusal("bg", CATALOGS, CATALOG, RecordRefusals.STATUS_NOT_ALLOWED_KEY,
                RecordRefusals.STATUS_NOT_ALLOWED_TEXT, null, "action", APPROVE, "allowed", "Чернова, Подадена", "entity", INVOICE,
                "status", "Платена");

        assertEquals("„Одобри“ е разрешено само от статус Чернова, Подадена - този запис „фактура“ е в статус Платена",
                refusal.getMessage());
        assertEquals(CATALOG + ".checks.actionStatusNotAllowed", refusal.getMessageKey());
        assertEquals(Map.of("action", "Одобри", "allowed", "Чернова, Подадена", "entity", "фактура", "status", "Платена"),
                refusal.getMessageParams());
        assertNull(refusal.getField());
    }

    @Test
    void anActionFromAWrongStatusReadsTheDefaultTextWithoutATranslation() {
        ValidationException refusal = RecordRefusals.refusal(null, CATALOGS, CATALOG, RecordRefusals.STATUS_NOT_ALLOWED_KEY,
                RecordRefusals.STATUS_NOT_ALLOWED_TEXT, null, "action", APPROVE, "allowed", "Draft, Submitted", "entity", INVOICE, "status",
                "Paid");

        assertEquals("Approve is allowed only from Draft, Submitted - this Invoice is Paid", refusal.getMessage());
    }

    @Test
    void theProjectsOwnTranslationWinsOverThePlatforms() {
        ValidationException refusal = RecordRefusals.refusal("bg", CATALOGS, CATALOG, RecordRefusals.LIFECYCLE_MOVE_KEY,
                RecordRefusals.LIFECYCLE_MOVE_TEXT, "Status", "entity", INVOICE, "from", "Чернова", "to", "Платена");

        assertEquals("Фактурата не може от Чернова в Платена", refusal.getMessage());
        assertEquals("Status", refusal.getField());
    }

    @Test
    void withoutACatalogTheRefusalKeepsTheDefaultTextAndCarriesNoKey() {
        ValidationException refusal =
                RecordRefusals.refusal("bg", CATALOGS, null, RecordRefusals.SHARING_OFF_KEY, RecordRefusals.SHARING_OFF_TEXT, null);

        assertEquals("Sharing documents through a link is switched off", refusal.getMessage());
        assertNull(refusal.getMessageKey());
    }

    @Test
    void theBodyCarriesTheMessageItsKeyAndFieldAndKeepsTheOlderErrorKey() {
        ValidationException refusal =
                new ValidationException("Записът не може да премине", CATALOG + ".checks.lifecycleMove", Map.of("to", "Платена"), "Status");

        assertEquals(
                Map.of("error", "Записът не може да премине", "message", "Записът не може да премине", "messageKey",
                        CATALOG + ".checks.lifecycleMove", "messageParams", Map.of("to", "Платена"), "field", "Status"),
                RecordRefusals.body(refusal));
    }

    @Test
    void aCheckKeyHasAPlatformTwinAndALabelKeyHasNone() {
        assertEquals("application-core:generated.checks.lifecycleMove", CheckMessages.platformKey(CATALOG + ".checks.lifecycleMove"));
        assertNull(CheckMessages.platformKey(CATALOG + ".t.INVOICES_INVOICE"));
    }
}
