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

import java.util.List;
import java.util.Optional;
import org.eclipse.dirigible.components.security.oauth.domain.ClientRegistration;
import org.springframework.stereotype.Service;

/**
 * The Class OAuthClientRegistrationService.
 */
@Service
public class ClientRegistrationService {

    /** The oauth client registratio repository. */
    private final ClientRegistrationRepository repository;

    /**
     * Instantiates a new tenant service.
     *
     * @param repository the oauth client registration repository
     */
    public ClientRegistrationService(ClientRegistrationRepository repository) {
        this.repository = repository;
    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    public final List<ClientRegistration> getAll() {
        return repository.findAll();
    }

    /**
     * Find by client id.
     *
     * @param clientId the clientId
     * @return the optional
     */
    public Optional<ClientRegistration> findByClientId(String clientId) {
        return repository.findByClientId(clientId);
    }

    /**
     * Save.
     *
     * @param registration the oauth client registration
     * @return the oauth client registration
     */
    public ClientRegistration save(ClientRegistration registration) {
        return repository.save(registration);
    }

    /**
     * Find by id.
     *
     * @param id the id
     * @return the optional
     */
    public Optional<ClientRegistration> findById(String id) {
        return repository.findById(id);
    }

    /**
     * Delete.
     *
     * @param registration the tenant
     */
    public void delete(ClientRegistration registration) {
        repository.delete(registration);
    }

}
