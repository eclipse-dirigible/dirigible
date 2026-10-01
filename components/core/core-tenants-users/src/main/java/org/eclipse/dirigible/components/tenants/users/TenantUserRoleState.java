/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.users;

/**
 * Where one role of a {@link TenantUser} stands. A role with no row is not held.
 */
public enum TenantUserRoleState {

    /** The role is held. */
    GRANTED,

    /** A request adds the role, and it is not applied yet. */
    ADDING,

    /** The role is held, and a request removes it. */
    REMOVING
}
