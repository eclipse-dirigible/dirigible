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
 * A banner on the record page (dirigible #8371): a sentence the page shows above the form while a
 * condition holds - on the record itself ({@link #when}), on one of its related rows
 * ({@link #related}), or both - optionally with the action that fixes it.
 *
 * <pre>
 * banners:
 *   - text: "The last shipment failed: {failure}"
 *     variant: negative
 *     related: { entity: Shipment, row: latest, when: "status == FAILED" }
 *     action: RetryShipment
 * </pre>
 *
 * <p>
 * UI-only, like {@code visibleWhen}: a banner decides what the page says, never what the server
 * allows.
 */
public class BannerIntent {

    /**
     * The sentence. {@code {field}} placeholders name the subject row's own fields - the matched
     * related row, or the record. Required.
     */
    private String text;

    /** {@code information} (the default), {@code warning} or {@code negative}. */
    private String variant;

    /**
     * The condition over the record's own fields and to-one relations, in the {@code visibleWhen}
     * grammar. Optional when {@link #related} is given; when both are, both must hold.
     */
    private Object when;

    /** The related rows the banner reads. Optional when {@link #when} is given. */
    private BannerRelatedIntent related;

    /**
     * An action, transition or generates of the subject's entity, offered in the banner as a button
     * while the subject's status guard and roles allow it. Optional.
     */
    private String action;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    /** The variant as the page renders it: the authored one, lower-cased, else {@code information}. */
    public String variantOrDefault() {
        return variant == null || variant.isBlank() ? "information"
                : variant.trim()
                         .toLowerCase(java.util.Locale.ROOT);
    }

    public Object getWhen() {
        return when;
    }

    public void setWhen(Object when) {
        this.when = when;
    }

    public BannerRelatedIntent getRelated() {
        return related;
    }

    public void setRelated(BannerRelatedIntent related) {
        this.related = related;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
