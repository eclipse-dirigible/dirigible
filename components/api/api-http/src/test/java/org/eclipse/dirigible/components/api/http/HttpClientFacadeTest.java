/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.dirigible.components.api.http.client.HttpClientRequestOptions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HttpClientFacade#parseOptions(String)}. Every facade method dereferences
 * the parsed options right away, so a missing options document must yield the defaults instead of
 * null - the client Java SDK's no-options overloads (for example
 * {@code org.eclipse.dirigible.sdk.http.HttpClient.get(String)}) pass none.
 */
class HttpClientFacadeTest {

    @Test
    void parseOptionsReturnsDefaultsForNull() {
        HttpClientRequestOptions options = HttpClientFacade.parseOptions(null);

        assertThat(options).isNotNull();
        assertThat(options.isBinary()).isFalse();
        assertThat(options.isSslTrustAllEnabled()).isFalse();
    }

    @Test
    void parseOptionsReturnsDefaultsForBlank() {
        assertThat(HttpClientFacade.parseOptions("   ")).isNotNull();
    }

    @Test
    void parseOptionsReadsTheSuppliedDocument() {
        HttpClientRequestOptions options = HttpClientFacade.parseOptions("{\"binary\":true,\"sslTrustAllEnabled\":true}");

        assertThat(options.isBinary()).isTrue();
        assertThat(options.isSslTrustAllEnabled()).isTrue();
    }

}
