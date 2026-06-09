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
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
class BasicAuthenticationInjector implements AuthenticationInjector {

    @Override
    public String type() {
        return Authentication.TYPE_BASIC;
    }

    @Override
    public ServerRequest inject(ServerRequest request, Authentication auth) {
        BasicAuthCredentials cred = auth.getCredentials();
        if (cred == null) {
            throw new IllegalStateException(
                    "Native app declares basic authentication but security.authentication.credentials is missing in the .native-app file.");
        }
        String user = cred.getUser();
        if (user == null || user.isBlank()) {
            throw new IllegalStateException(
                    "Native app declares basic authentication but security.authentication.credentials.user is missing or blank in the .native-app file"
                            + " (verify the value or its ${...} placeholder resolution).");
        }
        String pass = cred.getPassword();
        if (pass == null || pass.isBlank()) {
            throw new IllegalStateException(
                    "Native app declares basic authentication but security.authentication.credentials.password is missing or blank in the .native-app file"
                            + " (verify the value or its ${...} placeholder resolution).");
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
