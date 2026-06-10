/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.qldb;

import org.eclipse.dirigible.components.api.qldb.QLDBRepository;

/**
 * Entry point to Amazon QLDB (Quantum Ledger Database) — the platform's append-only,
 * cryptographically-verifiable ledger backend. {@link #open(String, String)} returns a
 * {@link QLDBRepository} pinned to a specific ledger / table; the returned object exposes the full
 * insert / get / update / delete / history surface.
 * <p>
 * Suitable for any data set that must keep an audit trail of every change (financial ledgers,
 * regulatory submissions, certified document chains). For regular CRUD without the audit trail,
 * prefer {@link org.eclipse.dirigible.sdk.db.Database Database} on a traditional RDBMS.
 */
public final class Qldb {

    private Qldb() {}

    public static QLDBRepository open(String ledgerName, String tableName) {
        return new QLDBRepository(ledgerName, tableName);
    }
}
