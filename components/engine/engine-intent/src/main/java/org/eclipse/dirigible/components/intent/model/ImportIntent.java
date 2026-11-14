/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A file import: a user uploads a file - a bank statement, a price list - and every record of it
 * becomes a row of {@link #into}. The inbound twin of a file export.
 *
 * <p>
 * {@link #format} says how the file is read: {@code csv} (with the locale rules of {@link #csv}),
 * {@code mt940} or {@code camt053}. {@link #map} projects what was read onto the entity's fields -
 * a CSV column (by header name, or by 1-based position without a header), or a key of the statement
 * vocabulary the MT940 and camt.053 readers share - and {@code _line} / {@code _file} name the
 * record's position in the file and the file's name for any format:
 *
 * <pre>
 * imports:
 *   - name: bankCsv
 *     into: BankStatementLine
 *     format: csv
 *     encoding: windows-1251
 *     csv: { separator: ";", decimal: ",", grouping: ".", dateFormat: dd.MM.yyyy, skipLines: 1 }
 *     map: { bookingDate: Дата, amount: Сума, details: Основание, statementId: _file, lineNumber: _line }
 *     duplicateKey: [statementId, lineNumber]
 * </pre>
 *
 * {@code camt053: { records: transactions }} reads a camt.053 file one record per transaction
 * instead of one per entry. {@link #where} keeps only the records whose source keys hold the given
 * values ({@code where: { status: BOOK }}; a list allows any of its values) - the rest are skipped
 * and counted, never saved.
 *
 * <p>
 * A file is imported whole or not at all: an unreadable value, or a record repeating the
 * {@link #duplicateKey} of a row already imported (or of another record of the same file), refuses
 * the file and saves nothing. Every attempt is recorded in the entity's import ledger.
 */
public class ImportIntent {

    private String name;
    private String into;
    private String format;
    private String encoding;
    private ImportCsvIntent csv;
    private ImportCamt053Intent camt053;
    private Map<String, Object> where = new LinkedHashMap<>();
    private Map<String, Object> map = new LinkedHashMap<>();
    private List<String> duplicateKey = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInto() {
        return into;
    }

    public void setInto(String into) {
        this.into = into;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public ImportCsvIntent getCsv() {
        return csv;
    }

    public void setCsv(ImportCsvIntent csv) {
        this.csv = csv;
    }

    public ImportCamt053Intent getCamt053() {
        return camt053;
    }

    public void setCamt053(ImportCamt053Intent camt053) {
        this.camt053 = camt053;
    }

    public Map<String, Object> getWhere() {
        return where == null ? new LinkedHashMap<>() : where;
    }

    public void setWhere(Map<String, Object> where) {
        this.where = where;
    }

    public Map<String, Object> getMap() {
        return map == null ? new LinkedHashMap<>() : map;
    }

    public void setMap(Map<String, Object> map) {
        this.map = map;
    }

    public List<String> getDuplicateKey() {
        return duplicateKey == null ? new ArrayList<>() : duplicateKey;
    }

    public void setDuplicateKey(List<String> duplicateKey) {
        this.duplicateKey = duplicateKey;
    }
}
