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
 * The state of the latest request made for an application user.
 */
public enum ApplicationUserRequestState {

    /** Published, not answered yet. */
    SENT,

    /** Answered with a granted role. */
    COMPLETED,

    /** Answered with a failure. */
    FAILED
}
