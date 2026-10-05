/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.access;

import java.util.Locale;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.commons.config.InvalidConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * What a browser is told and refused on whichever security chain the deployment runs: the response
 * headers that keep the platform's pages from being framed, sniffed or leaking their URLs, and the
 * {@link CrossSiteRequestFilter} that refuses a state-changing request another site's page sends in
 * a logged in user's name.
 *
 * <p>
 * Every chain applies the custom configurators before its URL matrix, so one bean reaches the
 * basic, snowflake and OAuth2 login chains alike; the chains no longer decide their frame options
 * themselves. Spring's other default headers ({@code X-Content-Type-Options: nosniff}, the cache
 * control of authenticated responses) stay as they are. The filter takes the slot of the CSRF
 * filter, which every chain disables, so it runs after CORS - a preflight is answered first - and
 * before logout and every authentication.
 */
@Component
class BrowserSecurityConfigurator implements CustomSecurityConfigurator {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserSecurityConfigurator.class);

    /**
     * Configure.
     *
     * @param http the http
     * @throws Exception the exception
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.headers(this::configureHeaders);
        if (DirigibleConfig.SECURITY_CROSS_SITE_PROTECTION.getBooleanValue()) {
            http.addFilterAt(new CrossSiteRequestFilter(), CsrfFilter.class);
        } else {
            LOGGER.warn("Cross-site request protection is disabled by [{}]: a page of any site a logged in user visits may post to the"
                    + " platform in that user's name.", DirigibleConfig.SECURITY_CROSS_SITE_PROTECTION.getKey());
        }
    }

    void configureHeaders(HeadersConfigurer<HttpSecurity> headers) {
        String frameOptions = DirigibleConfig.SECURITY_FRAME_OPTIONS.getStringValue();
        switch (normalized(frameOptions)) {
            case "SAMEORIGIN" -> headers.frameOptions(options -> options.sameOrigin());
            case "DENY" -> headers.frameOptions(options -> options.deny());
            case "DISABLED" -> headers.frameOptions(options -> options.disable());
            default -> throw new InvalidConfigException(
                    "Unsupported frame options [" + frameOptions + "], supported are SAMEORIGIN, DENY" + " and DISABLED",
                    DirigibleConfig.SECURITY_FRAME_OPTIONS.getKey());
        }

        String hsts = DirigibleConfig.SECURITY_HSTS.getStringValue();
        switch (normalized(hsts)) {
            // Spring's default: secure requests only, a year, subdomains included
            case "AUTO" -> {
            }
            case "ALWAYS" -> headers.httpStrictTransportSecurity(options -> options.requestMatcher(AnyRequestMatcher.INSTANCE));
            case "OFF" -> headers.httpStrictTransportSecurity(options -> options.disable());
            default -> throw new InvalidConfigException("Unsupported HSTS mode [" + hsts + "], supported are auto, always and off",
                    DirigibleConfig.SECURITY_HSTS.getKey());
        }

        String referrerPolicy = DirigibleConfig.SECURITY_REFERRER_POLICY.getStringValue();
        if (StringUtils.hasText(referrerPolicy)) {
            ReferrerPolicy policy = ReferrerPolicy.get(referrerPolicy.trim()
                                                                     .toLowerCase(Locale.ROOT));
            if (policy == null) {
                throw new InvalidConfigException("Unsupported referrer policy [" + referrerPolicy + "]",
                        DirigibleConfig.SECURITY_REFERRER_POLICY.getKey());
            }
            headers.referrerPolicy(options -> options.policy(policy));
        }

        String contentSecurityPolicy = DirigibleConfig.SECURITY_CONTENT_SECURITY_POLICY.getStringValue();
        if (StringUtils.hasText(contentSecurityPolicy)) {
            boolean reportOnly = DirigibleConfig.SECURITY_CONTENT_SECURITY_POLICY_REPORT_ONLY.getBooleanValue();
            headers.contentSecurityPolicy(options -> {
                options.policyDirectives(contentSecurityPolicy.trim());
                if (reportOnly) {
                    options.reportOnly();
                }
            });
        }

        String permissionsPolicy = DirigibleConfig.SECURITY_PERMISSIONS_POLICY.getStringValue();
        if (StringUtils.hasText(permissionsPolicy)) {
            headers.permissionsPolicyHeader(options -> options.policy(permissionsPolicy.trim()));
        }
    }

    private static String normalized(String value) {
        return value == null ? ""
                : value.trim()
                       .toUpperCase(Locale.ROOT);
    }
}
