/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package custom;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.eclipse.dirigible.sdk.component.Component;
import org.eclipse.dirigible.sdk.db.CalculatedField;

import gen.money.data.invoiceline.InvoiceLineEntity;

/**
 * The VAT of an invoice line: quantity x unit price x rate, rounded half-up to the cent. Computed in
 * {@link BigDecimal} from start to finish - a {@code double} evaluation turns {@code 1 x 2.90 @ 5%}
 * into {@code 0.14499999...} and rounds it to 0.14 instead of 0.15, which is exactly what
 * {@code custom/test/LineVatActionTest} pins.
 */
@Component
public class LineVatAction implements CalculatedField<InvoiceLineEntity, BigDecimal> {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int CENTS = 2;

    @Override
    public BigDecimal calculate(InvoiceLineEntity line) {
        // The repository calculates before it validates, so a line missing an input reaches here; the
        // validation that follows refuses it with the missing field's name.
        if (line.Quantity == null || line.UnitPrice == null || line.VatRate == null) {
            return null;
        }
        return line.Quantity.multiply(line.UnitPrice)
                            .multiply(line.VatRate)
                            .divide(HUNDRED)
                            .setScale(CENTS, RoundingMode.HALF_UP);
    }
}
