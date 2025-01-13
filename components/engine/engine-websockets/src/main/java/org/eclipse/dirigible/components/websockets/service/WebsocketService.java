/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.websockets.service;

import java.util.Optional;
import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.websockets.domain.Websocket;
import org.eclipse.dirigible.components.websockets.repository.WebsocketRepository;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class WebsocketService.
 */
@Service
@Transactional
public class WebsocketService extends BaseArtefactService<Websocket, Long> {

    /**
     * Instantiates a new websocket service.
     *
     * @param repository the repository
     */
    public WebsocketService(WebsocketRepository repository) {
        super(repository);
    }

    /**
     * Find by endpoint.
     *
     * @param endpoint the endpoint
     * @return the websocket
     */
    @Transactional(readOnly = true)
    public Websocket findByEndpoint(String endpoint) {
        Websocket filter = new Websocket();
        filter.setEndpoint(endpoint);
        Example<Websocket> example = Example.of(filter);
        Optional<Websocket> extension = getRepo().findOne(example);
        if (extension.isPresent()) {
            return extension.get();
        }
        throw new IllegalArgumentException("Websocket for endpoint does not exist: " + endpoint);
    }
}
