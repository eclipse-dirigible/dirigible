/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.service;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.security.repository.RoleRepository;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class RoleService.
 */

@Service
@Transactional
public class RoleService extends BaseArtefactService<Role, Long> {

    /**
     * Instantiates a new role service.
     *
     * @param repository the repository
     */
    public RoleService(RoleRepository repository) {
        super(repository);
    }

    /**
     * Role exists by name.
     *
     * @param name the name
     * @return true, if successful
     */
    @Transactional(readOnly = true)
    public boolean roleExistsByName(String name) {
        Role filter = new Role();
        filter.setName(name);
        Example<Role> example = Example.of(filter);
        return getRepo().findOne(example)
                        .isPresent();
    }

}
