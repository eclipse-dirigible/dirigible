/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.dependencies;

/**
 * The reported status of one artifact in the dependency report - every artifact the endpoint knows
 * carries exactly one of the {@code STATUS_*} values, so nothing about the dependency layer is ever
 * silent: shadowing, mediation, integrity failures and frozen-mode rejections all surface here.
 *
 * @param coordinate the groupId:artifactId:version coordinate (or the declared id when the
 *        declaration itself failed)
 * @param scope module or platform
 * @param status one of the {@code STATUS_*} values
 * @param message what happened, operator-readable
 */
record ArtifactStatus(String coordinate, String scope, String status, String message) {

    /** Serving in this process. */
    static final String STATUS_ACTIVE = "active";

    /** Takes effect at the next launch. */
    static final String STATUS_PENDING_RESTART = "pending-restart";

    /** The platform provides a different version; the declared one is inert. */
    static final String STATUS_SHADOWED = "shadowed";

    /** More than one version was requested; mediation chose this artifact's. */
    static final String STATUS_MEDIATED = "mediated";

    /** Not activated - declaration, resolution, integrity or activation failure. */
    static final String STATUS_FAILED = "failed";

    /** Rejected in frozen mode - the coordinate is not part of the lockfile. */
    static final String STATUS_FROZEN_MISMATCH = "frozen-mismatch";

    /** The module scope name as reported. */
    static final String SCOPE_MODULE = "module";

    /** The platform scope name as reported. */
    static final String SCOPE_PLATFORM = "platform";

}
