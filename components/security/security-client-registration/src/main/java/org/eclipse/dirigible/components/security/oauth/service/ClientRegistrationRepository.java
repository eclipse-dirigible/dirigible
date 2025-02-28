/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth.service;

import java.util.Optional;
import org.eclipse.dirigible.components.security.oauth.domain.ClientRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The Interface OAuthClientRegistrationRepository.
 */
interface ClientRegistrationRepository extends JpaRepository<ClientRegistration, String> {

    /**
     * Find by clientId.
     *
     * @param clientId the client id
     * @return the optional
     */
    Optional<ClientRegistration> findByClientId(String clientId);

}
