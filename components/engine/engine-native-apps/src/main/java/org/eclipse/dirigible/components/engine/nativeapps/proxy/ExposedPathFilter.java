/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.proxy;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.engine.nativeapps.domain.ExposedPath;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeAppConfig;
import org.eclipse.dirigible.components.engine.nativeapps.domain.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Optional;

/**
 * Enforces {@code security.exposedPaths} for a {@link NativeApp}: requests must hit one of the
 * configured path prefixes (else 404) and the caller must hold one of the required scopes (else
 * 403). If {@code security} or {@code exposedPaths} is null/empty the filter is a pass-through.
 */
@Component
class ExposedPathFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExposedPathFilter.class);

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        NativeApp app = NativeAppProxyAttributes.requireNativeApp(request);
        NativeAppConfig config = app.getConfig();
        Security security = config == null ? null : config.getSecurity();
        List<ExposedPath> exposed = security == null ? List.of() : security.getExposedPaths();
        if (exposed.isEmpty()) {
            return next.handle(request);
        }

        String upstreamPath = request.uri()
                                     .getPath();
        Optional<ExposedPath> match = pickLongestMatching(exposed, upstreamPath);
        if (match.isEmpty()) {
            LOGGER.debug("Path [{}] is not in the exposedPaths whitelist for native app [{}].", LogSanitizer.sanitize(upstreamPath),
                    LogSanitizer.sanitize(app.getName()));
            return ServerResponse.status(HttpStatus.NOT_FOUND)
                                 .body("Native app path is not exposed.");
        }

        if (!callerHoldsAnyScope(request, match.get()
                                               .getScopes())) {
            String user = MvcUtils.getApplicationContext(request) == null ? "anonymous" : safeRemoteUser(request);
            LOGGER.debug("User [{}] denied for path [{}] of native app [{}]; required scopes={}", LogSanitizer.sanitize(user),
                    LogSanitizer.sanitize(upstreamPath), LogSanitizer.sanitize(app.getName()), LogSanitizer.sanitize(match.get()
                                                                                                                          .getScopes()));
            return ServerResponse.status(HttpStatus.FORBIDDEN)
                                 .body("Insufficient scopes for the requested native-app path.");
        }
        return next.handle(request);
    }

    private static Optional<ExposedPath> pickLongestMatching(List<ExposedPath> exposed, String upstreamPath) {
        ExposedPath best = null;
        int bestLen = -1;
        for (ExposedPath candidate : exposed) {
            String prefix = candidate.getPath();
            if (prefix == null || prefix.isEmpty()) {
                continue;
            }
            if (upstreamPath.equals(prefix) || upstreamPath.startsWith(prefix.endsWith("/") ? prefix : prefix + "/")) {
                if (prefix.length() > bestLen) {
                    best = candidate;
                    bestLen = prefix.length();
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Returns whether the caller is allowed past the scope gate.
     *
     * <p>
     * Native-app scope semantics are intentionally strict — the platform's normal DEVELOPER /
     * ADMINISTRATOR super-roles do <strong>not</strong> grant implicit access here. A user with those
     * privileged roles still gets {@code 403} unless they are also explicitly assigned at least one of
     * the configured scopes. The reasoning: a "native app" is a user-published web service whose
     * audience the author defines; the platform's operations roles must not be a back-door into every
     * such service.
     *
     * <p>
     * When {@code scopes} is null or empty, the path is accessible to any authenticated caller —
     * authentication is already enforced upstream by Spring Security on the {@code /services/**}
     * pattern.
     */
    private static boolean callerHoldsAnyScope(ServerRequest request, List<String> scopes) {
        String user = safeRemoteUser(request);
        if (scopes == null || scopes.isEmpty()) {
            LOGGER.debug("Scope check passes for user [{}]: no scopes required, any authenticated caller may proceed.",
                    LogSanitizer.sanitize(user));
            return true;
        }
        HttpServletRequest servletRequest = request.servletRequest();
        for (String scope : scopes) {
            if (servletRequest.isUserInRole(scope)) {
                LOGGER.debug("Scope check passes for user [{}]: caller holds required scope [{}].", LogSanitizer.sanitize(user),
                        LogSanitizer.sanitize(scope));
                return true;
            }
        }
        LOGGER.debug(
                "Scope check fails for user [{}]: caller holds none of the required scopes {}. DEVELOPER/ADMINISTRATOR super-roles do not grant implicit access to native-app paths.",
                LogSanitizer.sanitize(user), LogSanitizer.sanitize(scopes));
        return false;
    }

    private static String safeRemoteUser(ServerRequest request) {
        String user = request.servletRequest()
                             .getRemoteUser();
        return user == null ? "anonymous" : user;
    }
}
