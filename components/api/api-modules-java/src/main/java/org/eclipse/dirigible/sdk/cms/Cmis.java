/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.cms;

import jakarta.servlet.ServletException;
import java.util.Set;
import org.eclipse.dirigible.components.api.cms.CmisFacade;
import org.eclipse.dirigible.components.security.domain.Access;

/**
 * Entry point into the platform's CMIS repository — internal store by default, S3 or SharePoint
 * when those engines are wired in. {@link #getSession()} returns the underlying Apache Chemistry
 * {@code Session} so callers can use the full CMIS 1.1 surface (create / read / update / delete
 * documents and folders, query with CMIS-SQL, manage versions).
 * <p>
 * The session is returned as {@link Object} to avoid pulling Apache Chemistry types into the SDK
 * compile surface; cast it to {@code org.apache.chemistry.opencmis.client.api.Session} at the call
 * site.
 * <p>
 * Access-control questions can be answered ahead of time with {@link #isAllowed(String, String)}
 * and {@link #getAccessDefinitions(String, String)} — useful when an authorisation outcome needs to
 * be reported before attempting the operation.
 */
public final class Cmis {

    public static final String METHOD_READ = CmisFacade.CMIS_METHOD_READ;
    public static final String METHOD_WRITE = CmisFacade.CMIS_METHOD_WRITE;

    private Cmis() {}

    public static Object getSession() {
        return CmisFacade.getSession();
    }

    public static Object getVersioningState(String state) {
        return CmisFacade.getVersioningState(state);
    }

    public static Object getUnifiedObjectDelete() {
        return CmisFacade.getUnifiedObjectDelete();
    }

    public static boolean isAllowed(String path, String method) {
        return CmisFacade.isAllowed(path, method);
    }

    public static Set<Access> getAccessDefinitions(String path, String method) throws ServletException {
        return CmisFacade.getAccessDefinitions(path, method);
    }
}
