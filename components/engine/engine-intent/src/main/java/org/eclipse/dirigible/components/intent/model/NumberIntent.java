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

/**
 * First-class document numbering on a string field: the platform stamps a gap-free number onto it,
 * either at insert ({@code stampOn: create}) or at a modeled issue step ({@code stampOn: issue},
 * where a placeholder holds the slot until then and the stamp is idempotent so a re-issue keeps the
 * number).
 *
 * <p>
 * The intent declares only a REFERENCE to a series - never how the number looks. The shape (prefix
 * + total width) belongs to the module's {@code .numbers} artefact and, per tenant, to the Document
 * Numbering settings: baking a format into the model forced a country that wants a different prefix
 * to fork the application and regenerate it. Several fields may reference the SAME series - a sales
 * invoice, a credit note and a debit note sharing one legal number range reference it once each,
 * and the series is defined once.
 */
public class NumberIntent {

    /**
     * The series this field draws from (e.g. {@code Sales Invoice}). Its prefix and width are defined
     * once per module in the {@code .numbers} artefact and are configurable per tenant afterwards.
     * Mandatory.
     */
    private String series;

    /**
     * Optional name of a to-one relation of the same entity whose value PARTITIONS the series: each
     * distinct value gets its own sequence, prefix and width. The canonical use is {@code per: Company}
     * - two legal entities in one tenant each owe their own sequential range, so they must not share a
     * counter. The value never appears IN the number; it only selects which sequence to draw from.
     * Absent = a single sequence for the whole tenant.
     */
    private String per;

    /**
     * When the number is stamped: {@code create} (at insert, by the generated repository) or
     * {@code issue} (at a modeled issue step, by the generated delegate).
     */
    private String stampOn;

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getPer() {
        return per;
    }

    public void setPer(String per) {
        this.per = per;
    }

    public String getStampOn() {
        return stampOn;
    }

    public void setStampOn(String stampOn) {
        this.stampOn = stampOn;
    }
}
