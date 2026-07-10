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

import java.util.List;

/**
 * A declarative validation on an {@link EntityIntent} - the cross-field / cross-line rules a plain
 * {@code required}/{@code unique} cannot express. Three kinds:
 * <ul>
 * <li>{@code exactlyOne} (row-level): exactly one of {@link #fields} is non-null on the record (a
 * journal line is either debit or credit) - enforced on every user write;</li>
 * <li>{@code itemsSumEqual} (document-level): the sums of the two {@link #over} fields across the
 * document's composition items are equal (the double-entry invariant) - enforced when the document
 * is persisted carrying the {@link #status} gate seed id, i.e. at the workflow transition;</li>
 * <li>{@code itemsMin} (document-level): the document has at least {@link #count} items - same
 * gate.</li>
 * </ul>
 */
public class CheckIntent {

    private String kind;
    /** {@code exactlyOne}: the record's own fields, exactly one of which must be non-null. */
    private List<String> fields;
    /** {@code itemsSumEqual}: the two numeric item fields whose sums must be equal. */
    private List<String> over;
    /** {@code itemsMin}: the minimum number of items. */
    private Integer count;
    /**
     * Document-level checks only: the EntityStatus seed id gating the check - it runs when the document
     * is persisted carrying this status (the workflow transition into e.g. POSTED), so drafting
     * item-by-item stays unconstrained.
     */
    private Integer status;
    /** The user-facing message when the check fails. */
    private String message;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public List<String> getOver() {
        return over;
    }

    public void setOver(List<String> over) {
        this.over = over;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
