/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.Session;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.api.http.HttpRequestFacade;
import org.eclipse.dirigible.components.api.http.HttpSessionFacade;
import org.eclipse.dirigible.components.base.context.ContextException;
import org.eclipse.dirigible.components.base.context.ThreadContextFacade;
import org.eclipse.dirigible.components.base.util.AuthoritiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static java.text.MessageFormat.format;

/**
 * The Class UserFacade.
 */
@Component
public class UserFacade {

    /** The Constant DIRIGIBLE_ANONYMOUS_USER_NAME_PROPERTY_NAME. */
    private static final String DIRIGIBLE_ANONYMOUS_USER_NAME_PROPERTY_NAME = "DIRIGIBLE_ANONYMOUS_USER_NAME_PROPERTY_NAME";

    /** The Constant DIRIGIBLE_ANONYMOUS_IDENTIFIER. */
    private static final String DIRIGIBLE_ANONYMOUS_IDENTIFIER = "dirigible-anonymous-identifier";

    /** The Constant DIRIGIBLE_ANONYMOUS_USER. */
    private static final String DIRIGIBLE_ANONYMOUS_USER = "dirigible-anonymous-user";

    /** The Constant DIRIGIBLE_JWT_USER. */
    private static final String DIRIGIBLE_JWT_USER = "dirigible-jwt-user";

    /** The Constant NO_VALID_REQUEST. */
    private static final String NO_VALID_REQUEST = "Trying to use HTTP Session Facade without a valid Session (HTTP Request/Response)";

    /** The Constant INVOCATION_COUNT. */
    private static final String INVOCATION_COUNT = "invocation.count";

    /** The Constant LANGUAGE_HEADER. */
    private static final String LANGUAGE_HEADER = "accept-language";

    /** The Constant ANY_LANGUAGE. */
    private static final String ANY_LANGUAGE = "*";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(UserFacade.class);

    /** The Constant GUEST. */
    private static final String GUEST = "guest";

    /** The Constant AUTH. */
    private static final String AUTH = "authorization";

    /**
     * Checks if the user is in role.
     *
     * @param role the role
     * @return true, if the user is in role
     */
    public static final boolean isInRole(String role) {
        if (Configuration.isAnonymousModeEnabled() || Configuration.isAnonymousUserEnabled()) {
            return true;
        }
        try {
            return HttpRequestFacade.isUserInRole(role);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
        }

        return false;
    }

    /**
     * Gets the user name by a given request as parameter.
     *
     * @param request the request
     * @return the user name
     */
    public static final String getName(HttpServletRequest request) {
        if (request == null) {
            return getName();
        }
        // HTTP case
        String userName = null;
        try {
            userName = request.getRemoteUser();
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
        }
        if (userName != null) {
            return userName;
        }
        return getName();
    }

    /**
     * Gets the user name.
     *
     * @return the user name
     */
    public static final String getName() {
        // HTTP case
        String userName = null;
        userName = getRemoteUser();

        // Anonymous case
        if (userName == null) {
            userName = getAnonymousUser();
        }

        // Return HTTP, Anonymous or Local case
        return userName != null ? userName : GUEST;
    }

