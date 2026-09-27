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
 * The body of a refusal of the tenant users endpoints.
 *
 * @param status the HTTP status
 * @param error the status name
 * @param message why, for a person
 * @param reason why, for a program
 */
record TenantUsersRefusal(int status, String error, String message, String reason) {
}
