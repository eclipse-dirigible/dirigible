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

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

class IntentNamingTest {

    @Test
    void javaModuleMirrorsTheTemplateEngineSanitizer() {
        // Must produce exactly what parameterUtils.js sanitizeJavaIdentifier produces for the same
        // name - the BPMN handler FQNs / endpoint URLs (producer) and the events template's package
        // line (consumer) only match if both sides sanitize identically.
        assertEquals("sales_invoices", IntentNaming.javaModule(named("sales-invoices")));
        assertEquals("numbers", IntentNaming.javaModule(named("numbers")));
        assertEquals("my_app_2", IntentNaming.javaModule(named("My App.2")));
        assertEquals("_7wonders", IntentNaming.javaModule(named("7wonders")));
    }

    @Test
    void eventsPackageIsModuleScoped() {
        // gen.events.<module>, NOT gen.<module>.events: the module segment goes UNDER gen/events so
        // the glue output stays a sibling of gen/<module> and survives a model-only regeneration.
        assertEquals("gen.events.sales_invoices", IntentNaming.eventsPackage(named("sales-invoices")));
    }

    private static IntentGenerationContext named(String name) {
        IntentModel model = new IntentModel();
        model.setName(name);
        return TestContexts.context(model);
    }

    @Test
    void upperSnakeKeepsCamelCaseBoundaries() {
        assertEquals("LOANED_ON", IntentNaming.upperSnake("loanedOn"));
        assertEquals("ID", IntentNaming.upperSnake("id"));
        assertEquals("UO_M", IntentNaming.upperSnake("UoM"));
        assertEquals("CUSTOMER", IntentNaming.upperSnake("Customer"));
    }

    @Test
    void upperSnakeCollapsesSeparatorsToUnderscore() {
        // A kebab-case intent/project name must become a VALID SQL identifier: SALES_INVOICES, not the
        // invalid SALES-INVOICES (an unquoted hyphen is parsed as minus and breaks table creation).
        assertEquals("SALES_INVOICES", IntentNaming.upperSnake("sales-invoices"));
        assertEquals("CUSTOMER_PAYMENTS", IntentNaming.upperSnake("customer-payments"));
        assertEquals("A_B_C", IntentNaming.upperSnake("a.b/c"));
        assertEquals("MY_APP", IntentNaming.upperSnake("my app"));
    }

    @Test
    void upperSnakeDoesNotLeaveDanglingOrDoubledUnderscores() {
        assertEquals("A_B", IntentNaming.upperSnake("a--b"));
        assertEquals("AB", IntentNaming.upperSnake("ab-"));
        assertEquals("AB", IntentNaming.upperSnake("-ab"));
        assertEquals("", IntentNaming.upperSnake("---"));
    }
}
