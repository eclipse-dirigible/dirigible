/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.imports;

import java.util.ArrayList;
import java.util.List;

/**
 * The outcome of one import of one file - what the upload answers, what a job calling the import
 * receives, what a post-import hook is handed and what the import ledger records.
 */
public final class ImportResult {

    /** How an import ended. */
    public enum Status {
        /** Every record of the file was saved. */
        IMPORTED,
        /** The file repeats records already imported; nothing was saved. */
        DUPLICATE,
        /** The file could not be read or a record could not be converted; nothing was saved. */
        REJECTED
    }

    private final String importName;
    private final String fileName;
    private final String checksum;
    private final Status status;
    private final int records;
    private final String message;
    private int skipped;
    private final List<String> warnings = new ArrayList<>();

    private ImportResult(String importName, String fileName, String checksum, Status status, int records, String message) {
        this.importName = importName;
        this.fileName = fileName;
        this.checksum = checksum;
        this.status = status;
        this.records = records;
        this.message = message;
    }

    /**
     * A file whose records were all saved.
     *
     * @param importName the declared import
     * @param fileName the file's name
     * @param checksum the file's checksum
     * @param records how many records were saved
     * @return the result
     */
    public static ImportResult imported(String importName, String fileName, String checksum, int records) {
        return new ImportResult(importName, fileName, checksum, Status.IMPORTED, records, null);
    }

    /**
     * A file refused because it repeats records already imported.
     *
     * @param importName the declared import
     * @param fileName the file's name
     * @param checksum the file's checksum
     * @param message which record repeats which key
     * @return the result
     */
    public static ImportResult duplicate(String importName, String fileName, String checksum, String message) {
        return new ImportResult(importName, fileName, checksum, Status.DUPLICATE, 0, message);
    }

    /**
     * A file refused because it could not be read or converted.
     *
     * @param importName the declared import
     * @param fileName the file's name
     * @param checksum the file's checksum
     * @param message why
     * @return the result
     */
    public static ImportResult rejected(String importName, String fileName, String checksum, String message) {
        return new ImportResult(importName, fileName, checksum, Status.REJECTED, 0, message);
    }

    /**
     * A file whose records were all saved, but for the ones the import's {@code where:} filter skipped.
     *
     * @param importName the declared import
     * @param fileName the file's name
     * @param checksum the file's checksum
     * @param records how many records were saved
     * @param skipped how many records of the file the filter skipped
     * @return the result
     */
    public static ImportResult imported(String importName, String fileName, String checksum, int records, int skipped) {
        ImportResult result = new ImportResult(importName, fileName, checksum, Status.IMPORTED, records, null);
        result.skipped = skipped;
        return result;
    }

    /**
     * Record a problem that did not undo the import - a post-import hook that failed after the records
     * were committed.
     *
     * @param warning the problem
     */
    public void addWarning(String warning) {
        warnings.add(warning);
    }

    /**
     * Returns the declared import's name.
     *
     * @return the declared import's name
     */
    public String getImportName() {
        return importName;
    }

    /**
     * Returns the imported file's name.
     *
     * @return the imported file's name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the SHA-256 checksum of the file, in lower-case hex.
     *
     * @return the SHA-256 checksum of the file, in lower-case hex
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Returns how the import ended.
     *
     * @return how the import ended
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Returns how many records were saved - 0 unless {@link Status#IMPORTED}.
     *
     * @return how many records were saved - 0 unless {@link Status#IMPORTED}
     */
    public int getRecords() {
        return records;
    }

    /**
     * Returns how many records of the file the import's {@code where:} filter skipped - 0 unless
     * {@link Status#IMPORTED}.
     *
     * @return how many records of the file the import's {@code where:} filter skipped
     */
    public int getSkipped() {
        return skipped;
    }

    /**
     * Returns why the file was refused, or {@code null} when it was imported.
     *
     * @return why the file was refused, or {@code null} when it was imported
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the problems that did not undo the import, in the order they happened.
     *
     * @return the problems that did not undo the import, in the order they happened
     */
    public List<String> getWarnings() {
        return List.copyOf(warnings);
    }
}
