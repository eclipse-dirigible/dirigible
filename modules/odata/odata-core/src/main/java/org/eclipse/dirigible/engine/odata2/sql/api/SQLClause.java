/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.api;

import org.apache.olingo.odata2.api.edm.EdmException;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLContext;

/**
 * The Interface SQLClause.
 */
public interface SQLClause {

    /**
     * Evaluate.
     *
     * @param context the context
     * @return the string
     * @throws EdmException the edm exception
     */
    String evaluate(SQLContext context) throws EdmException;

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    boolean isEmpty();
}
