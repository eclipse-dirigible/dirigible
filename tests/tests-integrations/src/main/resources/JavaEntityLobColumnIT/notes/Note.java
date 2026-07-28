/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package notes;

import org.eclipse.dirigible.sdk.db.Column;
import org.eclipse.dirigible.sdk.db.CreatedAt;
import org.eclipse.dirigible.sdk.db.Entity;
import org.eclipse.dirigible.sdk.db.GeneratedValue;
import org.eclipse.dirigible.sdk.db.GenerationType;
import org.eclipse.dirigible.sdk.db.Id;
import org.eclipse.dirigible.sdk.db.Lob;
import org.eclipse.dirigible.sdk.db.Table;

/**
 * Maps the same table as tables/note.table, whose NOTE_TEXT column is a CLOB. Without the
 * {@code @Lob} the property would be mapped as a VARCHAR of the annotation's default length and the
 * entity layer would resize the column to match.
 */
@Entity
@Table(name = "SCHEMA_FIRST_NOTE")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTE_ID")
    public Integer id;

    @Lob
    @Column(name = "NOTE_TEXT")
    public String text;

    @CreatedAt
    @Column(name = "NOTE_CREATED_AT")
    public java.time.Instant createdAt;
}
