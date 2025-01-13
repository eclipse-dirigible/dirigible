/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.cms;

import jakarta.servlet.ServletException;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.api.http.HttpRequestFacade;
import org.eclipse.dirigible.components.engine.cms.CmsProvider;
import org.eclipse.dirigible.components.engine.cms.CmsProviderFactory;
import org.eclipse.dirigible.components.security.domain.Access;
import org.eclipse.dirigible.components.security.verifier.AccessVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * The Class CmisFacade.
 */
@Component
public class CmisFacade implements ApplicationContextAware, InitializingBean {

    /** The Constant VERSIONING_STATE_NONE. */
    public static final String VERSIONING_STATE_NONE = "none";
    /** The Constant VERSIONING_STATE_MAJOR. */
    public static final String VERSIONING_STATE_MAJOR = "major";
    /** The Constant VERSIONING_STATE_MINOR. */
    public static final String VERSIONING_STATE_MINOR = "minor";
    /** The Constant VERSIONING_STATE_CHECKEDOUT. */
    public static final String VERSIONING_STATE_CHECKEDOUT = "checkedout";
    /** The Constant CMIS_METHOD_READ. */
    public static final String CMIS_METHOD_READ = "READ";
    /** The Constant CMIS_METHOD_WRITE. */
    public static final String CMIS_METHOD_WRITE = "WRITE";
    /** The Constant DIRIGIBLE_CMS_ROLES_ENABLED. */
    public static final String DIRIGIBLE_CMS_ROLES_ENABLED = "DIRIGIBLE_CMS_ROLES_ENABLED";
    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CmisFacade.class);
    /** The application context. */
    private static ApplicationContext applicationContext;
    /** The instance. */
    private static CmisFacade INSTANCE;
    /** The security access verifier. */
    private final AccessVerifier securityAccessVerifier;

    /**
     * Instantiates a new cmis facade.
     *
     * @param securityAccessVerifier the security access verifier
     */
    @Autowired
    public CmisFacade(AccessVerifier securityAccessVerifier) {
        this.securityAccessVerifier = securityAccessVerifier;
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
     * Sets the application context.
     *
     * @param ac the new application context
     */
    @Override
    public void setApplicationContext(ApplicationContext ac) {
        CmisFacade.applicationContext = ac;
    }

    /**
     * CMIS Session.
     *
     * @return the CMIS session object
     */
    public static final Object getSession() {
        String type = Configuration.get("DIRIGIBLE_CMS_PROVIDER", "cms-provider-internal");
        CmsProviderFactory cmsProviderFactory = applicationContext.getBean(type, CmsProviderFactory.class);
        CmsProvider cmsProvider = cmsProviderFactory.create();
        return cmsProvider.getSession();
    }

    /**
     * Mapping utility between the CMIS standard and Javascript string representation of the versioning
     * state.
     *
     * @param state the Javascript state
     * @return the CMIS state
     */
    public static final Object getVersioningState(String state) {
        if (VERSIONING_STATE_NONE.equals(state)) {
            return org.apache.chemistry.opencmis.commons.enums.VersioningState.NONE;
        } else if (VERSIONING_STATE_MAJOR.equals(state)) {
            return org.apache.chemistry.opencmis.commons.enums.VersioningState.MAJOR;
        } else if (VERSIONING_STATE_MINOR.equals(state)) {
            return org.apache.chemistry.opencmis.commons.enums.VersioningState.MINOR;
        } else if (VERSIONING_STATE_CHECKEDOUT.equals(state)) {
            return org.apache.chemistry.opencmis.commons.enums.VersioningState.CHECKEDOUT;
        }
        return org.apache.chemistry.opencmis.commons.enums.VersioningState.MAJOR;
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
            String user = HttpRequestFacade.getRemoteUser();
            Set<Access> readDefinitions = getAccessDefinitions(path, CMIS_METHOD_READ);
            boolean isReadOnly = false;
            boolean isReadable = true;
            if (!readDefinitions.isEmpty()) {
                isReadable = false;
                if (user == null) {
                    if (logger.isErrorEnabled()) {
                        logger.error("No logged in user accessing path: " + path);
                    }
                    return false;
                }
            }

            for (Access readDefinition : readDefinitions) {
                if (HttpRequestFacade.isUserInRole(readDefinition.getRole())) {
                    isReadOnly = true;
                    isReadable = true;
                    break;
                }
            }

            if (method.equals(CMIS_METHOD_WRITE)) {
                Set<Access> writeDefinitions = getAccessDefinitions(path, CMIS_METHOD_WRITE);
                if (!writeDefinitions.isEmpty()) {
                    isReadOnly = true;
                    if (user == null) {
                        if (logger.isErrorEnabled()) {
                            logger.error("No logged in user accessing path: " + path);
                        }
                        return false;
                    }
                }
                for (Access writeDefinition : writeDefinitions) {
                    if (HttpRequestFacade.isUserInRole(writeDefinition.getRole())) {
                        isReadOnly = false;
                        break;
                    }
                }
                return isReadable && !isReadOnly;
            } else {
                return isReadable;
            }

        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
        }
        return true;
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
        int indexOf = 0;
        do {
            String accessPath = path;
            indexOf = path.indexOf("/", indexOf + 1);
            if (indexOf > 0) {
                accessPath = path.substring(0, indexOf);
            }
            accessDefinitions.addAll(CmisFacade.get()
                                               .getSecurityAccessVerifier()
                                               .getMatchingSecurityAccesses("CMIS", accessPath, method));
        } while (indexOf > 0);
        return accessDefinitions;
    }

    /**
     * Gets the security access verifier.
     *
     * @return the security access verifier
     */
    public AccessVerifier getSecurityAccessVerifier() {
        return securityAccessVerifier;
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
