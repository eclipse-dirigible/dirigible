/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible
 * contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.access;

public enum DirigibleRole {

    DEVELOPER(RoleNames.DEVELOPER), OPERATOR(RoleNames.OPERATOR);

    private final String roleName;

    DirigibleRole(String roleName) {
        this.roleName = roleName;
    }

    public String getName() {
        return roleName;
    }

    @SuppressWarnings("hiding")
    public static class RoleNames {
        public static final String OPERATOR = "Operator";
        public static final String DEVELOPER = "Developer";

    }
}
