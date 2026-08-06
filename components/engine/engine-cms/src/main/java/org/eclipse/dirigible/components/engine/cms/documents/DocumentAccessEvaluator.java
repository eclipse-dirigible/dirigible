/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.documents;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.security.domain.Access;
import org.eclipse.dirigible.components.security.verifier.AccessVerifier;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Answers whether the caller may read or write a CMS path, from the {@code CMIS}-scoped access
 * constraints.
 * <p>
 * A path inherits the constraints of every ancestor; holding ANY granted role suffices; a path with
 * no constraints is open to any authenticated caller. Contexts without a request - synchronizers
 * seeding content, scheduled jobs, workflow delegates - are always allowed: they act as the system,
 * not as a user, and denying them would break content seeding the moment a rule exists.
 */
@Component
public class DocumentAccessEvaluator {

    /** Kill switch, honoured for compatibility with the JavaScript implementation. */
    private static final String DIRIGIBLE_CMS_ROLES_ENABLED = "DIRIGIBLE_CMS_ROLES_ENABLED";

    private static final String SCOPE_CMIS = "CMIS";
    private static final String METHOD_READ = "READ";
    private static final String METHOD_WRITE = "WRITE";

    private final AccessVerifier accessVerifier;

    DocumentAccessEvaluator(AccessVerifier accessVerifier) {
        this.accessVerifier = accessVerifier;
    }

    /**
     * Whether the caller may read the given path.
     *
     * @param path the CMS path
     * @param request the current request, null outside one
     * @return true when reading is allowed
     */
    public boolean isReadable(String path, HttpServletRequest request) {
        if (enforcementDisabled(request)) {
            return true;
        }
        List<Access> constraints = constraints(path, METHOD_READ);
        return constraints.isEmpty() || holdsAnyRole(constraints, request);
    }

    /**
     * Whether the caller may write the given path.
     *
     * @param path the CMS path
     * @param request the current request, null outside one
     * @return true when writing is allowed
     */
    public boolean isWritable(String path, HttpServletRequest request) {
        if (enforcementDisabled(request)) {
            return true;
        }
        if (!isReadable(path, request)) {
            return false;
        }
        List<Access> constraints = constraints(path, METHOD_WRITE);
        return constraints.isEmpty() || holdsAnyRole(constraints, request);
    }

    /**
     * The flags the Documents user interfaces render per child.
     *
     * @param path the CMS path
     * @param request the current request, null outside one
     * @return readable + readOnly for that path
     */
    public AccessFlags flags(String path, HttpServletRequest request) {
        boolean readable = isReadable(path, request);
        return new AccessFlags(readable, readable && !isWritable(path, request));
    }

    private boolean enforcementDisabled(HttpServletRequest request) {
        return request == null || Configuration.isAnonymousModeEnabled()
                || !Boolean.parseBoolean(Configuration.get(DIRIGIBLE_CMS_ROLES_ENABLED, Boolean.TRUE.toString()));
    }

    /** The constraints of the path and of every ancestor of it. */
    private List<Access> constraints(String path, String method) {
        List<Access> matching = new ArrayList<>();
        String normalized = path == null || path.isBlank() ? "/" : path;
        int separator = 0;
        do {
            String ancestor = normalized;
            separator = normalized.indexOf('/', separator + 1);
            if (separator > 0) {
                ancestor = normalized.substring(0, separator);
            }
            matching.addAll(accessVerifier.getMatchingSecurityAccesses(SCOPE_CMIS, ancestor, method));
        } while (separator > 0);
        return matching;
    }

    private boolean holdsAnyRole(List<Access> constraints, HttpServletRequest request) {
        for (Access constraint : constraints) {
            if (request.isUserInRole(constraint.getRole())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Per-path access flags.
     *
     * @param readable whether the caller may read the path
     * @param readOnly whether the caller may read but not write it
     */
    public record AccessFlags(boolean readable, boolean readOnly) {
    }
}
