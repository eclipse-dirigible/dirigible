/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A document capture: a user uploads a scanned or photographed document - a fiscal receipt, a
 * supplier's invoice - and a vision model reads it into a NEW record of {@link #into}, which a
 * person then reviews. The image-to-fields sibling of a file import.
 *
 * <pre>
 * captures:
 *   - name: supplierInvoiceScan
 *     into: PurchaseInvoice
 *     label: Scan a supplier invoice           # optional: the caption of the button; defaults to the name
 *     attachment: PurchaseInvoiceScan          # optional: the function: Attachment child the file is kept in
 *     instructions: "The buyer is our company; the supplier is the other party."
 *     qr: { separator: "*", parts: [device, receipt, date, time, total] }
 *     fields:
 *       - { field: supplierNumber, describe: "the supplier's invoice number" }
 *       - { field: date, qr: date }
 *       - { field: Supplier, match: [registrationNumber, vatNumber], describe: "the seller" }
 *       - { field: total, compare: true }
 *     items:
 *       into: PurchaseInvoiceItem
 *       fields:
 *         - { field: description }
 *         - { field: quantity }
 *         - { field: price }
 * </pre>
 *
 * {@link #fields} names what the model must read: a field of {@link #into}, or one of its to-one
 * relations, which is resolved by a natural key ({@code match:}) and left empty when no single row
 * matches. {@link #items} reads the document's lines into a composition child. {@link #qr} decodes
 * a QR code printed on the document first and splits it into named parts a field can be checked
 * against ({@code qr:}); a part is exact, so it wins over what the model read.
 *
 * <p>
 * The record a capture creates is never complete by decree: it is saved in the entity's initial
 * status with whatever was read, and everything a person must look at - a value the model was not
 * sure of, a value missing from the document, a relation nothing matched, a total that differs from
 * the one the entity calculated - is recorded as a flag in the entity's capture ledger, next to the
 * provider, the model and the tokens of the run.
 */
public class CaptureIntent {

    private String name;
    private String into;
    private String label;
    private String attachment;
    private String instructions;
    private CaptureQrIntent qr;
    private List<CaptureFieldIntent> fields = new ArrayList<>();
    private CaptureItemsIntent items;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getInto() {
        return into;
    }

    public void setInto(String into) {
        this.into = into;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public CaptureQrIntent getQr() {
        return qr;
    }

    public void setQr(CaptureQrIntent qr) {
        this.qr = qr;
    }

    public List<CaptureFieldIntent> getFields() {
        return fields;
    }

    public void setFields(List<CaptureFieldIntent> fields) {
        this.fields = fields == null ? new ArrayList<>() : fields;
    }

    public CaptureItemsIntent getItems() {
        return items;
    }

    public void setItems(CaptureItemsIntent items) {
        this.items = items;
    }
}
