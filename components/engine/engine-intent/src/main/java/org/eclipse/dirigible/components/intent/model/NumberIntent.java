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
 * First-class document numbering for a string field: the platform maintains a gap-free counter per
 * {@link #series} (partitioned by {@link #scope}) and stamps the formatted number onto the field,
 * either at insert ({@code stampOn: create}) or at a modeled issue step ({@code stampOn: issue}, a
 * placeholder holds the slot until then; the stamp is idempotent so a re-issue keeps the number).
 * Replaces the hand-written placeholder action + {@code generateNumber} service-task delegate.
 */
public class NumberIntent {

    /**
     * The counter identity. Documents that must share one running sequence (e.g. invoices + credit +
     * debit notes) name the <b>same</b> series. Mandatory.
     */
    private String series;

    /**
     * Format template over {@code {seq}} (zero-pad via {@code {seq:07}}) plus scope tokens
     * ({@code {year}}, {@code {<Field>}}). Optional; defaults to {@code "{series}-{seq:06}"}.
     */
    private String format;

    /**
     * The counter is partitioned by these - a sibling field name of the same entity and/or the literal
     * {@code year}; each distinct combination has its own running counter. Empty → one counter per
     * series.
     */
    private List<String> scope = new ArrayList<>();

    /**
     * When set to {@code year}, the counter restarts as the year scope rolls over. Requires
     * {@code year} in {@link #scope}. Optional.
     */
    private String resetOn;

    /**
     * When the number is stamped: {@code create} (numbered immediately on insert) or {@code issue} (a
     * placeholder holds the slot; the real number is stamped at the modeled issue transition, and the
     * stamp is idempotent). Optional; defaults to {@code create}.
     */
    private String stampOn;

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public String getResetOn() {
        return resetOn;
    }

    public void setResetOn(String resetOn) {
        this.resetOn = resetOn;
    }

    public String getStampOn() {
        return stampOn;
    }

    public void setStampOn(String stampOn) {
        this.stampOn = stampOn;
    }
}
