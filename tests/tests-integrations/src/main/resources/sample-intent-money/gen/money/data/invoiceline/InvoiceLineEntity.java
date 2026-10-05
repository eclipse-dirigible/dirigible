/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package gen.money.data.invoiceline;

import org.eclipse.dirigible.sdk.db.Column;
import org.eclipse.dirigible.sdk.db.CreatedAt;
import org.eclipse.dirigible.sdk.db.CreatedBy;
import org.eclipse.dirigible.sdk.platform.Documentation;
import org.eclipse.dirigible.sdk.db.Entity;
import org.eclipse.dirigible.sdk.db.GeneratedValue;
import org.eclipse.dirigible.sdk.db.GenerationType;
import org.eclipse.dirigible.sdk.db.Id;
import org.eclipse.dirigible.sdk.db.Table;
import org.eclipse.dirigible.sdk.db.UpdatedAt;
import org.eclipse.dirigible.sdk.db.UpdatedBy;

@Entity(name = "money_InvoiceLineEntity")
@Table(name = "MONEY_INVOICE_LINE")
@Documentation("InvoiceLine entity mapping")
public class InvoiceLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INVOICE_LINE_ID", length = 20)
    @Documentation("Id")
    public Integer Id;

    @Column(name = "INVOICE_LINE_DESCRIPTION", length = 200, nullable = false)
    @Documentation("Description")
    public String Description;

    @Column(name = "INVOICE_LINE_QUANTITY", precision = 18, scale = 3, nullable = false)
    @Documentation("Quantity")
    public java.math.BigDecimal Quantity;

    @Column(name = "INVOICE_LINE_UNIT_PRICE", precision = 18, scale = 2, nullable = false)
    @Documentation("UnitPrice")
    public java.math.BigDecimal UnitPrice;

    @Column(name = "INVOICE_LINE_VAT_RATE", precision = 5, scale = 2, nullable = false)
    @Documentation("VatRate")
    public java.math.BigDecimal VatRate;

    @Column(name = "INVOICE_LINE_VAT_AMOUNT", precision = 18, scale = 2, nullable = true)
    @Documentation("VatAmount")
    public java.math.BigDecimal VatAmount;

}
