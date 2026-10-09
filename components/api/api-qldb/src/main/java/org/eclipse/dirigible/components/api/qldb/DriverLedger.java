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

import com.amazon.ion.*;
import com.amazon.ion.system.IonSystemBuilder;
import com.fasterxml.jackson.dataformat.ion.IonObjectMapper;
import software.amazon.awssdk.services.qldbsession.QldbSessionClient;
import software.amazon.qldb.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * The QLDB ledger operations on the Amazon QLDB driver - the only class of this module that links
 * against the driver, Ion, or the AWS SDK. {@link QLDBRepository} creates it after checking that
 * the driver is present, so a bundle without the add-on never loads it.
 */
final class DriverLedger implements QLDBRepository.Ledger {
    private static final String DOCUMENT_ID_FIELD = QLDBRepository.DOCUMENT_ID_FIELD;
    private static final IonSystem ION_SYSTEM = IonSystemBuilder.standard()
                                                                .build();
    private final String tableName;
    private final QldbDriver qldbDriver;

    DriverLedger(String ledgerName, String tableName) {
        this.tableName = tableName;
        this.qldbDriver = QldbDriver.builder()
                                    .ledger(ledgerName)
                                    .transactionRetryPolicy(RetryPolicy.builder()
                                                                       .maxRetries(3)
                                                                       .build())
                                    .sessionClientBuilder(QldbSessionClient.builder())
                                    .build();
    }

    @Override
    public void createTable() {
        qldbDriver.execute(txn -> {
            txn.execute("CREATE TABLE " + tableName);
            txn.execute("CREATE INDEX ON " + tableName + "(id)");
        });
    }

    @Override
    public void dropTable() {
        qldbDriver.execute(txn -> {
            txn.execute("DROP TABLE " + tableName);
        });
    }

    @Override
    public Map<String, Object> insert(Object entry) {
        return qldbDriver.execute(txn -> {
            IonValue ionEntry = serialize(entry);
            String insertIntoStatement = buildInsertIntoSqlStatement();
            Result result = txn.execute(insertIntoStatement, ionEntry);
            String documentId = getIdFromIonValue(result.iterator()
                                                        .next());
            Map<String, Object> createdEntry = deserialize(ionEntry);
            createdEntry.put(DOCUMENT_ID_FIELD, documentId);
            return createdEntry;
        });
    }

    private String buildInsertIntoSqlStatement() {
        return "INSERT INTO " + tableName + " ?";
    }

    private String getIdFromIonValue(IonValue value) {
        IonStruct struct = (IonStruct) value;
        IonValue documentId = struct.get(DOCUMENT_ID_FIELD);
        return ((IonString) documentId).stringValue();
    }

    @Override
    public List<Map<String, Object>> getAll() {
        return qldbDriver.execute(txn -> {
            String selectAllStatement = buildSelectAllStatement();
            Result transactionResult = txn.execute(selectAllStatement);
            return ionValuesToList(transactionResult);
        });
    }

    private String buildSelectAllStatement() {
        return "SELECT * FROM " + tableName + " BY " + DOCUMENT_ID_FIELD;
    }

    private List<Map<String, Object>> ionValuesToList(Result ionValues) {
        return StreamSupport.stream(ionValues.spliterator(), false)
                            .map(this::deserialize)
                            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getById(String id) {
        return qldbDriver.execute(txn -> {
            IonValue ionId = stringToIonValue(id);
            String getByIdStatement = buildGetByIdStatement();
            Result transactionResult = txn.execute(getByIdStatement, ionId);
            return getExactlyOneItemFromResult(transactionResult);
        });
    }

    private String buildGetByIdStatement() {
        return "SELECT * FROM " + tableName + " BY " + DOCUMENT_ID_FIELD + " WHERE " + DOCUMENT_ID_FIELD + " = ?";
    }

    private Map<String, Object> getExactlyOneItemFromResult(Result result) {
        List<Map<String, Object>> entries = ionValuesToList(result);
        if (entries.size() > 1) {
            throw new IllegalStateException("More than one element found from getById()");
        }
        return entries.get(0);
    }

    private IonValue stringToIonValue(String string) {
        return ION_SYSTEM.newString(string);
    }

    @Override
    public Map<String, Object> update(Map<String, Object> identifiableEntry) {
        return qldbDriver.execute(txn -> {
            IonValue serializedEntry = serialize(identifiableEntry);
            String stringId = QLDBRepository.documentIdOf(identifiableEntry);
            IonValue ionId = stringToIonValue(stringId);
            List<IonValue> parameters = List.of(serializedEntry, ionId);
            String updateStatement = buildUpdateSqlStatement();
            txn.execute(updateStatement, parameters);
            return identifiableEntry;
        });
    }

    private String buildUpdateSqlStatement() {
        return "UPDATE " + tableName + " AS t BY pid SET t = ? WHERE pid = ?";
    }

    @Override
    public String delete(String entryId) {
        return qldbDriver.execute(txn -> {
            IonValue ionId = stringToIonValue(entryId);
            String deleteStatement = buildDeleteSqlStatement();
            Result result = txn.execute(deleteStatement, ionId);
            return getIdFromIonValue(result.iterator()
                                           .next());
        });
    }

    private String buildDeleteSqlStatement() {
        return "DELETE FROM " + tableName + " BY " + DOCUMENT_ID_FIELD + " WHERE " + DOCUMENT_ID_FIELD + " = ?";
    }

    @Override
    public List<Map<String, Object>> getHistory() {
        Result history = qldbDriver.execute(txn -> {
            String getHistoryStatement = buildGetHistorySqlStatement();
            return txn.execute(getHistoryStatement);
        });
        return ionValuesToList(history);
    }

    private String buildGetHistorySqlStatement() {
        return "SELECT * FROM history(" + tableName + ") AS h\n";
    }

    private IonValue serialize(Object entry) {
        try {
            return IonObjectMapper.builder()
                                  .build()
                                  .writeValueAsIonValue(entry);
        } catch (IOException e) {
            throw new QLDBRepositoryException("Could not serialize entry to IonValue", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserialize(IonValue entry) {
        try {
            return IonObjectMapper.builder()
                                  .build()
                                  .readValue(entry, Map.class);
        } catch (IOException e) {
            throw new QLDBRepositoryException("Could not deserialize IonValue to Map<String, Object>", e);
        }
    }
}
