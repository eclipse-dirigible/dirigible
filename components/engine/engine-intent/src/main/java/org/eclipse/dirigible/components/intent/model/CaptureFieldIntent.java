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
 * One thing a {@link CaptureIntent} reads off the document: a field of the captured entity, or one
 * of its to-one relations.
 *
 * <ul>
 * <li>{@link #field} - the field or to-one relation, as authored on the entity.</li>
 * <li>{@link #describe} - what the value is on the document, in the words the model is asked in;
 * defaults to the field's label.</li>
 * <li>{@link #match} - for a relation: the fields of its target the document carries (a
 * registration number, a VAT number, a code). The model reads each, and the first one that
 * identifies exactly one row sets the relation; when none does the relation stays empty and is
 * flagged.</li>
 * <li>{@link #qr} - the named part of the capture's QR code this field must equal. The part is
 * exact: when it is present it is the value, and a different reading is flagged.</li>
 * <li>{@link #compare} - the field is not written: it is one the entity calculates (a total), and
 * what the document prints is compared with what the saved record holds; a difference is
 * flagged.</li>
 * </ul>
 */
public class CaptureFieldIntent {

    private String field;
    private String describe;
    private List<String> match = new ArrayList<>();
    private String qr;
    private boolean compare;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public List<String> getMatch() {
        return match;
    }

    public void setMatch(List<String> match) {
        this.match = match == null ? new ArrayList<>() : match;
    }

    public String getQr() {
        return qr;
    }

    public void setQr(String qr) {
        this.qr = qr;
    }

    public boolean isCompare() {
        return compare;
    }

    public void setCompare(boolean compare) {
        this.compare = compare;
    }
}
