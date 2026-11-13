/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.tenant.export;

/**
 * Where a tenant export stands.
 */
public enum TenantExportStatus {

    /** Asked for; its process has not taken it up yet. */
    REQUESTED,

    /** Its process is exporting the tables or packing the archive. */
    RUNNING,

    /** The archive is stored and can be downloaded until it expires. */
    COMPLETED,

    /** It ended without an archive; the row says why. */
    FAILED
}
