/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.tenant;

/**
 * A tenant the user may enter, as offered to the tenant picker.
 *
 * @param id the tenant id, as it appears in the user's groups
 * @param name the name this instance knows the tenant under, the id when it knows none
 * @param provisionedHere whether this instance has finished provisioning the tenant; a tenant that
 *        is not cannot be entered yet, and the picker says so instead of hiding it. Exactly
 *        {@code state == State#READY}, kept because it is the field the picker was first written
 *        against
 * @param state why the tenant can or cannot be entered - {@code provisionedHere} only says that it
 *        cannot, and the two reasons need different words
 */
public record TenantOption(String id, String name, boolean provisionedHere, State state) {

    /**
     * What this instance knows about a tenant the user's groups grant them.
     *
     * <p>
     * The distinction is the whole point: one of these ends by itself and the other never does, so
     * telling a user to wait is right for one and misleading for the other.
     */
    public enum State {
        /** Registered here and provisioned: the user can enter it. */
        READY,
        /** Registered here, provisioning not finished. Waiting is the correct advice. */
        PREPARING,
        /**
         * Not registered here at all. Nothing in this instance is preparing it, so waiting will not help -
         * the group says the user belongs to a tenant this application has never been told about, which is
         * worth an operator's attention rather than a user's patience.
         */
        UNKNOWN
    }
}
