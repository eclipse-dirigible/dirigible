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
 * A business key spanning more than one column of an {@link EntityIntent} - one row per
 * {@code (tenant, application)}, one assignment per {@code (tenant, user)}, one price per
 * {@code (product, priceList, validFrom)}.
 *
 * <p>
 * The field-level {@code unique} covers a single column; the rule that says what a row <em>is</em>
 * across several has no other expression. Left unmodelled it lives in a read-then-write in
 * hand-written code - a race, and one every writer (an import, an inbound message, a scheduled
 * create) has to repeat - or in a constraint added to the generated schema by hand, which the next
 * Generate knows nothing about.
 *
 * <p>
 * {@link #fields} names fields <em>or to-one relations</em>: a relation contributes its foreign-key
 * column, which is what a pair like {@code (tenant, application)} means. The columns are the key;
 * the declared order is how it reads and how it is emitted, and does not change which rows collide.
 * {@link #message} is what a caller is told when a write does.
 */
public class UniqueIntent {

    /** The fields and/or to-one relations the business key spans, in the declared order. */
    private List<String> fields = new ArrayList<>();

    /** The user-facing message when a write collides with an existing row. */
    private String message;

    /**
     * Gets the fields and to-one relations the key spans, in the declared order.
     *
     * @return the field names, never null
     */
    public List<String> getFields() {
        return fields == null ? List.of() : fields;
    }

    /**
     * Gets the user-facing conflict message.
     *
     * @return the message, or null when none was authored
     */
    public String getMessage() {
        return message;
    }
}
