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
import org.eclipse.dirigible.components.security.domain.Role;
import org.eclipse.dirigible.components.security.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.Set;

/**
 * The Class SecurityRoleSynchronizer.
 */

@Component
@Order(SynchronizersOrder.ROLE)
public class RolesSynchronizer extends BaseSynchronizer<Role, Long> {

    /**
     * The Constant FILE_EXTENSION_SECURITY_ROLE.
     */
    public static final String FILE_EXTENSION_SECURITY_ROLE = ".roles";
    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(RolesSynchronizer.class);

    /** The Constant PRESERVED_ROLE_LOCATION_PREFIXES. */
    private static final Set<String> PRESERVED_ROLE_LOCATION_PREFIXES = Set.of("SYSTEM_");
    /**
     * The security role service.
     */
    private final RoleService securityRoleService;

    /**
     * The synchronization callback.
     */
    private SynchronizerCallback callback;

    /**
     * Instantiates a new security role synchronizer.
     *
     * @param securityRoleService the security role service
     */
    @Autowired
    public RolesSynchronizer(RoleService securityRoleService) {
        this.securityRoleService = securityRoleService;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the artefact
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return Role.ARTEFACT_TYPE.equals(type);
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
    protected List<Role> parseImpl(String location, byte[] content) throws ParseException {
        Role[] roles = JsonHelper.fromJson(new String(content, StandardCharsets.UTF_8), Role[].class);
        Integer roleIndex = 1;
        for (Role role : roles) {
            Configuration.configureObject(role);
            role.setLocation(location);
            role.setName(role.getName());
            role.setType(Role.ARTEFACT_TYPE);
            role.updateKey();

            try {
                Role maybe = getService().findByKey(role.getKey());
                if (maybe != null) {
                    role.setId(maybe.getId());
                }
                role = getService().save(role);
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
                if (logger.isErrorEnabled()) {
                    logger.error("security role: {}", role);
                }
                if (logger.isErrorEnabled()) {
                    logger.error("content: {}", new String(content));
                }
                throw new ParseException(e.getMessage(), roleIndex);
            }
            roleIndex++;
        }
        return List.of(roles);
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<Role, Long> getService() {
        return securityRoleService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<Role> retrieve(String location) {
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
    public void setStatus(Role artefact, ArtefactLifecycle lifecycle, String error) {
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
    protected boolean completeImpl(TopologyWrapper<Role> wrapper, ArtefactPhase flow) {
        callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
        return true;
    }

    /**
     * Cleanup.
     *
     * @param role the security role
     */
    @Override
    public void cleanupImpl(Role role) {
        try {
            Boolean delete = PRESERVED_ROLE_LOCATION_PREFIXES.stream()
                                                             .filter(p -> role.getLocation()
                                                                              .startsWith(p))
                                                             .findFirst()
                                                             .map(p -> Boolean.FALSE)
                                                             .orElse(Boolean.TRUE);
            if (delete) {
                getService().delete(role);
            }
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, role, ArtefactLifecycle.DELETED, e);
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
        return FILE_EXTENSION_SECURITY_ROLE;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return Role.ARTEFACT_TYPE;
    }
}
