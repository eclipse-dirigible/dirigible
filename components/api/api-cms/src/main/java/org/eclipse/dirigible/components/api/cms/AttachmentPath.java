/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.cms;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Computes the CMS storage path for a record attachment:
 * {@code /Attachments/<MasterEntity>/<yyyy>/<MM>/<uuid>/<file-name>} - the master entity type, then
 * the upload year and month, then a per-upload uuid folder, then the original file name. The uuid
 * folder makes every upload collision-free while preserving the original file name, and the
 * entity/date prefix keeps the {@code /Attachments} tree browseable in the Documents perspective.
 *
 * <p>
 * Pure and deterministic (date and uuid are supplied by the caller) so it is unit-testable in
 * isolation; {@link AttachmentsFacade} feeds it {@code LocalDate.now()} and a fresh uuid.
 */
public final class AttachmentPath {

    /** Root CMS folder under which all record attachments are stored. */
    public static final String ROOT = "/Attachments";

    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM");

    private AttachmentPath() {}

    /**
     * The folder that holds a single upload: {@code /Attachments/<Master>/<yyyy>/<MM>/<uuid>}.
     *
     * @param masterEntity the owning entity type name (sanitized to a safe path segment)
     * @param date the upload date
     * @param uuid the per-upload identifier
     * @return the folder path (no trailing separator)
     */
    public static String folder(String masterEntity, LocalDate date, String uuid) {
        return ROOT + "/" + segment(masterEntity) + "/" + date.format(YEAR) + "/" + date.format(MONTH) + "/" + segment(uuid);
    }

    /**
     * The full document path for an uploaded file: the {@link #folder(String, LocalDate, String)} plus
     * the (base-)name of the file.
     *
     * @param masterEntity the owning entity type name
     * @param date the upload date
     * @param uuid the per-upload identifier
     * @param fileName the original file name (any directory part is stripped)
     * @return the full CMS document path
     */
    public static String build(String masterEntity, LocalDate date, String uuid, String fileName) {
        return folder(masterEntity, date, uuid) + "/" + fileName(fileName);
    }

    /**
     * Reduce an identifier to a safe single path segment: everything outside {@code [A-Za-z0-9_-]}
     * becomes {@code _}. Guards against a path-traversal or a separator sneaking into the master name
     * or uuid.
     *
     * @param value the raw value
     * @return the sanitized segment ({@code _} when blank)
     */
    static String segment(String value) {
        if (value == null || value.isBlank()) {
            return "_";
        }
        return value.trim()
                    .replaceAll("[^A-Za-z0-9_-]", "_");
    }

    /**
     * The base file name with any directory part and path separators removed, so an uploaded name can
     * never escape its uuid folder. The rest of the name (spaces, dots, unicode) is preserved.
     *
     * @param name the original file name
     * @return the safe base file name ({@code file} when blank)
     */
    static String fileName(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }
        String base = name.trim()
                          .replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.strip();
        return base.isEmpty() ? "file" : base;
    }
}
