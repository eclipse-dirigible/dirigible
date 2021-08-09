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
package org.eclipse.dirigible.database.derby;

import static java.text.MessageFormat.format;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.database.api.AbstractDatabase;
import org.eclipse.dirigible.database.api.IDatabase;
import org.eclipse.dirigible.database.api.wrappers.WrappedDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Derby Database adapter.
 */
public class DerbyDatabase extends AbstractDatabase {

	private static final Logger logger = LoggerFactory.getLogger(DerbyDatabase.class);

	/** The Constant TYPE. */
	public static final String NAME = "derby";

	/** The Constant TYPE. */
	public static final String TYPE = "local";

	/** The Constant DIRIGIBLE_DATABASE_DERBY_ROOT_FOLDER. */
	public static final String DIRIGIBLE_DATABASE_DERBY_ROOT_FOLDER = "DIRIGIBLE_DATABASE_DERBY_ROOT_FOLDER"; //$NON-NLS-1$

	/** The Constant DIRIGIBLE_DATABASE_DERBY_ROOT_FOLDER_DEFAULT. */
	public static final String DIRIGIBLE_DATABASE_DERBY_ROOT_FOLDER_DEFAULT = DIRIGIBLE_DATABASE_DERBY_ROOT_FOLDER + "_DEFAULT"; //$NON-NLS-1$

	/** The Constant DATASOURCES. */
	private static final Map<String, DataSource> DATASOURCES = Collections.synchronizedMap(new HashMap<String, DataSource>());

	/**
	 * Constructor with default root folder - user.dir
	 *
	 * @throws DerbyDatabaseException
	 *             in case the database cannot be created
	 */
	public DerbyDatabase() throws DerbyDatabaseException {
		this(null);
	}

	/**
	 * Constructor with root folder parameter.
	 *
	 * @param rootFolder
	 *            the root folder
	 * @throws DerbyDatabaseException
	 *             in case the database cannot be created
	 */
	public DerbyDatabase(String rootFolder) throws DerbyDatabaseException {
		logger.debug("Initializing the embedded Derby datasource...");

		initialize();

		logger.debug("Embedded Derby datasource initialized.");
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.database.api.IDatabase#initialize()
	 */
	@Override
	public void initialize() {
		Configuration.loadModuleConfig("/dirigible-database-derby.properties");
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
		if (DIRIGIBLE_DATABASE_DATASOURCE_DEFAULT.equals(name) || DIRIGIBLE_DATABASE_DATASOURCE_TEST.equals(name)) {
			dataSource = createDataSource(name);
			return dataSource;
		}
		return null;
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

	/**
	 * Creates the data source.
	 *
	 * @param name
	 *            the name
	 * @return the data source
	 */
	protected DataSource createDataSource(String name) {
		logger.debug(String.format("Creating an embedded Derby datasource %s ...", name));

		synchronized (DerbyDatabase.class) {
			try {
				DataSource dataSource = new EmbeddedDataSource();
				String derbyRoot = prepareRootFolder(name);
				((EmbeddedDataSource) dataSource).setDatabaseName(derbyRoot);
				((EmbeddedDataSource) dataSource).setCreateDatabase("create");
				logger.warn(String.format("Embedded Derby at: %s", derbyRoot));

				WrappedDataSource wrappedDataSource = new WrappedDataSource(dataSource);
				DATASOURCES.put(name, wrappedDataSource);
				return wrappedDataSource;
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				throw new DerbyDatabaseException(e);
			}
		}
	}

	/**
	 * Prepare root folder.
	 *
	 * @param name
	 *            the name
	 * @return the string
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private String prepareRootFolder(String name) throws IOException {
		// TODO validate name parameter
		// TODO get by name from Configuration

		String rootFolder = (IDatabase.DIRIGIBLE_DATABASE_DATASOURCE_DEFAULT.equals(name)) ? DIRIGIBLE_DATABASE_DERBY_ROOT_FOLDER_DEFAULT
				: DIRIGIBLE_DATABASE_DERBY_ROOT_FOLDER + name;
		String derbyRoot = Configuration.get(rootFolder, name);
		File rootFile = new File(derbyRoot);
		File parentFile = rootFile.getCanonicalFile().getParentFile();
		if (!parentFile.exists()) {
			if (!parentFile.mkdirs()) {
				throw new IOException(format("Creation of the root folder [{0}] of the embedded Derby database failed.", derbyRoot));
			}
		}
		return derbyRoot;
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
