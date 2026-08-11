/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.verifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
    /**
     * Scope (lower-case) → HTTP method (upper-case, or {@code *}) → the access definitions for that
     * bucket, pre-split into exact vs. Ant-pattern paths so the per-request match avoids scanning every
     * definition. Rebuilt wholesale on {@link #refreshCache(boolean)} and published through this
     * {@code volatile} reference; the {@link Bucket} it points at is never mutated after build, so
     * concurrent readers are safe without locking.
     */
    private volatile Map<String, Map<String, Bucket>> cache = Map.of();
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
    @Scheduled(fixedRate = 8_000)
    public void scheduledRefreshCache() {
        refreshCache(false);
    }

    /**
     * Rebuild the access-definition cache from the current set (the scope+method index, each bucket
     * split into exact vs. Ant-pattern paths) and swap it in atomically, so the matcher adapts to
     * dynamically added, changed or removed {@code *.access} definitions.
     *
     * @param force rebuild even when nothing has been flagged as modified
     */
    public void refreshCache(boolean force) {
        if (!force) {
            if (!this.isModified()) {
                return;
            }
        }
        List<Access> all = accessService.getAll();
        // Group by scope + method, then pre-split each bucket into exact and Ant-pattern paths. Access
        // definitions appear/change/disappear dynamically, so this is rebuilt from scratch on every
        // (needed) refresh and swapped in atomically - the machinery adapts to the current set.
        Map<String, Map<String, List<Access>>> byScopeMethod = new HashMap<>();
        for (Access access : all) {
            byScopeMethod.computeIfAbsent(access.getScope()
                                                .toLowerCase(),
                    scope -> new HashMap<>())
                         .computeIfAbsent(access.getMethod()
                                                .toUpperCase(),
                                 method -> new ArrayList<>())
                         .add(access);
        }
        Map<String, Map<String, Bucket>> newCache = new HashMap<>();
        for (Map.Entry<String, Map<String, List<Access>>> scopeEntry : byScopeMethod.entrySet()) {
            Map<String, Bucket> methodBuckets = new HashMap<>();
            for (Map.Entry<String, List<Access>> methodEntry : scopeEntry.getValue()
                                                                         .entrySet()) {
                methodBuckets.put(methodEntry.getKey(), buildBucket(methodEntry.getValue()));
            }
            newCache.put(scopeEntry.getKey(), methodBuckets);
        }
        this.cache = newCache;
        setModified(false);
        logger.debug("Access constraints reloaded");
    }

    /**
     * Split a scope+method's access definitions into an exact-path index (matched by an O(1) lookup)
     * and a list of genuine Ant patterns (matched by {@link AntPathMatcher}), the latter sorted by
     * descending path length so the request-time scan can stop as soon as a candidate is shorter than
     * the best match found so far.
     *
     * @param accesses the definitions of one scope+method bucket
     * @return the pre-split bucket
     */
    private Bucket buildBucket(List<Access> accesses) {
        Map<String, List<Access>> exact = new HashMap<>();
        List<Access> patterns = new ArrayList<>();
        for (Access access : accesses) {
            if (antPathMatcher.isPattern(access.getPath())) {
                patterns.add(access);
            } else {
                exact.computeIfAbsent(access.getPath(), path -> new ArrayList<>())
                     .add(access);
            }
        }
        patterns.sort(Comparator.comparingInt((Access access) -> access.getPath()
                                                                       .length())
                                .reversed());
        return new Bucket(exact, patterns);
    }

    /**
     * One scope+method's access definitions, pre-split so a request avoids matching every definition:
     * an exact-path index for the common literal case, and the residual Ant patterns (longest first).
     *
     * @param exact literal path → its access definitions (an exact request-path lookup is O(1))
     * @param patterns the Ant-pattern definitions, sorted by descending path length
     */
    private record Bucket(Map<String, List<Access>> exact, List<Access> patterns) {
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

        Map<String, Bucket> methodMap = this.cache.get(scope.toLowerCase());

        if (methodMap == null) {
            return List.of();
        }

        Bucket specificMethod = methodMap.get(method.toUpperCase());
        Bucket wildcardMethod = methodMap.get("*");

        if (specificMethod == null && wildcardMethod == null) {
            return List.of();
        }

        // The most specific match wins - the longest matching path string - with equal-length matches
        // returned together (the caller then requires the user to hold a role from any of them).
        List<Access> result = new ArrayList<>();
        int bestLength = -1;

        // Exact paths first: a literal definition for this very path is a definite match, found by an
        // O(1) lookup instead of running the Ant matcher over every definition. All exact hits share the
        // request path's length; a longer Ant pattern (e.g. `/a/b/**` over `/a/b`) can still out-specify
        // them, which the length comparison below preserves.
        bestLength = collectExact(specificMethod, path, result, bestLength);
        bestLength = collectExact(wildcardMethod, path, result, bestLength);

        // Then the genuine Ant patterns (longest first): stop as soon as a candidate is shorter than the
        // best match found - it can neither win nor tie.
        bestLength = collectPatterns(specificMethod, path, method, result, bestLength);
        collectPatterns(wildcardMethod, path, method, result, bestLength);

        if (result.isEmpty()) {
            logger.trace("URI [{}] with HTTP method [{}] is NOT secured", path, method);
        }

        return result;
    }

    /**
     * Add a bucket's exact hit for {@code path} (if any) to {@code result}, keeping only the
     * longest-path matches (an exact hit's length is the request path's length).
     *
     * @return the running best matched path length
     */
    private int collectExact(Bucket bucket, String path, List<Access> result, int bestLength) {
        if (bucket == null) {
            return bestLength;
        }
        List<Access> hits = bucket.exact()
                                  .get(path);
        if (hits == null) {
            return bestLength;
        }
        for (Access securityAccess : hits) {
            bestLength = keepLongest(result, securityAccess, path.length(), bestLength);
        }
        return bestLength;
    }

    /**
     * Match a bucket's Ant patterns (pre-sorted by descending path length) against {@code path},
     * keeping only the longest-path matches and breaking out as soon as a candidate cannot beat or tie
     * the best.
     *
     * @return the running best matched path length
     */
    private int collectPatterns(Bucket bucket, String path, String method, List<Access> result, int bestLength) {
        if (bucket == null) {
            return bestLength;
        }
        for (Access securityAccess : bucket.patterns()) {
            int length = securityAccess.getPath()
                                       .length();
            if (length < bestLength) {
                break; // sorted descending - every remaining pattern is shorter, so none can win or tie
            }
            if (antPathMatcher.match(securityAccess.getPath(), path)) {
                logger.debug("Path [{}] and HTTP method [{}] is secured by definition [{}]", path, method, securityAccess.getLocation());
                bestLength = keepLongest(result, securityAccess, length, bestLength);
            }
        }
        return bestLength;
    }

    /**
     * Keep the most specific matches only: a strictly longer path replaces the collected set, an
     * equal-length path joins it, a shorter one is ignored.
     *
     * @return the resulting best matched path length
     */
    private static int keepLongest(List<Access> result, Access securityAccess, int length, int bestLength) {
        if (length > bestLength) {
            result.clear();
            result.add(securityAccess);
            return length;
        }
        if (length == bestLength) {
            result.add(securityAccess);
        }
        return bestLength;
    }

}
