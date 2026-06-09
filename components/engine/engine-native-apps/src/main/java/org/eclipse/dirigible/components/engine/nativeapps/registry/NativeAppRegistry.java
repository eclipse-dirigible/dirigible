/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.registry;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.engine.nativeapps.domain.NativeApp;
import org.eclipse.dirigible.components.engine.nativeapps.service.NativeAppService;
import org.eclipse.dirigible.components.engine.nativeapps.synchronizer.NativeAppParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Read-through cache of {@link NativeApp} entities for the proxy and lifecycle hot path.
 *
 * <p>
 * The {@link org.eclipse.dirigible.components.engine.nativeapps.synchronizer.NativeAppSynchronizer
 * NativeAppSynchronizer} is the single writer (via {@link #register} and {@link #unregister});
 * proxy filters, the monitor job, and the management endpoint read through {@link #findByBasePath}
 * or {@link #findById}.
 */
@Component
public class NativeAppRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeAppRegistry.class);
    private static final int CACHE_MAX_SIZE = 200;

    private final NativeAppService nativeAppService;
    private final Cache<String, NativeApp> byBasePath;
    private final Cache<Long, NativeApp> byId;

    NativeAppRegistry(NativeAppService nativeAppService) {
        this.nativeAppService = nativeAppService;
        Duration ttl = Duration.ofSeconds(DirigibleConfig.NATIVE_APP_REGISTRY_TTL_SECONDS.getIntValue());
        this.byBasePath = Caffeine.newBuilder()
                                  .expireAfterWrite(ttl)
                                  .maximumSize(CACHE_MAX_SIZE)
                                  .build();
        this.byId = Caffeine.newBuilder()
                            .expireAfterWrite(ttl)
                            .maximumSize(CACHE_MAX_SIZE)
                            .build();
    }

    public Optional<NativeApp> findByBasePath(String basePath) {
        String key = normaliseBasePath(basePath);
        NativeApp cached = byBasePath.getIfPresent(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<NativeApp> loaded = nativeAppService.findOptionalByBasePath(key);
        loaded.ifPresent(this::indexIntoCaches);
        return loaded;
    }

    public Optional<NativeApp> findById(Long id) {
        NativeApp cached = byId.getIfPresent(id);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<NativeApp> loaded = nativeAppService.findOptionalById(id);
        loaded.ifPresent(this::indexIntoCaches);
        return loaded;
    }

    public List<NativeApp> findAll() {
        return nativeAppService.getAll();
    }

    public void register(NativeApp app) {
        if (app == null) {
            return;
        }
        indexIntoCaches(app);
        LOGGER.debug("Registered native app [{}] basePath=[{}]", app.getName(), app.getBasePath());
    }

    public void unregister(NativeApp app) {
        if (app == null) {
            return;
        }
        if (app.getBasePath() != null) {
            byBasePath.invalidate(normaliseBasePath(app.getBasePath()));
        }
        if (app.getId() != null) {
            byId.invalidate(app.getId());
        }
        LOGGER.debug("Unregistered native app [{}] basePath=[{}]", app.getName(), app.getBasePath());
    }

    private void indexIntoCaches(NativeApp app) {
        NativeAppParser.rehydrateConfig(app);
        if (app.getBasePath() != null) {
            byBasePath.put(normaliseBasePath(app.getBasePath()), app);
        }
        if (app.getId() != null) {
            byId.put(app.getId(), app);
        }
    }

    /**
     * Trim leading / trailing slashes so {@code "library-native-app-nodejs"} and
     * {@code "/library-native-app-nodejs/"} map to the same cache key.
     */
    static String normaliseBasePath(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
