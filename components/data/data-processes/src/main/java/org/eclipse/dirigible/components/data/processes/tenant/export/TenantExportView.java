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

import java.time.Instant;

/**
 * A tenant export as the routes answer it.
 *
 * @param id the export's key
 * @param status where it stands
 * @param requestedBy who started it
 * @param requestedAt when it was started
 * @param completedAt when it ended
 * @param expiresAt when it is deleted
 * @param sizeBytes the archive's size
 * @param archiveName the archive's file name, which says whose data it is and from when
 * @param error why it failed
 */
record TenantExportView(long id, TenantExportStatus status, String requestedBy, Instant requestedAt, Instant completedAt, Instant expiresAt,
        Long sizeBytes, String archiveName, String error) {

    /**
     * The view of an export.
     *
     * @param export the export
     * @return the view
     */
    static TenantExportView of(TenantExport export) {
        return new TenantExportView(export.getId(), export.getStatus(), export.getRequestedBy(), export.getRequestedAt(),
                export.getCompletedAt(), export.getExpiresAt(), export.getSizeBytes(), archiveName(export.getArchivePath()),
                export.getError());
    }

    private static String archiveName(String archivePath) {
        return archivePath == null ? null : archivePath.substring(archivePath.lastIndexOf('/') + 1);
    }
}
