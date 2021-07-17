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

import javax.inject.Inject;
import javax.sql.DataSource;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.core.scheduler.manager.SchedulerManager;
import org.eclipse.dirigible.database.api.DatabaseModule;

/**
 * The Datasource Provider.
 */
public class DatasourceProvider {

	@Inject
	private DataSource datasource;
	
	private DataSource runtimeDataSource;

	/**
	 * Gets the datasource.
	 *
	 * @return the datasource
	 */
	public DataSource getDatasource() {
		if (this.runtimeDataSource == null) {
			this.runtimeDataSource = this.datasource;
			Configuration.loadModuleConfig("/dirigible-scheduler.properties");
			String dataSourceType = Configuration.get(SchedulerManager.DIRIGIBLE_SCHEDULER_DATABASE_DATASOURCE_TYPE);
			String dataSourceName = Configuration.get(SchedulerManager.DIRIGIBLE_SCHEDULER_DATABASE_DATASOURCE_NAME);
			if (dataSourceType != null && dataSourceName != null) {
				this.runtimeDataSource = DatabaseModule.getDataSource(dataSourceType, dataSourceName);
			}
		}
		
		return this.runtimeDataSource;
	}

//	/**
//	 * Sets the datasource.
//	 *
//	 * @param datasource
//	 *            the new datasource
//	 */
//	public void setDatasource(DataSource datasource) {
//		this.datasource = datasource;
//	}

}
