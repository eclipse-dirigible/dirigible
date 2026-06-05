/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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
import org.eclipse.dirigible.components.security.domain.Scope;
import org.eclipse.dirigible.components.security.service.ScopeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;

/**
 * The Class ScopesSynchronizer.
 *
 * <p>
 * Loads {@code *.scopes} artefacts from projects into the {@code DIRIGIBLE_SECURITY_SCOPES} table
 * so that one OAuth2 scope can grant several Dirigible roles. Mirrors {@link RolesSynchronizer}.
 */
@Component
@Order(SynchronizersOrder.SCOPE)
public class ScopesSynchronizer extends BaseSynchronizer<Scope, Long> {

    /**
     * The Constant FILE_EXTENSION_SECURITY_SCOPE.
     */
    public static final String FILE_EXTENSION_SECURITY_SCOPE = ".scopes";

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ScopesSynchronizer.class);

    /**
     * The scope service.
     */
    private final ScopeService scopeService;

    /**
     * The synchronization callback.
     */
    private SynchronizerCallback callback;

    /**
     * Instantiates a new scopes synchronizer.
     *
     * @param scopeService the scope service
     */
    @Autowired
    public ScopesSynchronizer(ScopeService scopeService) {
        this.scopeService = scopeService;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the artefact
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return Scope.ARTEFACT_TYPE.equals(type);
    }

    /**
     * Parse.
     *
     * @param location the location
     * @param content the content
     * @return the list
     * @throws ParseException the parse exception
     */
    @Override
    protected List<Scope> parseImpl(String location, byte[] content) throws ParseException {
        Scope[] scopes = JsonHelper.fromJson(new String(content, StandardCharsets.UTF_8), Scope[].class);
        int scopeIndex = 1;
        for (Scope scope : scopes) {
            Configuration.configureObject(scope);
            scope.setLocation(location);
            scope.setName(scope.getScope());
            scope.setType(Scope.ARTEFACT_TYPE);
            scope.updateKey();

            try {
                Scope maybe = getService().findByKey(scope.getKey());
                if (maybe != null) {
                    scope.setId(maybe.getId());
                }
                scope = getService().save(scope);
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
                if (logger.isErrorEnabled()) {
                    logger.error("scope: {}", scope);
                }
                if (logger.isErrorEnabled()) {
                    logger.error("content: {}", new String(content));
                }
                throw new ParseException(e.getMessage(), scopeIndex);
            }
            scopeIndex++;
        }
        return List.of(scopes);
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<Scope, Long> getService() {
        return scopeService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<Scope> retrieve(String location) {
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
    public void setStatus(Scope artefact, ArtefactLifecycle lifecycle, String error) {
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
    protected boolean completeImpl(TopologyWrapper<Scope> wrapper, ArtefactPhase flow) {
        callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
        return true;
    }

    /**
     * Cleanup.
     *
     * @param scope the scope
     */
    @Override
    public void cleanupImpl(Scope scope) {
        try {
            getService().delete(scope);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, scope, ArtefactLifecycle.DELETED, e);
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
        return FILE_EXTENSION_SECURITY_SCOPE;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return Scope.ARTEFACT_TYPE;
    }
}
