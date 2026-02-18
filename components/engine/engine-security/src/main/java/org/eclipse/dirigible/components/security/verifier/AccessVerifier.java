/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.verifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.components.security.domain.Access;
import org.eclipse.dirigible.components.security.service.AccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import jakarta.annotation.PostConstruct;

/**
 * Utility class that checks whether the location is secured via the *.access file
 */

@Component
public class AccessVerifier {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(AccessVerifier.class);

    private final AntPathMatcher antPathMatcher;
    private volatile Map<String, Map<String, List<Access>>> cache = Map.of();
    private final AtomicBoolean modified;

    private final AccessService accessService;
    private final SynchronizationWatcher synchronizationWatcher;

    AccessVerifier(AccessService accessService, SynchronizationWatcher synchronizationWatcher) {
        this.accessService = accessService;
        this.synchronizationWatcher = synchronizationWatcher;
        this.antPathMatcher = new AntPathMatcher();
        this.modified = new AtomicBoolean(false);
        refreshCache(true);
    }

    @PostConstruct
    @Scheduled(fixedRate = 30_000)
    public void scheduledRefreshCache() {
        refreshCache(false);
    }

    public void refreshCache(boolean force) {
        if (!force) {
            if (!this.isModified()) {
                return;
            }
        }
        List<Access> all = accessService.getAll();
        Map<String, Map<String, List<Access>>> newCache = all.stream()
                                                             .collect(Collectors.groupingBy(a -> a.getScope()
                                                                                                  .toLowerCase(),
                                                                     Collectors.groupingBy(a -> a.getMethod()
                                                                                                 .toUpperCase())));
        this.cache = newCache;
        setModified(false);
        logger.debug("Access constraints reloaded");
    }

    @PostConstruct
    @Scheduled(fixedRate = 5_000)
    public void scheduledRefreshModified() {
        if (!this.isModified()) {
            if (this.synchronizationWatcher.isModified()) {
                setModified(true);
                logger.debug("Access constraints is scheduled for reloading...");
            }
        }
    }

    /**
     * Checks if is modified.
     *
     * @return true, if is modified
     */
    public boolean isModified() {
        return this.modified.get();
    }

    /**
     * set modified flag.
     */
    public void setModified(boolean modified) {
        this.modified.set(modified);
    }

    /**
     * Checks whether the URI is secured via the *.access file or not
     *
     * @param scope the scope
     * @param path the path
     * @param method the method
     * @return all the most specific security access entry matching the URI if any
     */
    public List<Access> getMatchingSecurityAccesses(String scope, String path, String method) {

        Map<String, Map<String, List<Access>>> localCache = this.cache;

        Map<String, List<Access>> methodMap = localCache.get(scope.toLowerCase());

        if (methodMap == null) {
            return List.of();
        }

        List<Access> candidates = new ArrayList<>();

        List<Access> specificMethod = methodMap.get(method.toUpperCase());
        List<Access> wildcardMethod = methodMap.get("*");

        if (specificMethod != null) {
            candidates.addAll(specificMethod);
        }

        if (wildcardMethod != null) {
            candidates.addAll(wildcardMethod);
        }

        if (candidates.isEmpty()) {
            return List.of();
        }

        Access currentSecurityAccess = null;
        List<Access> result = new ArrayList<>();

        for (Access securityAccess : candidates) {

            if (antPathMatcher.match(securityAccess.getPath(), path)) {
                logger.debug("Path [{}] and HTTP method [{}] is secured by definition [{}]", path, method, securityAccess.getLocation());
                if (currentSecurityAccess == null || securityAccess.getPath()
                                                                   .length() > currentSecurityAccess.getPath()
                                                                                                    .length()) {

                    currentSecurityAccess = securityAccess;
                    result.clear();
                    result.add(securityAccess);
                } else if (securityAccess.getPath()
                                         .length() == currentSecurityAccess.getPath()
                                                                           .length()) {
                    result.add(securityAccess);
                }
            }
        }

        if (result.isEmpty()) {
            logger.trace("URI [{}] with HTTP method {}] is NOT secured", path, method);
        }

        return result;
    }

}
