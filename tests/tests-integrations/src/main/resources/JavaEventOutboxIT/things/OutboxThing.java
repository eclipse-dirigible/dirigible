/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package things;

import org.eclipse.dirigible.sdk.db.Column;
import org.eclipse.dirigible.sdk.db.Entity;
import org.eclipse.dirigible.sdk.db.GeneratedValue;
import org.eclipse.dirigible.sdk.db.GenerationType;
import org.eclipse.dirigible.sdk.db.Id;
import org.eclipse.dirigible.sdk.db.Table;

/** Maps the same table as tables/outbox_thing.table. */
@Entity
@Table(name = "EVENT_OUTBOX_THING")
public class OutboxThing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "THING_ID")
    public Integer id;

    @Column(name = "THING_NAME")
    public String name;
}
