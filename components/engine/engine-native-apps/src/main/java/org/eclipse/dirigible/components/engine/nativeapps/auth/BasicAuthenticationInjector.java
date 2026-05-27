/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.auth;

import org.eclipse.dirigible.components.engine.nativeapps.domain.Authentication;
import org.eclipse.dirigible.components.engine.nativeapps.domain.BasicAuthCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class BasicAuthenticationInjector implements AuthenticationInjector {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicAuthenticationInjector.class);

    @Override
    public String type() {
        return Authentication.TYPE_BASIC;
    }

    @Override
    public ServerRequest inject(ServerRequest request, Authentication auth) {
        BasicAuthCredentials cred = auth == null ? null : auth.getCredentials();
        if (cred == null) {
            LOGGER.debug("Basic auth requested but no credentials configured; request will be forwarded unchanged.");
            return request;
        }
        String user = cred.getUser();
        String pass = cred.getPassword();
        if (user == null || pass == null) {
            LOGGER.debug("Basic auth credentials missing user or pass; request will be forwarded unchanged.");
            return request;
        }
        String encoded = Base64.getEncoder()
                               .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
        // Use headers(Consumer) to REPLACE any inbound Authorization header — builder.header(...)
        // only appends, which would leave the inbound credentials in front of ours.
        return ServerRequest.from(request)
                            .headers(h -> h.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded))
                            .build();
    }
}
