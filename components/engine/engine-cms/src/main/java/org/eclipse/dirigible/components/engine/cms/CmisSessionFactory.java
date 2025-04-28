package org.eclipse.dirigible.components.engine.cms;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.security.verifier.AccessVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
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
    /** The security access verifier. */
    private final AccessVerifier securityAccessVerifier;

    /**
     * Instantiates a new cmis session factory.
     *
     * @param securityAccessVerifier the security access verifier
     */
    @Autowired
    public CmisSessionFactory(AccessVerifier securityAccessVerifier) {
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
        String type = Configuration.get(DIRIGIBLE_CMS_PROVIDER, CMS_PROVIDER_INTERNAL);
        CmsProviderFactory cmsProviderFactory = applicationContext.getBean(type, CmsProviderFactory.class);
        CmsProvider cmsProvider = cmsProviderFactory.create();
        return (CmisSession) cmsProvider.getSession();
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
