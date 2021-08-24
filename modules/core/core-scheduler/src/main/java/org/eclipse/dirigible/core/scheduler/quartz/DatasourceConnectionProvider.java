/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.core.scheduler.quartz;

import java.sql.Connection;
import java.sql.SQLException;

import org.quartz.utils.ConnectionProvider;

/**
 * The Datasource Connection Provider.
 */
public class DatasourceConnectionProvider implements ConnectionProvider {

	private DatasourceProvider datasourceProvider;

	/*
	 * (non-Javadoc)
	 * @see org.quartz.utils.ConnectionProvider#getConnection()
	 */
	@Override
	public Connection getConnection() throws SQLException {
		return this.datasourceProvider.getDatasource().getConnection();
	}

	/*
	 * (non-Javadoc)
	 * @see org.quartz.utils.ConnectionProvider#initialize()
	 */
	@Override
	public void initialize() throws SQLException {
		if (this.datasourceProvider == null) {
			this.datasourceProvider = new DatasourceProvider();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.quartz.utils.ConnectionProvider#shutdown()
	 */
	@Override
	public void shutdown() throws SQLException {
		//
	}

}
