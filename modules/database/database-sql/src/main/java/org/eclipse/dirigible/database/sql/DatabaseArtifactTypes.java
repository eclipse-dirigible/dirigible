/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql;

/**
 * The Database Artifacts Types.
 */
public interface DatabaseArtifactTypes {

    /** The Constant TABLE. */
    public static final int TABLE = 1;

    /** The Constant VIEW. */
    public static final int VIEW = 2;

    /** The Constant PROCEDURE. */
    public static final int PROCEDURE = 3;

    /** The Constant FUNCTION. */
    public static final int FUNCTION = 4;

    /** The Constant SEQUENCE. */
    public static final int SEQUENCE = 5;

    /** The Constant SYNONYM. */
    public static final int SYNONYM = 6;

    /** The Constant SCHEMA. */
    public static final int SCHEMA = 7;

    /** The Constant TABLE_TYPE. */
    public static final int TABLE_TYPE = 8;

}
