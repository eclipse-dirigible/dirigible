/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.qldbsession.QldbSessionClient;
import software.amazon.qldb.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class QLDBRepository {
    public static final String DOCUMENT_ID_FIELD = "documentId";
    private static final IonSystem ION_SYSTEM = IonSystemBuilder.standard()
                                                                .build();
    private final String tableName;
    private final String ledgerName;
    private final QldbDriver qldbDriver;

    public QLDBRepository(String ledgerName, String tableName) {
        this.ledgerName = ledgerName;
        this.tableName = tableName;
        this.qldbDriver = QldbDriver.builder()
                                    .ledger(ledgerName)
                                    .transactionRetryPolicy(RetryPolicy.builder()
                                                                       .maxRetries(3)
                                                                       .build())
                                    .sessionClientBuilder(QldbSessionClient.builder())
                                    .build();
    }

    public void createTable() {
        qldbDriver.execute(txn -> {
            txn.execute("CREATE TABLE " + tableName);
            txn.execute("CREATE INDEX ON " + tableName + "(id)");
        });
    }

    public void dropTable() {
        qldbDriver.execute(txn -> {
            txn.execute("DROP TABLE " + tableName);
        });
    }

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

    public Map<String, Object> update(Map<String, Object> identifiableEntry) {
        return qldbDriver.execute(txn -> {
            IonValue serializedEntry = serialize(identifiableEntry);
            String stringId = getDocumentIdFromIdentifiableEntry(identifiableEntry);
            IonValue ionId = stringToIonValue(stringId);
            List<IonValue> parameters = List.of(serializedEntry, ionId);
            String updateStatement = buildUpdateSqlStatement();
            txn.execute(updateStatement, parameters);
            return identifiableEntry;
        });
    }

    private String getDocumentIdFromIdentifiableEntry(Map<String, Object> identifiableEntry) {
        String stringId = (String) identifiableEntry.get(DOCUMENT_ID_FIELD);
        if (stringId == null) {
            throw new QLDBRepositoryException("Argument identifiableEntry must have a documentId field set");
        }
        return stringId;
    }

    private String buildUpdateSqlStatement() {
        return "UPDATE " + tableName + " AS t BY pid SET t = ? WHERE pid = ?";
    }

    public String delete(String entryId) {
        return qldbDriver.execute(txn -> {
            IonValue ionId = stringToIonValue(entryId);
            String deleteStatement = buildDeleteSqlStatement();
            Result result = txn.execute(deleteStatement, ionId);
            return getIdFromIonValue(result.iterator()
                                           .next());
        });
    }

    public String delete(Map<String, Object> identifiableEntry) {
        String stringId = getDocumentIdFromIdentifiableEntry(identifiableEntry);
        return delete(stringId);
    }

    private String buildDeleteSqlStatement() {
        return "DELETE FROM " + tableName + " BY " + DOCUMENT_ID_FIELD + " WHERE " + DOCUMENT_ID_FIELD + " = ?";
    }

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

    public String getLedgerName() {
        return ledgerName;
    }

    public String getTableName() {
        return tableName;
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
