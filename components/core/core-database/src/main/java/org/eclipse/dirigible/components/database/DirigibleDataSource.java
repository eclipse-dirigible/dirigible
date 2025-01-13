/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database;

import javax.sql.DataSource;
import java.sql.SQLException;

public interface DirigibleDataSource extends DataSource, DatabaseSystemAware {

    void close();

    @Override
    DirigibleConnection getConnection() throws SQLException;

    @Override
    DirigibleConnection getConnection(String username, String password) throws SQLException;

}
