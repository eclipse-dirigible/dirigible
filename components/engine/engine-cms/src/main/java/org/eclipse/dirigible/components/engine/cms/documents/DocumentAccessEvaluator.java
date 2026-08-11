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

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.engine.cms.access.CmsAccessGrant;
import org.eclipse.dirigible.components.engine.cms.access.CmsAccessService;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Applies the CMS path grants to the caller of a request.
 * <p>
 * The decision itself belongs to {@link CmsAccessService}; this adds the request-bound part - which
 * roles the caller holds, and when enforcement does not apply at all.
 * <p>
 * <b>System contexts are always allowed.</b> Content seeding, scheduled jobs, workflow delegates
 * and message listeners write to the CMS with no user and no roles behind them; denying them would
 * break document seeding and snapshot generation the moment an administrator creates a single
 * grant. Enforcement therefore engages only for an actual request, is skipped in anonymous mode,
 * and can be switched off wholesale with {@code DIRIGIBLE_CMS_ROLES_ENABLED=false}.
 */
@Component
public class DocumentAccessEvaluator {

    /** Kill switch, carried over from the JavaScript implementation. */
    private static final String DIRIGIBLE_CMS_ROLES_ENABLED = "DIRIGIBLE_CMS_ROLES_ENABLED";

    private final CmsAccessService accessService;

    DocumentAccessEvaluator(CmsAccessService accessService) {
        this.accessService = accessService;
    }

    /**
     * Whether the caller may read the given path.
     *
     * @param path the CMS path
     * @param request the current request, null outside one
     * @return true when reading is allowed
     */
    public boolean isReadable(String path, HttpServletRequest request) {
        return isAllowed(path, CmsAccessGrant.METHOD_READ, request);
    }

    /**
     * Whether the caller may write the given path.
     *
     * @param path the CMS path
     * @param request the current request, null outside one
     * @return true when writing is allowed
     */
    public boolean isWritable(String path, HttpServletRequest request) {
        return isAllowed(path, CmsAccessGrant.METHOD_WRITE, request);
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

    private boolean isAllowed(String path, String method, HttpServletRequest request) {
        if (enforcementDisabled(request)) {
            return true;
        }
        return accessService.isAllowed(path, method, request::isUserInRole);
    }

    private boolean enforcementDisabled(HttpServletRequest request) {
        return request == null || Configuration.isAnonymousModeEnabled()
                || !Boolean.parseBoolean(Configuration.get(DIRIGIBLE_CMS_ROLES_ENABLED, Boolean.TRUE.toString()));
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
