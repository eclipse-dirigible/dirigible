/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.event;

import org.eclipse.dirigible.components.security.domain.Role;
import org.springframework.context.ApplicationEvent;

/**
 * Published by {@code RolesSynchronizer} immediately before a {@link Role} row is removed from
 * {@code DIRIGIBLE_SECURITY_ROLES}. Listeners are expected to remove any rows that hold an FK to
 * the role being deleted (notably {@code DIRIGIBLE_USER_ROLE_ASSIGNMENTS}) so the subsequent
 * {@code DELETE} does not trip a referential-integrity constraint.
 *
 * Decoupled via a Spring event because the role-assignment table lives in {@code core-tenants},
 * which depends on {@code engine-security}; a direct call from the synchronizer would invert that
 * dependency.
 */
public class RoleDeletionEvent extends ApplicationEvent {

    private final Role role;

    public RoleDeletionEvent(Object source, Role role) {
        super(source);
        this.role = role;
    }

    public Role getRole() {
        return role;
    }
}
