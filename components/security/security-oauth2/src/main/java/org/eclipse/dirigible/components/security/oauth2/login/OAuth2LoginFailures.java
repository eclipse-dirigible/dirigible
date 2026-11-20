/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.login;

import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.util.StringUtils;

/**
 * Makes a sign-in the identity provider refused say why, on the OAuth2 login profiles (#8801): the
 * refusal is logged once with the provider's error and description, and the login page - the
 * generated one, or the application's own ({@code DIRIGIBLE_SECURITY_LOGIN_PAGE}) through
 * {@code GET /login/oauth2/failure} - shows the provider's sentence instead of
 * {@code Invalid credentials}.
 */
public final class OAuth2LoginFailures {

    /** The page Spring Security generates when no login page is configured. */
    private static final String GENERATED_LOGIN_PAGE = "/login";

    private OAuth2LoginFailures() {}

    /**
     * Installs the failure handling on the chain's {@code oauth2Login}.
     *
     * <p>
     * The handler is set on the login filter by a post-processor rather than through
     * {@code oauth2Login().failureHandler(...)}: a custom handler clears the configurer's failure URL,
     * and the generated page then no longer recognises {@code /login?error} as its error page.
     *
     * @param http the chain
     * @param loginPage the configured login page, blank for the generated one
     * @param messages turns the provider's error into the sentence the person reads
     * @throws Exception when the chain cannot be configured
     */
    public static void configure(HttpSecurity http, String loginPage, OAuth2LoginFailureMessages messages) throws Exception {
        boolean customLoginPage = StringUtils.hasText(loginPage);
        OAuth2LoginFailureHandler failureHandler =
                new OAuth2LoginFailureHandler((customLoginPage ? loginPage : GENERATED_LOGIN_PAGE) + "?error", messages);
        http.oauth2Login(oauth2 -> oauth2.withObjectPostProcessor(new ObjectPostProcessor<OAuth2LoginAuthenticationFilter>() {
            @Override
            public <O extends OAuth2LoginAuthenticationFilter> O postProcess(O filter) {
                filter.setAuthenticationFailureHandler(failureHandler);
                return filter;
            }
        }));
        if (!customLoginPage) {
            http.addFilterBefore(new GeneratedLoginPageFailureFilter(), DefaultLoginPageGeneratingFilter.class);
        }
    }
}
