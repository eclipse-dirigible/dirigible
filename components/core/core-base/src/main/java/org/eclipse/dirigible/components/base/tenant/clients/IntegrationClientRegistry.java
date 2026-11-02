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

import java.util.Optional;

/**
 * The integration clients this application accepts: which machine client belongs to which tenant,
 * with which roles. The external provisioning system writes the registry; the security layer reads
 * it on every request of a client that is not one of the deployment's own, so a revoked client is
 * refused on its next call rather than when its token expires.
 *
 * <p>
 * Declared here, free of any storage, so the security modules depend on the question rather than on
 * the module that keeps the answer. Absent when that module is not deployed, in which case no
 * client is registered.
 */
public interface IntegrationClientRegistry {

    /**
     * The registered client of an id, unless it was revoked.
     *
     * @param clientId the client id the access token names
     * @return the client, empty when the id is unknown or revoked
     */
    Optional<IntegrationClient> findActive(String clientId);
}
