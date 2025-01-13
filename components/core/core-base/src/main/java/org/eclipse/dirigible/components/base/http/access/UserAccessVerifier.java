/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.access;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The Interface UserAccessVerifier.
 */
public interface UserAccessVerifier {

    /**
     * Checks if is in role.
     *
     * @param request the request
     * @param role the role
     * @return true, if is in role
     */
    public boolean isInRole(HttpServletRequest request, String role);
}
