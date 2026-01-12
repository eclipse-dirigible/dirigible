/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.synchronizer;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.base.synchronizer.BaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.security.domain.Access;
import org.eclipse.dirigible.components.security.domain.Constraints;
import org.eclipse.dirigible.components.security.service.AccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Class AccessSynchronizer.
 */

@Component
@Order(SynchronizersOrder.ACCESS)
public class AccessSynchronizer extends BaseSynchronizer<Access, Long> {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(AccessSynchronizer.class);

    /**
     * The Constant FILE_EXTENSION_SECURITY_ACCESS.
     */
    private static final String FILE_EXTENSION_SECURITY_ACCESS = ".access";

    /**
     * The security access service.
     */
    private final AccessService securityAccessService;

    /**
     * The synchronization callback.
     */
    private SynchronizerCallback callback;

    /**
     * Instantiates a new security access synchronizer.
     *
     * @param securityAccessService the security access service
     */
    @Autowired
    public AccessSynchronizer(AccessService securityAccessService) {
        this.securityAccessService = securityAccessService;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the artefact
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return Access.ARTEFACT_TYPE.equals(type);
    }

    /**
     * Load.
     *
     * @param location the location
     * @param content the content
     * @return the list
     * @throws ParseException the parse exception
     */
    @Override
    public List<Access> parseImpl(String location, byte[] content) throws ParseException {
        Constraints constraints = JsonHelper.fromJson(new String(content, StandardCharsets.UTF_8), Constraints.class);
        Configuration.configureObject(constraints);

        List<Access> accesses = constraints.buildSecurityAccesses(location);
        List<Access> result = new ArrayList<Access>();

        for (int idx = 0; idx < accesses.size(); idx++) {
            Access access = accesses.get(idx);
            try {
                access.updateKey();
                Access maybe = getService().findByKey(access.getKey());
                if (maybe != null) {
                    access.setId(maybe.getId());
                }
                access = getService().save(access);
                result.add(access);
            } catch (Exception e) {
                String errorMessage = "Failed tp parse access " + access;
                logger.error(errorMessage, e);
                throw new ParseException(errorMessage, idx);
            }
        }
        return result;
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<Access, Long> getService() {
        return securityAccessService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<Access> retrieve(String location) {
        return getService().findByLocation(location);
    }

    /**
     * Sets the status.
     *
     * @param artefact the artefact
     * @param lifecycle the lifecycle
     * @param error the error
     */
    @Override
    public void setStatus(Access artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    /**
     * Complete.
     *
     * @param wrapper the wrapper
     * @param flow the flow
     * @return true, if successful
     */
    @Override
    protected boolean completeImpl(TopologyWrapper<Access> wrapper, ArtefactPhase flow) {
        callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
        return true;
    }

    /**
     * Cleanup.
     *
     * @param access the security access
     */
    @Override
    public void cleanupImpl(Access access) {
        try {
            getService().delete(access);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, access, ArtefactLifecycle.DELETED, e);
        }
    }

    /**
     * Sets the callback.
     *
     * @param callback the new callback
     */
    @Override
    public void setCallback(SynchronizerCallback callback) {
        this.callback = callback;
    }

    /**
     * Gets the file extension.
     *
     * @return the file extension
     */
    @Override
    public String getFileExtension() {
        return FILE_EXTENSION_SECURITY_ACCESS;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return Access.ARTEFACT_TYPE;
    }

}
