/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.uri;

import java.util.Collection;
import java.util.List;

/**
 * SPI implemented by an optional, hosted engine to declare the URL prefixes it serves, so the core
 * platform can secure and filter those prefixes without naming them.
 *
 * <p>
 * An engine that is packaged outside the platform (e.g. the extracted OData engine) contributes a
 * Spring {@code @Component} implementing this interface. The platform collects every implementation
 * on the classpath and merges the declared patterns into the security chain and the request
 * filters. When no such engine is present, no patterns are contributed and the platform stays
 * unaware of the prefix.
 */
public interface HostedEngineUris {

    /**
     * Ant-style patterns that must require authentication in the main Spring Security chain (e.g.
     * {@code "/odata/**"}).
     *
     * @return the secured Ant patterns, never {@code null}
     */
    default Collection<String> securedAntPatterns() {
        return List.of();
    }

    /**
     * Servlet-style URL patterns the platform request and security filters must cover (e.g.
     * {@code "/odata/v2/*"}).
     *
     * @return the servlet filter URL patterns, never {@code null}
     */
    default Collection<String> filterUrlPatterns() {
        return List.of();
    }
}
