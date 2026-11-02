/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.tenant.clients;

import java.io.Serializable;
import java.util.Set;

/**
 * An integration client as the registry knows it: the machine client of a tenant's external system,
 * which calls this application with an access token of its own (client credentials) and acts in
 * exactly one tenant, with the roles it was given there.
 *
 * @param clientId the client id the identity provider issued, as the access token names it
 * @param tenantId the one tenant the client may enter
 * @param name the human-readable name of the client
 * @param roles the roles the client holds in its tenant - never a role its token's scopes map to
 */
public record IntegrationClient(String clientId, String tenantId, String name, Set<String> roles) implements Serializable {

    /**
     * Keeps the roles immutable whatever the caller handed in.
     */
    public IntegrationClient {
        roles = Set.copyOf(roles);
    }
}
