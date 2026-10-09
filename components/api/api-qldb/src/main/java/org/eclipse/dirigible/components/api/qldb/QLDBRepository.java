/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.qldb;

import java.util.List;
import java.util.Map;

/**
 * A table of an Amazon QLDB ledger, behind {@code @aerokit/sdk/qldb} and
 * {@code org.eclipse.dirigible.sdk.qldb.Qldb}.
 * <p>
 * The QLDB driver is an add-on that the default bundle does not ship (#7783): add
 * {@code org.eclipse.dirigible:dirigible-components-api-qldb-driver} ({@code <type>pom</type>}) to
 * the application. Without it the constructor throws {@link QldbNotAvailableException}. The
 * signatures here carry no driver type, so the JavaScript host can introspect the class either way.
 */
public class QLDBRepository {
    public static final String DOCUMENT_ID_FIELD = "documentId";

    /** The class whose presence means the driver is bundled. */
    static final String DRIVER_CLASS = "software.amazon.qldb.QldbDriver";

    private final String tableName;
    private final String ledgerName;
    private final Ledger ledger;

    /**
     * Opens a table of a ledger.
     *
     * @param ledgerName the ledger name
     * @param tableName the table name
     * @throws QldbNotAvailableException when the QLDB driver is not bundled
     */
    public QLDBRepository(String ledgerName, String tableName) {
        requireDriver();
        this.ledgerName = ledgerName;
        this.tableName = tableName;
        this.ledger = new DriverLedger(ledgerName, tableName);
    }

    public void createTable() {
        ledger.createTable();
    }

    public void dropTable() {
        ledger.dropTable();
    }

    public Map<String, Object> insert(Object entry) {
        return ledger.insert(entry);
    }

    public List<Map<String, Object>> getAll() {
        return ledger.getAll();
    }

    public Map<String, Object> getById(String id) {
        return ledger.getById(id);
    }

    public Map<String, Object> update(Map<String, Object> identifiableEntry) {
        return ledger.update(identifiableEntry);
    }

    public String delete(String entryId) {
        return ledger.delete(entryId);
    }

    public String delete(Map<String, Object> identifiableEntry) {
        return delete(documentIdOf(identifiableEntry));
    }

    public List<Map<String, Object>> getHistory() {
        return ledger.getHistory();
    }

    public String getLedgerName() {
        return ledgerName;
    }

    public String getTableName() {
        return tableName;
    }

    static String documentIdOf(Map<String, Object> identifiableEntry) {
        String stringId = (String) identifiableEntry.get(DOCUMENT_ID_FIELD);
        if (stringId == null) {
            throw new QLDBRepositoryException("Argument identifiableEntry must have a documentId field set");
        }
        return stringId;
    }

    /**
     * Checks that the QLDB driver is on the classpath.
     *
     * @throws QldbNotAvailableException when it is not
     */
    private static void requireDriver() {
        try {
            Class.forName(DRIVER_CLASS, false, QLDBRepository.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            throw new QldbNotAvailableException(e);
        }
    }

    /**
     * The ledger operations, implemented on the driver by {@link DriverLedger}.
     */
    interface Ledger {

        void createTable();

        void dropTable();

        Map<String, Object> insert(Object entry);

        List<Map<String, Object>> getAll();

        Map<String, Object> getById(String id);

        Map<String, Object> update(Map<String, Object> identifiableEntry);

        String delete(String entryId);

        List<Map<String, Object>> getHistory();
    }
}
