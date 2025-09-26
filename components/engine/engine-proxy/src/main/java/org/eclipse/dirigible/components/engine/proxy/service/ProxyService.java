/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.proxy.service;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.engine.proxy.domain.Proxy;
import org.eclipse.dirigible.components.engine.proxy.repository.ProxyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class ProxyService extends BaseArtefactService<Proxy, Long> {

    ProxyService(ProxyRepository proxyRepository) {
        super(proxyRepository);
    }

    public Optional<Proxy> findOptionalByName(String name) {
        return getRepo().findByName(name);
    }

}
