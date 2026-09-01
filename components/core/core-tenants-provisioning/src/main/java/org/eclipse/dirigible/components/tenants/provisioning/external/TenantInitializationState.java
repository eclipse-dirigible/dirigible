/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

/**
 * The initialization of a tenant, as a caller sees it.
 *
 * @param status how far it has got
 * @param error why it failed, or null
 */
public record TenantInitializationState(InitializationStatus status, String error) {

    /**
     * A state without an error detail.
     *
     * @param status the status
     * @return the state
     */
    static TenantInitializationState of(InitializationStatus status) {
        return new TenantInitializationState(status, null);
    }
}
