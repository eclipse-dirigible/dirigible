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

/**
 * How a {@code camt053} import reads its file: {@code records: entries} (the default) makes one
 * record per {@code Ntry}; {@code records: transactions} makes one per
 * {@code Ntry/NtryDtls/TxDtls}, so a batch-booked entry yields one record per payer.
 */
public class ImportCamt053Intent {

    private String records;

    public String getRecords() {
        return records;
    }

    public void setRecords(String records) {
        this.records = records;
    }
}
