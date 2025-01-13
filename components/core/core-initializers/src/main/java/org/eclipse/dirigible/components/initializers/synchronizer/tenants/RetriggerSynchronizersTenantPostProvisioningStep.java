/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.synchronizer.tenants;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.components.base.synchronizer.Synchronizer;
import org.eclipse.dirigible.components.base.tenant.TenantPostProvisioningStep;
import org.eclipse.dirigible.components.base.tenant.TenantProvisioningException;
import org.eclipse.dirigible.components.initializers.definition.DefinitionService;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The Class RetriggerSynchronizersTenantPostProvisioningStep.
 */
@Component
class RetriggerSynchronizersTenantPostProvisioningStep implements TenantPostProvisioningStep {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(RetriggerSynchronizersTenantPostProvisioningStep.class);

    /** The multitenant synchronizers. */
    private final List<Synchronizer<?, ?>> multitenantSynchronizers;

    /** The definition service. */
    private final DefinitionService definitionService;

    /** The synchronization processor. */
    private final SynchronizationProcessor synchronizationProcessor;

    /**
     * Instantiates a new retrigger synchronizers tenant post provisioning step.
     *
     * @param synchronizers the synchronizers
     * @param definitionService the definition service
     * @param synchronizationProcessor the synchronization processor
     */
    RetriggerSynchronizersTenantPostProvisioningStep(List<Synchronizer<?, ?>> synchronizers, DefinitionService definitionService,
            SynchronizationProcessor synchronizationProcessor) {
        this.definitionService = definitionService;
        this.synchronizationProcessor = synchronizationProcessor;
        this.multitenantSynchronizers = synchronizers.stream()
                                                     .filter(Synchronizer::multitenantExecution)
                                                     .collect(Collectors.toList());
    }

    /**
     * Execute.
     *
     * @throws TenantProvisioningException the tenant provisioning exception
     */
    @Override
    public void execute() throws TenantProvisioningException {
        logger.info("Registered [{}] multitenant synchronizers: [{}]", multitenantSynchronizers.size(), multitenantSynchronizers);
        Set<String> multitenantArtifactTypes = multitenantSynchronizers.stream()
                                                                       .map(Synchronizer::getArtefactType)
                                                                       .collect(Collectors.toSet());
        logger.info("Changing checksums for definitions with types in [{}]", multitenantArtifactTypes);
        definitionService.updateChecksums(StringUtils.EMPTY, multitenantArtifactTypes);

        logger.info("Retriggering synchronizers...");
        synchronizationProcessor.forceProcessSynchronizers();
        logger.info("Synchronizers have completed.");
    }

}
