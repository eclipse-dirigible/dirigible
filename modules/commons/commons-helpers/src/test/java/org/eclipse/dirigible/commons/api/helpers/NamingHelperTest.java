/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.api.helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the naming rules shared by the code-generation paths.
 */
class NamingHelperTest {

    @Test
    void humanizeIdentifierSplitsOnCaseBoundaries() {
        assertEquals("Tax Event Date", NamingHelper.humanizeIdentifier("TaxEventDate"));
        assertEquals("Librarian Review", NamingHelper.humanizeIdentifier("librarianReview"));
        assertEquals("Id", NamingHelper.humanizeIdentifier("Id"));
        assertEquals("", NamingHelper.humanizeIdentifier(null));
        assertEquals("", NamingHelper.humanizeIdentifier(""));
    }

    @Test
    void humanizeIdentifierKeepsConsecutiveCapitalsTogether() {
        assertEquals("VATRate", NamingHelper.humanizeIdentifier("VATRate"));
    }

    @Test
    void humanizeIdentifierAppliesTheAcronymOverride() {
        assertEquals("Unit of Measure", NamingHelper.humanizeIdentifier("UoM"));
        assertEquals("Unit of Measure", NamingHelper.humanizeIdentifier("uom"));
    }

    /**
     * The distinction the model-generation pipeline depends on: only the name form treats separators as
     * word boundaries. Collapsing the two would relabel every generated artefact whose property names
     * contain a separator.
     */
    @Test
    void onlyTheNameFormTreatsSeparatorsAsWordBoundaries() {
        assertEquals("Payment_method", NamingHelper.humanizeIdentifier("payment_method"));
        assertEquals("Payment Method", NamingHelper.humanizeName("payment_method"));
        assertEquals("Sales Invoices", NamingHelper.humanizeName("sales-invoices"));
        assertEquals("Sales-invoices", NamingHelper.humanizeIdentifier("sales-invoices"));
    }

    @Test
    void humanizeNameFallsBackToTheIdentifierFormWithoutSeparators() {
        assertEquals("Loan Approval", NamingHelper.humanizeName("LoanApproval"));
    }

    @Test
    void pluralizeLabelAppliesTheEnglishRules() {
        assertEquals("Books", NamingHelper.pluralizeLabel("Book"));
        assertEquals("Sales Invoices", NamingHelper.pluralizeLabel("Sales Invoice"));
        assertEquals("Countries", NamingHelper.pluralizeLabel("Country"));
        assertEquals("Days", NamingHelper.pluralizeLabel("Day"));
        assertEquals("Addresses", NamingHelper.pluralizeLabel("Address"));
        assertEquals("Boxes", NamingHelper.pluralizeLabel("Box"));
        assertEquals("Batches", NamingHelper.pluralizeLabel("Batch"));
        assertEquals("Dishes", NamingHelper.pluralizeLabel("Dish"));
        assertEquals("", NamingHelper.pluralizeLabel(null));
    }

    @Test
    void pluralizeLabelAppliesTheIrregularOverrides() {
        assertEquals("Units of Measure", NamingHelper.pluralizeLabel("Unit of Measure"));
        assertEquals("Units of Measure", NamingHelper.pluralizeLabel("UoM"));
    }

    @Test
    void sanitizeJavaIdentifierLowercasesAndReplacesEverythingElse() {
        assertEquals("sales_order", NamingHelper.sanitizeJavaIdentifier("sales-order"));
        assertEquals("sales_orders", NamingHelper.sanitizeJavaIdentifier("Sales Orders"));
        assertEquals("uom", NamingHelper.sanitizeJavaIdentifier("UoM"));
        assertEquals("_", NamingHelper.sanitizeJavaIdentifier(null));
        assertEquals("_", NamingHelper.sanitizeJavaIdentifier(""));
    }

    @Test
    void sanitizeJavaIdentifierPrefixesALeadingDigit() {
        assertEquals("_2024_report", NamingHelper.sanitizeJavaIdentifier("2024-report"));
    }

}
