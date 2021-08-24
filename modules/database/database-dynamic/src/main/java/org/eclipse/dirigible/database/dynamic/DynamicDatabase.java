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
package org.eclipse.dirigible.database.dynamic;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.database.api.AbstractDatabase;
import org.eclipse.dirigible.database.api.IDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Dynamic Database
 */
public class DynamicDatabase extends AbstractDatabase {

	private static final Logger logger = LoggerFactory.getLogger(DynamicDatabase.class);

	/** The Constant NAME. */
	public static final String NAME = "basic";

	/** The Constant TYPE. */
	public static final String TYPE = "dynamic";

	private static final Map<String, DataSource> DATASOURCES = Collections.synchronizedMap(new HashMap<String, DataSource>());

	/**
	 * The default constructor
	 */
	public DynamicDatabase() {
		logger.debug("Initializing the custom datasources...");

		initialize();

		logger.debug("Custom datasources initialized.");
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.database.api.IDatabase#initialize()
	 */
	@Override
	public void initialize() {
		//Configuration.load("/dirigible-database-dynamic.properties");
		logger.debug(this.getClass().getCanonicalName() + " module initialized.");
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.database.api.IDatabase#getDataSource(java.lang.String)
	 */
	@Override
	public DataSource getDataSource(String name) {
		DataSource dataSource = DATASOURCES.get(name);
		if (dataSource != null) {
			return dataSource;
		}
		throw new IllegalArgumentException("Dynamic datasource has not been created: " + name);
	}

	/**
	 * Initialize a data source.
	 *
	 * @param name
	 *            the name
	 * @return the data source
	 * @throws IOException 
	 */
	public static DataSource createDataSource(String name, String databaseDriver, String databaseUrl, String databaseUsername, String databasePassword, String databaseConnectionProperties) throws IOException {
		if ((databaseDriver != null) && (databaseUrl != null) && (databaseUsername != null) && (databasePassword != null)) {
			BasicDataSource basicDataSource = new BasicDataSource();
			basicDataSource.setDriverClassName(databaseDriver);
			basicDataSource.setUrl(databaseUrl);
			basicDataSource.setUsername(databaseUsername);
			basicDataSource.setPassword(databasePassword);
			basicDataSource.setDefaultAutoCommit(true);
			basicDataSource.setAccessToUnderlyingConnectionAllowed(true);
			if (databaseConnectionProperties != null) {
				Properties properties = new Properties();
				properties.load(new StringReader(databaseConnectionProperties));
				basicDataSource.setConnectionProperties(databaseConnectionProperties);
			}
			DATASOURCES.put(name, basicDataSource);
			return basicDataSource;
		}
		throw new IllegalArgumentException("Invalid configuration for the dynamic datasource: " + name);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.database.api.IDatabase#getName()
	 */
	@Override
	public String getName() {
		return NAME;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.database.api.IDatabase#getType()
	 */
	@Override
	public String getType() {
		return TYPE;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.database.api.IDatabase#getDataSources()
	 */
	@Override
	public Map<String, DataSource> getDataSources() {
		Map<String, DataSource> datasources = new HashMap<String, DataSource>();
		datasources.putAll(DATASOURCES);
		return datasources;
	}

}
