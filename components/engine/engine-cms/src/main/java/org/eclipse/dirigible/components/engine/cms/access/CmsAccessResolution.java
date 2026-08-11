/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * The CMS access semantics, in one place and free of persistence and request plumbing so they can
 * be stated and tested exactly once.
 * <p>
 * There is deliberately ONE definition of "may this caller read this path", because the
 * implementation this replaces had three that disagreed - one requiring every listed role, one
 * requiring any, and one that only ever matched an exact path so a restricted parent folder never
 * hid its children.
 * <p>
 * The rules:
 * <ul>
 * <li><b>Inheritance</b> - a grant on {@code /a} covers {@code /a/**}.</li>
 * <li><b>The most specific path wins</b> - only the grants of the deepest ancestor that has any for
 * the method are considered, so opening one child of a restricted parent is expressible.</li>
 * <li><b>Roles are alternatives</b> - holding ANY granted role suffices.</li>
 * <li><b>No grants means open</b> - an unconstrained path stays readable and writable by any
 * authenticated caller. The CMS is not deny-by-default; a rule is what restricts it.</li>
 * <li><b>Writing implies reading</b> - a caller who may not read a path may not write it
 * either.</li>
 * </ul>
 */
final class CmsAccessResolution {

    private CmsAccessResolution() {}

    /**
     * The grants that decide the given path and method: those of the deepest ancestor (or the path
     * itself) that has any.
     *
     * @param grants every grant of the tenant
     * @param path the CMS path
     * @param method {@code READ} or {@code WRITE}
     * @return the deciding grants, empty when the path is unconstrained
     */
    static List<CmsAccessGrant> deciding(Collection<CmsAccessGrant> grants, String path, String method) {
        List<CmsAccessGrant> deciding = List.of();
        int deepest = -1;
        for (String candidate : ancestry(path)) {
            List<CmsAccessGrant> matching = new ArrayList<>();
            for (CmsAccessGrant grant : grants) {
                if (method.equalsIgnoreCase(grant.method()) && normalize(grant.path()).equals(candidate)) {
                    matching.add(grant);
                }
            }
            if (!matching.isEmpty() && candidate.length() > deepest) {
                deciding = matching;
                deepest = candidate.length();
            }
        }
        return deciding;
    }

    /**
     * Whether the caller may act on the path.
     *
     * @param grants every grant of the tenant
     * @param path the CMS path
     * @param method {@code READ} or {@code WRITE}
     * @param holdsRole tells whether the caller holds a given role
     * @return true when unconstrained, or when the caller holds any deciding role
     */
    static boolean isAllowed(Collection<CmsAccessGrant> grants, String path, String method, Predicate<String> holdsRole) {
        List<CmsAccessGrant> deciding = deciding(grants, path, method);
        if (deciding.isEmpty()) {
            return true;
        }
        for (CmsAccessGrant grant : deciding) {
            if (holdsRole.test(grant.role())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The path and every ancestor of it, root first ({@code /a/b} yields {@code /}, {@code /a},
     * {@code /a/b}).
     *
     * @param path the CMS path
     * @return the ancestry, always including the root
     */
    static List<String> ancestry(String path) {
        List<String> ancestry = new ArrayList<>();
        ancestry.add("/");
        String normalized = normalize(path);
        if ("/".equals(normalized)) {
            return ancestry;
        }
        int separator = normalized.indexOf('/', 1);
        while (separator > 0) {
            ancestry.add(normalized.substring(0, separator));
            separator = normalized.indexOf('/', separator + 1);
        }
        ancestry.add(normalized);
        return ancestry;
    }

    /** A path with exactly one leading separator and no trailing one. */
    static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.trim()
                                .replaceAll("/+", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
