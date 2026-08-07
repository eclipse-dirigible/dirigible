/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.artefact.endpoint;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;

/**
 * Exposes what is deployed and whether it synchronized, aggregated across every artefact type. Each
 * synchronizer registers its own {@link ArtefactService} bean, so the injected bean list is the
 * complete inventory - there is no registration step for a new artefact type.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_CORE + "artefacts")
@RolesAllowed({"ADMINISTRATOR", "DEVELOPER", "OPERATOR"})
class ArtefactStatusEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtefactStatusEndpoint.class);

    private final List<ArtefactService<?, ?>> artefactServices;

    ArtefactStatusEndpoint(List<ArtefactService<?, ?>> artefactServices) {
        this.artefactServices = artefactServices;
    }

    /**
     * Gets the status of all artefacts known to the instance.
     *
     * @return the artefact statuses
     */
    @GetMapping
    ResponseEntity<List<ArtefactStatus>> getArtefacts() {
        List<ArtefactStatus> statuses = new ArrayList<>();
        for (ArtefactService<?, ?> artefactService : artefactServices) {
            statuses.addAll(getStatuses(artefactService));
        }
        return ResponseEntity.ok(statuses);
    }

    /**
     * Reads one artefact type. A type that cannot be read - a table missing because its engine is not
     * enabled on this instance, for example - must not take down the whole inventory, which is the one
     * screen an operator uses to find out what is broken.
     *
     * @param artefactService the service to read
     * @return the statuses of its artefacts, empty when it cannot be read
     */
    private List<ArtefactStatus> getStatuses(ArtefactService<?, ?> artefactService) {
        try {
            return artefactService.getAll()
                                  .stream()
                                  .map(ArtefactStatus::of)
                                  .toList();
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to read the artefacts of [{}]", artefactService.getClass()
                                                                               .getName(),
                    e);
            return List.of();
        }
    }
}
