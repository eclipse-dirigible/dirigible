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
 * Where one role of an application user stands: asked for, granted, or refused.
 */
public enum ApplicationUserRoleState {

    /** The request for the role was published and not answered yet. */
    REQUESTED,

    /** The role is granted - the person holds the identity-provider group. */
    GRANTED,

    /** The latest request for the role failed. */
    FAILED
}
