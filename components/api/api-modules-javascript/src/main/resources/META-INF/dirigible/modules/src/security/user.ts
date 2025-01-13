/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */

const UserFacade = Java.type("org.eclipse.dirigible.components.api.security.UserFacade");

export class User {

    public static getName(): string {
        return UserFacade.getName();
    }

    public static isInRole(role: string): boolean {
        return UserFacade.isInRole(role);
    }

    public static getTimeout(): number {
        return UserFacade.getTimeout();
    }

    public static getAuthType(): string {
        return UserFacade.getAuthType();
    }

    public static getSecurityToken(): string {
        return UserFacade.getSecurityToken();
    }

    public static getInvocationCount(): number {
        return UserFacade.getInvocationCount();
    }

    public static getLanguage(): string {
        return UserFacade.getLanguage();
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = User;
}
