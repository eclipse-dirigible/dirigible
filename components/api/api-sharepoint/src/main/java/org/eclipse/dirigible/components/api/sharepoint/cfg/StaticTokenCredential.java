/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.sharepoint.cfg;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * A testing class which will could be used for local testing. A token could be got from the Graph
 * Explorer https://developer.microsoft.com/en-us/graph/graph-explorer credentials
 */
class StaticTokenCredential implements TokenCredential {

    private final String token;

    public StaticTokenCredential(String token) {
        this.token = token;
    }

    @Override
    public Mono<AccessToken> getToken(TokenRequestContext request) {
        return Mono.just(new AccessToken(token, OffsetDateTime.now()
                                                              .plusHours(1)));
    }
}
