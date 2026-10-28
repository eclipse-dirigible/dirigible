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
 * A boolean field's {@code single:} (dirigible #8101): at most one row holds it - the "default /
 * base" flag every nomenclature carries.
 *
 * <pre>
 * fields:
 *   - { name: base, type: boolean, single: true }                                  # one base currency
 *   - { name: base, type: boolean, single: { per: Company } }                      # one per company
 *   - { name: base, type: boolean, single: { per: Company, onConflict: refuse } }  # 409 instead
 * </pre>
 *
 * A write that sets the flag demotes the row that held it in the same scope, in the same unit of
 * work; with {@code onConflict: refuse} it is answered 409 while another row holds it. The
 * shorthand {@code single: true} is the empty mapping (fleet-wide, demote).
 */
public class SingleIntent {

    /** The to-one relation that scopes the flag (one holder per its value), or null for fleet-wide. */
    private String per;

    /** {@code demote} (the default) or {@code refuse}. */
    private String onConflict;

    public String getPer() {
        return per;
    }

    public String getOnConflict() {
        return onConflict;
    }

    /**
     * Whether a write setting the flag while another row holds it is refused instead of demoting it.
     *
     * @return true for {@code onConflict: refuse}
     */
    public boolean isRefuse() {
        return "refuse".equals(onConflict);
    }
}
