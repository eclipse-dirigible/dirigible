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
 * One input of a {@code generates} action's {@code prompt:} - a declared input form shown before
 * the target record is created (issue #6685). Each entry names a property of the <b>target</b>
 * entity ({@code GeneratesIntent#getTo()}), so the control is typed from the target's own field /
 * relation definition and the target's {@code dependsOn:} declarations apply unchanged in the
 * dialog. The prompted value is posted to the generated controller together with the source id and
 * set on the target after {@code map:} / {@code defaults:}. It reaches a post-issue child record (a
 * manual payment allocation on an immutable invoice) as well, since an action button is not gated
 * on mutability the way the document's own panels are.
 */
public class PromptFieldIntent {

    /** The target entity's authored field or to-one relation name. */
    private String field;

    /**
     * Whether the input must be provided; enforced in the dialog and again by the generated controller
     * (400 when missing). Defaults to {@code false}.
     */
    private boolean required;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}
