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
 * One condition of a {@link ScheduleIntent}'s {@code where} filter: a field, a comparison operator
 * ({@code eq}/{@code ne}/{@code gt}/{@code ge}/{@code lt}/{@code le}/{@code like}) and a value. The
 * value is a literal, or the token {@code CURRENT_DATE} / {@code CURRENT_TIMESTAMP} which the
 * generated job evaluates to "now". Maps to a typed {@code Criteria} condition.
 */
public class ScheduleConditionIntent {

    private String field;
    private String op;
    private Object value;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
