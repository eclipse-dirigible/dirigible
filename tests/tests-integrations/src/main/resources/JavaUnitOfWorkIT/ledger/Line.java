/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package ledger;

import java.math.BigDecimal;
import java.time.Instant;

import org.eclipse.dirigible.sdk.db.Column;
import org.eclipse.dirigible.sdk.db.CreatedAt;
import org.eclipse.dirigible.sdk.db.CreatedBy;
import org.eclipse.dirigible.sdk.db.Entity;
import org.eclipse.dirigible.sdk.db.GeneratedValue;
import org.eclipse.dirigible.sdk.db.GenerationType;
import org.eclipse.dirigible.sdk.db.Id;
import org.eclipse.dirigible.sdk.db.Table;
import org.eclipse.dirigible.sdk.db.UpdatedAt;

/** A document line: points at its header by foreign key and carries the amount the header sums. */
@Entity
@Table(name = "UOW_LINE")
public class Line {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LINE_ID")
    public Integer id;

    @Column(name = "LINE_DOCUMENT")
    public Integer document;

    @Column(name = "LINE_AMOUNT", precision = 16, scale = 2)
    public BigDecimal amount;

    @CreatedAt
    @Column(name = "LINE_CREATED_AT")
    public Instant createdAt;

    @CreatedBy
    @Column(name = "LINE_CREATED_BY", length = 128)
    public String createdBy;

    @UpdatedAt
    @Column(name = "LINE_UPDATED_AT")
    public Instant updatedAt;
}
