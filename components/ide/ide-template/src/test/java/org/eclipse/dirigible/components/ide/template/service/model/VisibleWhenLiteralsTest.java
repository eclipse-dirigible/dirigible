/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The JavaScript a model's {@code visibleWhen} scalar renders as (dirigible #7502): the field gate
 * a form folds into its {@code x-show}, and the panel terms a detail registration hands the shared
 * panel.
 */
class VisibleWhenLiteralsTest {

    @Test
    void aFieldGateReadsTheFormAndFallsBackToTheDefaultStatus() {
        String expression = VisibleWhenLiterals.formExpression("Status != 1", entity(property("Status", "INTEGER", "1")));
        assertEquals("(form.Status == null || form.Status === '' ? '1' : String(form.Status)) !== '1'", expression,
                "a create form has no status yet - the record it creates starts in the init one, so the field stays hidden");
    }

    @Test
    void termsAreAndedAndEscapedForTheHtmlAttribute() {
        String expression = VisibleWhenLiterals.formExpression("Paid == true && Kind == 'a\"b'",
                entity(property("Paid", "BOOLEAN", null), property("Kind", "VARCHAR", null)));
        assertEquals("(form.Paid == null || form.Paid === '' ? 'false' : String(form.Paid)) === 'true' &amp;&amp; "
                + "(form.Kind == null || form.Kind === '' ? '' : String(form.Kind)) === 'a&quot;b'", expression);
    }

    @Test
    void aQuotedAmpersandPairIsAValueNotATermSeparator() {
        List<VisibleWhenLiterals.Term> terms = VisibleWhenLiterals.terms("Kind == 'R&&D' && Status == 2");
        assertEquals(2, terms.size());
        assertEquals("R&&D", terms.get(0)
                                  .value());
    }

    @Test
    void thePanelTermsAreTheForbidWhenGuardShape() {
        assertEquals("[{ property: 'Status', equal: false, value: '1' }, { property: 'Status', equal: false, value: '3' }]",
                VisibleWhenLiterals.termsLiteral("Status != 1 && Status != 3"));
    }

    @Test
    void aConditionOutsideTheGrammarRendersNoGate() {
        assertNull(VisibleWhenLiterals.termsLiteral("Status >= 2"));
        assertNull(VisibleWhenLiterals.formExpression("Status >= 2", entity()));
        assertNull(VisibleWhenLiterals.termsLiteral(null));
        assertTrue(VisibleWhenLiterals.terms("   ")
                                      .isEmpty());
    }

    @Test
    void theProcessorPutsBothRenderingsOnTheModel() {
        Map<String, Object> status = property("Status", "INTEGER", "1");
        Map<String, Object> paidOn = property("PaidOn", "DATE", null);
        paidOn.put("visibleWhen", "Status == 3");
        Map<String, Object> invoice = entity(status, paidOn);
        invoice.put("name", "Invoice");
        invoice.put("type", "PRIMARY");
        invoice.put("perspectiveName", "Invoices");
        Map<String, Object> payment = entity(property("Id", "INTEGER", null));
        payment.put("name", "InvoicePayment");
        payment.put("type", "DEPENDENT");
        payment.put("perspectiveName", "Invoices");
        payment.put("visibleWhen", "Status != 1");
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("entities", new ArrayList<>(List.of(invoice, payment)));
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("projectName", "sales");
        parameters.put("genFolderName", "sales");

        ModelParameterProcessor.process(model, parameters);

        assertEquals("(form.Status == null || form.Status === '' ? '1' : String(form.Status)) === '3'", paidOn.get("visibleWhenJs"));
        assertNull(status.get("visibleWhenJs"));
        assertEquals("[{ property: 'Status', equal: false, value: '1' }]", payment.get("visibleWhenTermsJs"));
        assertNull(invoice.get("visibleWhenTermsJs"));
    }

    @SafeVarargs
    private static Map<String, Object> entity(Map<String, Object>... properties) {
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("properties", new ArrayList<>(List.of(properties)));
        return entity;
    }

    private static Map<String, Object> property(String name, String dataType, String defaultValue) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", name);
        property.put("dataType", dataType);
        if (defaultValue != null) {
            property.put("dataDefaultValue", defaultValue);
        }
        return property;
    }
}
