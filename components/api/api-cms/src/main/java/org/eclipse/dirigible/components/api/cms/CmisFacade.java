/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.cms;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.api.http.HttpRequestFacade;
import org.eclipse.dirigible.components.engine.cms.CmisSessionFactory;
import org.eclipse.dirigible.components.engine.cms.access.CmsAccessGrant;
import org.eclipse.dirigible.components.engine.cms.access.CmsAccessService;
import org.eclipse.dirigible.components.security.domain.Access;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;

/**
 * The Class CmisFacade.
 */
@Component
public class CmisFacade implements InitializingBean {

    private static final String SECURITY_TYPE_CMIS = "CMIS";
    /** The Constant CMIS_METHOD_READ. */
    public static final String CMIS_METHOD_READ = "READ";
    /** The Constant CMIS_METHOD_WRITE. */
    public static final String CMIS_METHOD_WRITE = "WRITE";
    /** The Constant DIRIGIBLE_CMS_ROLES_ENABLED. */
    public static final String DIRIGIBLE_CMS_ROLES_ENABLED = "DIRIGIBLE_CMS_ROLES_ENABLED";
    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CmisFacade.class);
    /** The instance. */
    private static CmisFacade INSTANCE;

    /**
     * The CMS path grants. This facade no longer resolves constraints itself: the decision belongs to
     * one service so the Java callers and the JavaScript ones cannot drift apart.
     */
    private final CmsAccessService cmsAccessService;

    /**
     * Instantiates a new cmis facade.
     *
     * @param cmsAccessService the CMS access grants
     */
    @Autowired
    public CmisFacade(CmsAccessService cmsAccessService) {
        this.cmsAccessService = cmsAccessService;
    }

    /**
     * After properties set.
     *
     * @throws Exception the exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
    }

    /**
     * CMIS Session.
     *
     * @return the CMIS session object
     */
    public static final Object getSession() {
        return CmisSessionFactory.getSession();
    }

    /**
     * Mapping utility between the CMIS standard and Javascript string representation of the versioning
     * state.
     *
     * @param state the Javascript state
     * @return the CMIS state
     */
    public static final Object getVersioningState(String state) {
        return CmisSessionFactory.getVersioningState(state);
    }

    /**
     * Gets the unified object delete.
     *
     * @return the unified object delete
     */
    public static final Object getUnifiedObjectDelete() {
        return org.apache.chemistry.opencmis.commons.enums.UnfileObject.DELETE;
    }

    /**
     * Checks if the user can access the given path with the given method.
     *
     * @param path the path
     * @param method the method
     * @return true, if the user is in role
     */
    public static final boolean isAllowed(String path, String method) {
        if (Configuration.isAnonymousModeEnabled()) {
            return true;
        }

        if (!Boolean.parseBoolean(Configuration.get(DIRIGIBLE_CMS_ROLES_ENABLED, Boolean.TRUE.toString()))) {
            return true;
        }

        if (!HttpRequestFacade.isValid()) {
            return true;
        }

        try {
            return CmisFacade.get()
                             .getCmsAccessService()
                             .isAllowed(path, method, CmisFacade::isUserInRole);
        } catch (RuntimeException e) {
            logger.error("Failed to resolve the CMS access of [{}] for [{}]", path, method, e);
        }
        return true;
    }

    /** Whether the caller of the current request holds a role. */
    private static boolean isUserInRole(String role) {
        return HttpRequestFacade.isUserInRole(role);
    }

    /**
     * Gets the access definitions.
     *
     * @param path the path
     * @param method the method
     * @return the access definitions
     * @throws ServletException the servlet exception
     */
    public static Set<Access> getAccessDefinitions(String path, String method) throws ServletException {
        Set<Access> accessDefinitions = new HashSet<Access>();
        for (CmsAccessGrant grant : CmisFacade.get()
                                              .getCmsAccessService()
                                              .effectiveGrants(path)
                                              .all()) {
            if (method.equalsIgnoreCase(grant.method())) {
                Access access = new Access();
                access.setScope(SECURITY_TYPE_CMIS);
                access.setPath(grant.path());
                access.setMethod(grant.method());
                access.setRole(grant.role());
                accessDefinitions.add(access);
            }
        }
        return accessDefinitions;
    }

    /**
     * Gets the CMS access grants.
     *
     * @return the CMS access grants
     */
    public CmsAccessService getCmsAccessService() {
        return cmsAccessService;
    }

    /**
     * Gets the instance.
     *
     * @return the cmis facade
     */
    public static CmisFacade get() {
        return INSTANCE;
    }
}
