/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.proxy;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.eclipse.dirigible.components.engine.proxy.domain.Proxy;
import org.eclipse.dirigible.components.engine.proxy.service.ProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class ProxyRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRegistry.class);

    private static final Duration CACHE_EXPIRATION = Duration.ofSeconds(60);

    private final ProxyService proxyService;
    private final LoadingCache<String, Optional<Proxy>> proxiesCache;

    ProxyRegistry(ProxyService proxyService) {
        this.proxyService = proxyService;
        this.proxiesCache = Caffeine.newBuilder()
                                    .expireAfterWrite(CACHE_EXPIRATION)
                                    .maximumSize(50)
                                    .build(proxyService::findOptionalByName);
    }

    public Optional<Proxy> findByName(String proxyName) {
        return proxiesCache.get(proxyName);
    }

    public void register(Proxy proxy) {
        proxiesCache.put(proxy.getName(), Optional.of(proxy));
    }

    public void unregister(Proxy proxy) {
        proxiesCache.invalidate(proxy.getName());
    }

}
