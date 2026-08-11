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

/**
 * One grant: a role may read or write a CMS path (and, by inheritance, everything under it).
 *
 * @param path the CMS path the grant applies to
 * @param method {@code READ} or {@code WRITE}
 * @param role the role that is granted
 */
public record CmsAccessGrant(String path, String method, String role) {

    /** Read access. */
    public static final String METHOD_READ = "READ";

    /** Write access. */
    public static final String METHOD_WRITE = "WRITE";
}
