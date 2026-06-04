/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.service;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.eclipse.dirigible.components.engine.nativeapps.repository.NativeAppRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class NativeAppService extends BaseArtefactService<NativeApp, Long> {

    public NativeAppService(NativeAppRepository repository) {
        super(repository);
    }

    public Optional<NativeApp> findOptionalByBasePath(String basePath) {
        return ((NativeAppRepository) getRepo()).findByBasePath(basePath);
    }

    public Optional<NativeApp> findOptionalById(Long id) {
        return getRepo().findById(id);
    }
}