    /**
     * Gets the remote user.
     *
     * @return the remote user
     */
    private static String getRemoteUser() {
        try {
            if (HttpRequestFacade.isValid()) {
                return HttpRequestFacade.getRemoteUser();
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
        }
        return null;
    }

    /**
     * Gets the anonymous user.
     *
     * @return the anonymous user
     */
    private static String getAnonymousUser() {
        String userName = null;
        if (Configuration.isAnonymousModeEnabled()) {
            try {
                userName = getContextProperty(DIRIGIBLE_ANONYMOUS_IDENTIFIER);
            } catch (ContextException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage());
                }
            }
        } else if (Configuration.isAnonymousUserEnabled()) {
            try {
                userName = getContextProperty(DIRIGIBLE_ANONYMOUS_USER);
                if (userName == null) {
                    userName = setAnonymousUser();
                }
            } catch (ContextException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage());
                }
            }
        } else if (Configuration.isJwtModeEnabled()) {
            try {
                userName = getContextProperty(DIRIGIBLE_JWT_USER);
            } catch (ContextException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage());
                }
            }
        }
        return userName;
    }

    /**
     * Gets the context property.
     *
     * @param property the property
     * @return the context property
     * @throws ContextException the context exception
     */
    private static String getContextProperty(String property) throws ContextException {
        if (HttpSessionFacade.isValid()) {
            return HttpSessionFacade.getAttribute(property);
        } else if (ThreadContextFacade.isValid()) {
            Object value = ThreadContextFacade.get(property);
            if ((value != null) && (value instanceof String)) {
                return (String) value;
            }
        }
        return null;
    }

    /**
     * Sets the anonymous user.
     *
     * @return the string
     */
    private static String setAnonymousUser() {
        String userName = null;
        String anonymousUserNamePropertyName = Configuration.get(DIRIGIBLE_ANONYMOUS_USER_NAME_PROPERTY_NAME);
        if (anonymousUserNamePropertyName != null) {
            userName = Configuration.get(anonymousUserNamePropertyName);
            try {
                setName(userName);
            } catch (ContextException e) {
                if (logger.isInfoEnabled()) {
                    logger.info("Error while setting userName from DIRIGIBLE_ANONYMOUS_USER_PROPERTY_NAME.", e);
                }
            }
        }
        return userName;
    }

    /**
     * Sets the user name.
     *
     * @param userName the user name
     * @throws ContextException in case of not initialized ThreadContext
     */
    public static final void setName(String userName) throws ContextException {
        if (Configuration.isAnonymousModeEnabled()) {
            setContextProperty(DIRIGIBLE_ANONYMOUS_IDENTIFIER, userName);
            if (logger.isDebugEnabled()) {
                logger.debug(
                        format("User name has been set programmatically {0} to the session as the anonymous mode is enabled", userName));
            }
        } else if (Configuration.isAnonymousUserEnabled()) {
            setContextProperty(DIRIGIBLE_ANONYMOUS_USER, userName);
            if (logger.isDebugEnabled()) {
                logger.debug(
                        format("User name has been set programmatically {0} to the session as the anonymous mode is enabled", userName));
            }
        } else if (Configuration.isJwtModeEnabled()) {
            setContextProperty(DIRIGIBLE_JWT_USER, userName);
            if (logger.isDebugEnabled()) {
                logger.debug(format("User name has been set programmatically {0} to the session as the JWT mode is enabled", userName));
            }
        } else {
            throw new SecurityException("Setting the user name programmatically is supported only when the anonymous mode is enabled");
        }
    }

    /**
     * Sets the context property.
     *
     * @param property the property
     * @param value the value
     * @throws ContextException the context exception
     */
    private static void setContextProperty(String property, String value) throws ContextException {
        if (HttpSessionFacade.isValid()) {
            HttpSessionFacade.setAttribute(property, value);
        } else {
            if (ThreadContextFacade.isValid()) {
                ThreadContextFacade.set(property, value);
            }
        }
    }

    /**
     * Gets the user name by a given request as parameter.
     *
     * @param session the session
     * @return the user name
     */
    public static final String getName(Session session) {
        if (session == null) {
            return getName();
        }
        // WebSockets case
        String userName = null;
        try {
            userName = session.getUserPrincipal()
                              .getName();
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
        }
        if (userName != null) {
            return userName;
        }
        return getName();
    }

    /**
     * Gets the timeout.
     *
     * @return the timeout
     */
    public static final Integer getTimeout() {
        if (HttpSessionFacade.isValid()) {
            return HttpSessionFacade.getMaxInactiveInterval();
        } else {
            if (logger.isErrorEnabled()) {
                logger.error(NO_VALID_REQUEST);
            }
        }
        return 0;
    }

    /**
     * Gets the auth type.
     *
     * @return the auth type
     */
    public static String getAuthType() {
        if (HttpRequestFacade.isValid()) {
            return HttpRequestFacade.getAuthType();
        } else {
            if (logger.isErrorEnabled()) {
                logger.error(NO_VALID_REQUEST);
            }
        }
        return null;
    }

    /**
     * The Authorization header returns the type + token. Substring from the empty space to only get the
     * token.
     *
     * @return the security token
     */
    public static String getSecurityToken() {
        if (HttpRequestFacade.isValid()) {
            String token = HttpRequestFacade.getHeader(AUTH);
            return token != null && !"".equals(token) ? token.substring(token.indexOf(" ")) : "";
        } else {
            if (logger.isErrorEnabled()) {
                logger.error(NO_VALID_REQUEST);
            }
        }
        return "";
    }

    /**
     * Gets the invocation count.
     *
     * @return the invocation count
     */
    public static String getInvocationCount() {
        if (HttpSessionFacade.isValid()) {
            return HttpSessionFacade.getAttribute(INVOCATION_COUNT);
        } else {
            if (logger.isErrorEnabled()) {
                logger.error(NO_VALID_REQUEST);
            }
        }
        return null;
    }

    /**
     * The accept-language attribute returns multiple values. Eg. en-GB,en-US;q=0.9,en;q=0.8 Substring
     * until the semicolon to get the IETF (BCP 47) format.
     *
     * @return the language
     */
    public static String getLanguage() {
        if (HttpRequestFacade.isValid()) {
            String language = HttpRequestFacade.getHeader(LANGUAGE_HEADER);
            if (language == null || language.isEmpty()) {
                language = ANY_LANGUAGE;
            }
            List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(language);
            return ranges == null || ranges.isEmpty() ? ""
                    : ranges.get(0)
                            .getRange();
        } else {
            if (logger.isErrorEnabled()) {
                logger.error(NO_VALID_REQUEST);
            }
        }
        return null;
    }

    public static Collection<String> getUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return AuthoritiesUtil.toRoleNames(authorities);
    }
}
