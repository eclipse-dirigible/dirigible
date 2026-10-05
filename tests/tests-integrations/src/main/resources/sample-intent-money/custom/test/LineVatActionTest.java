/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package custom.test;

import static org.eclipse.dirigible.sdk.test.Slice.expectRefused;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.eclipse.dirigible.sdk.test.IntentSlice;
import org.eclipse.dirigible.sdk.test.Slice;
import org.junit.jupiter.api.Test;

import custom.LineVatAction;
import gen.money.data.invoiceline.InvoiceLineEntity;
import gen.money.data.invoiceline.InvoiceLineRepository;

/**
 * The VAT of an invoice line, as the application computes it on a write - through the generated
 * repository, which calls {@link LineVatAction} - not as a test re-implements it. Runs in-process with
 * {@code mvn test}: no platform, no server.
 */
@IntentSlice
class LineVatActionTest {

    private static final String LINE_CREATED = "sample-intent-money-InvoiceLine-InvoiceLine";

    /**
     * {@code 1 x 2.90 x 5%} is exactly 0.145, which rounds half-up to 0.15. In {@code double} the same
     * product is 0.14499999999999999 and rounds to 0.14 - the cent a delegate computing money in
     * {@code double} silently loses.
     */
    @Test
    void roundsTheVatHalfUpToTheCent(Slice slice) {
        InvoiceLineEntity line = slice.given(InvoiceLineEntity.class)
                                      .with("Description", "Paper")
                                      .with("Quantity", 1)
                                      .with("UnitPrice", "2.90")
                                      .with("VatRate", 5)
                                      .saved();

        assertEquals(new BigDecimal("0.15"), line.VatAmount);
    }

    @Test
    void computesTheVatOnTheServerWhateverTheCallerSent(Slice slice) {
        InvoiceLineEntity line = slice.given(InvoiceLineEntity.class)
                                      .with("Description", "Toner")
                                      .with("Quantity", 3)
                                      .with("UnitPrice", "11.50")
                                      .with("VatRate", 20)
                                      .with("VatAmount", "999.99")
                                      .saved();

        assertEquals(new BigDecimal("6.90"), line.VatAmount);
    }

    @Test
    void recalculatesTheVatWhenTheLineChanges(Slice slice, InvoiceLineRepository lines) {
        InvoiceLineEntity line = slice.given(InvoiceLineEntity.class)
                                      .with("Description", "Paper")
                                      .with("Quantity", 1)
                                      .with("UnitPrice", "2.90")
                                      .with("VatRate", 5)
                                      .saved();

        line.Quantity = new BigDecimal("3");
        lines.update(line);

        assertEquals(new BigDecimal("0.44"), lines.findById(line.Id).VatAmount);
    }

    @Test
    void refusesALineWithoutAQuantityAndStoresNothing(Slice slice, InvoiceLineRepository lines) {
        var refusal = expectRefused(() -> slice.given(InvoiceLineEntity.class)
                                               .with("Description", "Paper")
                                               .with("UnitPrice", "2.90")
                                               .with("VatRate", 5)
                                               .saved());

        assertEquals("InvoiceLine.Quantity is required", refusal.getMessage());
        assertEquals(0, lines.count());
    }

    @Test
    void leavesTheVatEmptyWhileAnInputIsMissing(Slice slice) {
        InvoiceLineEntity line = slice.given(InvoiceLineEntity.class)
                                      .with("Quantity", 1)
                                      .entity();

        assertNull(slice.bean(LineVatAction.class)
                        .calculate(line));
    }

    @Test
    void announcesTheCreatedLineWithItsVat(Slice slice) {
        slice.given(InvoiceLineEntity.class)
             .with("Description", "Paper")
             .with("Quantity", 1)
             .with("UnitPrice", "2.90")
             .with("VatRate", 5)
             .saved();

        List<String> announced = slice.sentTo(LINE_CREATED);
        assertEquals(1, announced.size(), "one create event per saved line, got: " + slice.sent());
        assertTrue(announced.get(0)
                            .contains("0.15"),
                "the event carries the stored VAT: " + announced.get(0));
    }
}
