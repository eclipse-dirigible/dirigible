/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms;

import java.util.Arrays;
import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * A factory for creating CmisSession objects.
 */
@Component
public class CmisSessionFactory implements ApplicationContextAware, InitializingBean {

    /** The Constant DIRIGIBLE_CMS_PROVIDER. */
    private static final String DIRIGIBLE_CMS_PROVIDER = "DIRIGIBLE_CMS_PROVIDER";
    /** The Constant CMS_PROVIDER_INTERNAL. */
    private static final String CMS_PROVIDER_INTERNAL = "cms-provider-internal";
    /**
     * The providers that ship as add-on modules outside the default bundle, by provider name, with the
     * Maven artifact an application adds to get them.
     */
    private static final Map<String, String> ADD_ON_PROVIDERS =
            Map.of("cms-provider-ms-sharepoint", "org.eclipse.dirigible:dirigible-components-engine-cms-sharepoint");
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
    private static final Logger logger = LoggerFactory.getLogger(CmisSessionFactory.class);
    /** The application context. */
    private static ApplicationContext applicationContext;
    /** The instance. */
    private static CmisSessionFactory INSTANCE;

    /**
     * After properties set.
     *
     * @throws Exception the exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
        // fail the boot, not the first document request, when an explicitly configured provider is absent
        String configured = Configuration.get(DIRIGIBLE_CMS_PROVIDER);
        if (configured != null && !configured.isBlank()) {
            resolveProviderFactory(applicationContext, configured);
        }
    }

    /**
     * Sets the application context.
     *
     * @param ac the new application context
     */
    @Override
    public void setApplicationContext(ApplicationContext ac) {
        CmisSessionFactory.applicationContext = ac;
    }

    /**
     * Gets the instance.
     *
     * @return the cmis facade
     */
    public static CmisSessionFactory get() {
        return INSTANCE;
    }

    /**
     * CMIS Session.
     *
     * @return the CMIS session object
     */
    public static final CmisSession getSession() {
        CmsProviderFactory cmsProviderFactory = resolveProviderFactory(applicationContext, getConfiguredProvider());
        CmsProvider cmsProvider = cmsProviderFactory.create();
        return (CmisSession) cmsProvider.getSession();
    }

    private static String getConfiguredProvider() {
        return Configuration.get(DIRIGIBLE_CMS_PROVIDER, CMS_PROVIDER_INTERNAL);
    }

    /**
     * Resolves the factory of the configured CMS provider, failing with a message that names the
     * missing module when the provider is not on the classpath.
     *
     * @param beanFactory the bean factory holding the provider factories
     * @param provider the configured provider name (the value of DIRIGIBLE_CMS_PROVIDER)
     * @return the provider factory
     * @throws IllegalStateException when no provider of that name is available
     */
    static CmsProviderFactory resolveProviderFactory(ListableBeanFactory beanFactory, String provider) {
        String[] available = beanFactory.getBeanNamesForType(CmsProviderFactory.class);
        if (Arrays.asList(available)
                  .contains(provider)) {
            return beanFactory.getBean(provider, CmsProviderFactory.class);
        }
        String addOn = ADD_ON_PROVIDERS.get(provider);
        String message = addOn != null
                ? DIRIGIBLE_CMS_PROVIDER + " is set to '" + provider
                        + "', but that provider is an add-on that is not bundled by default; add the " + addOn
                        + " artifact to the application, or unset " + DIRIGIBLE_CMS_PROVIDER + " to use the internal repository"
                : DIRIGIBLE_CMS_PROVIDER + " is set to '" + provider + "', which is not an available CMS provider; available providers: "
                        + String.join(", ", available);
        logger.error(message);
        throw new IllegalStateException(message);
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
}
