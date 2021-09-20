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
package org.eclipse.dirigible.database.ds.synchronizer;

import static java.text.MessageFormat.format;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.api.helpers.DataStructuresUtils;
import org.eclipse.dirigible.commons.config.StaticObjects;
import org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizer;
import org.eclipse.dirigible.core.scheduler.api.SchedulerException;
import org.eclipse.dirigible.core.scheduler.api.SynchronizationException;
import org.eclipse.dirigible.core.scheduler.service.SynchronizerCoreService;
import org.eclipse.dirigible.database.ds.api.DataStructuresException;
import org.eclipse.dirigible.database.ds.model.DataStructureDataAppendModel;
import org.eclipse.dirigible.database.ds.model.DataStructureDataDeleteModel;
import org.eclipse.dirigible.database.ds.model.DataStructureDataReplaceModel;
import org.eclipse.dirigible.database.ds.model.DataStructureDataUpdateModel;
import org.eclipse.dirigible.database.ds.model.DataStructureModel;
import org.eclipse.dirigible.database.ds.model.DataStructureSchemaModel;
import org.eclipse.dirigible.database.ds.model.DataStructureTableModel;
import org.eclipse.dirigible.database.ds.model.DataStructureTopologicalSorter;
import org.eclipse.dirigible.database.ds.model.DataStructureViewModel;
import org.eclipse.dirigible.database.ds.model.IDataStructureModel;
import org.eclipse.dirigible.database.ds.model.processors.TableAlterProcessor;
import org.eclipse.dirigible.database.ds.model.processors.TableCreateProcessor;
import org.eclipse.dirigible.database.ds.model.processors.TableDropProcessor;
import org.eclipse.dirigible.database.ds.model.processors.TableForeignKeysCreateProcessor;
import org.eclipse.dirigible.database.ds.model.processors.TableForeignKeysDropProcessor;
import org.eclipse.dirigible.database.ds.model.processors.ViewCreateProcessor;
import org.eclipse.dirigible.database.ds.model.processors.ViewDropProcessor;
import org.eclipse.dirigible.database.ds.model.transfer.TableDataReader;
import org.eclipse.dirigible.database.ds.model.transfer.TableExporter;
import org.eclipse.dirigible.database.ds.model.transfer.TableImporter;
import org.eclipse.dirigible.database.ds.model.transfer.TableMetadataHelper;
import org.eclipse.dirigible.database.ds.service.DataStructuresCoreService;
import org.eclipse.dirigible.database.persistence.PersistenceException;
import org.eclipse.dirigible.database.persistence.PersistenceManager;
import org.eclipse.dirigible.database.persistence.processors.identity.Identity;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Data Structures Synchronizer.
 */
public class DataStructuresSynchronizer extends AbstractSynchronizer {

	private static final Logger logger = LoggerFactory.getLogger(DataStructuresSynchronizer.class);

	private static final Map<String, DataStructureTableModel> TABLES_PREDELIVERED = Collections
			.synchronizedMap(new HashMap<String, DataStructureTableModel>());

	private static final Map<String, DataStructureViewModel> VIEWS_PREDELIVERED = Collections
			.synchronizedMap(new HashMap<String, DataStructureViewModel>());

	private static final Map<String, DataStructureDataReplaceModel> REPLACE_PREDELIVERED = Collections
			.synchronizedMap(new HashMap<String, DataStructureDataReplaceModel>());

	private static final Map<String, DataStructureDataAppendModel> APPEND_PREDELIVERED = Collections
			.synchronizedMap(new HashMap<String, DataStructureDataAppendModel>());

	private static final Map<String, DataStructureDataDeleteModel> DELETE_PREDELIVERED = Collections
			.synchronizedMap(new HashMap<String, DataStructureDataDeleteModel>());

	private static final Map<String, DataStructureDataUpdateModel> UPDATE_PREDELIVERED = Collections
			.synchronizedMap(new HashMap<String, DataStructureDataUpdateModel>());
	
	private static final Map<String, DataStructureSchemaModel> SCHEMA_PREDELIVERED = Collections
			.synchronizedMap(new HashMap<String, DataStructureSchemaModel>());

	private static final List<String> TABLES_SYNCHRONIZED = Collections.synchronizedList(new ArrayList<String>());

	private static final List<String> VIEWS_SYNCHRONIZED = Collections.synchronizedList(new ArrayList<String>());

	private static final List<String> REPLACE_SYNCHRONIZED = Collections.synchronizedList(new ArrayList<String>());

	private static final List<String> APPEND_SYNCHRONIZED = Collections.synchronizedList(new ArrayList<String>());

	private static final List<String> DELETE_SYNCHRONIZED = Collections.synchronizedList(new ArrayList<String>());

	private static final List<String> UPDATE_SYNCHRONIZED = Collections.synchronizedList(new ArrayList<String>());
	
	private static final List<String> SCHEMA_SYNCHRONIZED = Collections.synchronizedList(new ArrayList<String>());

	private static final Map<String, DataStructureModel> DATA_STRUCTURE_MODELS = new LinkedHashMap<String, DataStructureModel>();

	private static final Map<String, DataStructureDataReplaceModel> DATA_STRUCTURE_REPLACE_MODELS = new LinkedHashMap<String, DataStructureDataReplaceModel>();

	private static final Map<String, DataStructureDataAppendModel> DATA_STRUCTURE_APPEND_MODELS = new LinkedHashMap<String, DataStructureDataAppendModel>();

	private static final Map<String, DataStructureDataDeleteModel> DATA_STRUCTURE_DELETE_MODELS = new LinkedHashMap<String, DataStructureDataDeleteModel>();

	private static final Map<String, DataStructureDataUpdateModel> DATA_STRUCTURE_UPDATE_MODELS = new LinkedHashMap<String, DataStructureDataUpdateModel>();
	
	private static final Map<String, DataStructureSchemaModel> DATA_STRUCTURE_SCHEMA_MODELS = new LinkedHashMap<String, DataStructureSchemaModel>();

	private DataStructuresCoreService dataStructuresCoreService = new DataStructuresCoreService();
	
	private DataSource dataSource = (DataSource) StaticObjects.get(StaticObjects.DATASOURCE);
	
	private final String SYNCHRONIZER_NAME = this.getClass().getCanonicalName();
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.ISynchronizer#synchronize()
	 */
	@Override
	public void synchronize() {
		synchronized (DataStructuresSynchronizer.class) {
			if (beforeSynchronizing()) {
				logger.trace("Synchronizing Data Structures...");
				try {
					if (isSynchronizationEnabled()) {
						startSynchronization(SYNCHRONIZER_NAME);
						clearCache();
						synchronizePredelivered();
						synchronizeRegistry();
						updateDatabaseSchema();
						updateDatabaseContent();
						int immutableTablesCount = TABLES_PREDELIVERED.size();
						int immutableViewsCount = VIEWS_PREDELIVERED.size();
						int immutableSchemaCount = SCHEMA_PREDELIVERED.size();
						int immutableReplaceCount = REPLACE_PREDELIVERED.size();
						int immutableAppendCount = APPEND_PREDELIVERED.size();
						int immutableDeleteCount = DELETE_PREDELIVERED.size();
						int immutableUpdateCount = UPDATE_PREDELIVERED.size();
						
						int mutableTablesCount = TABLES_SYNCHRONIZED.size();
						int mutableViewsCount = VIEWS_SYNCHRONIZED.size();
						int mutableSchemaCount = DATA_STRUCTURE_SCHEMA_MODELS.size();
						int mutableReplaceCount = DATA_STRUCTURE_REPLACE_MODELS.size();
						int mutableAppendCount = DATA_STRUCTURE_APPEND_MODELS.size();
						int mutableDeleteCount = DATA_STRUCTURE_DELETE_MODELS.size();
						int mutableUpdateCount = DATA_STRUCTURE_UPDATE_MODELS.size();
						
						cleanup(); // TODO drop tables and views for non-existing models
						clearCache();
						
						successfulSynchronization(SYNCHRONIZER_NAME, format("Immutable: [ Tables: {0}, Views: {1}, Schema: {2}, Replace: {3}, Append: {4}, Delete: {5}, Update: {6}], "
								+ "Mutable: [Tables: {7}, Views: {8}, Schema: {9}, Replace: {10}, Append: {11}, Delete: {12}, Update: {13}]", 
								immutableTablesCount, immutableViewsCount, immutableSchemaCount, immutableReplaceCount, immutableAppendCount, immutableDeleteCount, immutableUpdateCount,
								mutableTablesCount, mutableViewsCount, mutableSchemaCount, mutableReplaceCount, mutableAppendCount, mutableDeleteCount, mutableUpdateCount));
					} else {
						logger.debug("Synchronization has been disabled");
					}
				} catch (Exception e) {
					logger.error("Synchronizing process for Data Structures failed.", e);
					try {
						failedSynchronization(SYNCHRONIZER_NAME, e.getMessage());
					} catch (SchedulerException e1) {
						logger.error("Synchronizing process for Data Structures files failed in registering the state log.", e);
					}
				}
				logger.trace("Done synchronizing Data Structures.");
				afterSynchronizing();
			}
		}
	}

	/**
	 * Force synchronization.
	 */
	public static final void forceSynchronization() {
		DataStructuresSynchronizer synchronizer = new DataStructuresSynchronizer();
		synchronizer.setForcedSynchronization(true);
		try {
			synchronizer.synchronize();
		} finally {
			synchronizer.setForcedSynchronization(false);
		}
	}

	/**
	 * Register predelivered table.
	 *
	 * @param tableModelPath
	 *            the table model path
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void registerPredeliveredTable(String tableModelPath) throws IOException {
		String json = loadResourceContent(tableModelPath);
		DataStructureTableModel tableModel = dataStructuresCoreService.parseTable(json);
		tableModel.setLocation(tableModelPath);
		TABLES_PREDELIVERED.put(tableModelPath, tableModel);
	}

	/**
	 * Register predelivered view.
	 *
	 * @param viewModelPath
	 *            the view model path
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void registerPredeliveredView(String viewModelPath) throws IOException {
		String json = loadResourceContent(viewModelPath);
		DataStructureViewModel viewModel = dataStructuresCoreService.parseView(json);
		viewModel.setLocation(viewModelPath);
		VIEWS_PREDELIVERED.put(viewModelPath, viewModel);
	}

	/**
	 * Register predelivered replace files.
	 *
	 * @param contentPath
	 *            the data path
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void registerPredeliveredReplace(String contentPath) throws IOException {
		String data = loadResourceContent(contentPath);
		DataStructureDataReplaceModel model = dataStructuresCoreService.parseReplace(contentPath, data);
		REPLACE_PREDELIVERED.put(contentPath, model);
	}

	/**
	 * Register predelivered append files.
	 *
	 * @param contentPath
	 *            the data path
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void registerPredeliveredAppend(String contentPath) throws IOException {
		String data = loadResourceContent(contentPath);
		DataStructureDataAppendModel model = dataStructuresCoreService.parseAppend(contentPath, data);
		APPEND_PREDELIVERED.put(contentPath, model);
	}

	/**
	 * Register predelivered delete files.
	 *
	 * @param contentPath
	 *            the data path
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void registerPredeliveredDelete(String contentPath) throws IOException {
		String data = loadResourceContent(contentPath);
		DataStructureDataDeleteModel model = dataStructuresCoreService.parseDelete(contentPath, data);
		DELETE_PREDELIVERED.put(contentPath, model);
	}

	/**
	 * Register predelivered update files.
	 *
	 * @param contentPath
	 *            the data path
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void registerPredeliveredUpdate(String contentPath) throws IOException {
		String data = loadResourceContent(contentPath);
		DataStructureDataUpdateModel model = dataStructuresCoreService.parseUpdate(contentPath, data);
		UPDATE_PREDELIVERED.put(contentPath, model);
	}
	
	/**
	 * Register predelivered schema files.
	 *
	 * @param contentPath
	 *            the data path
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void registerPredeliveredSchema(String contentPath) throws IOException {
		String data = loadResourceContent(contentPath);
		DataStructureSchemaModel model = dataStructuresCoreService.parseSchema(contentPath, data);
		SCHEMA_PREDELIVERED.put(contentPath, model);
	}

	private String loadResourceContent(String modelPath) throws IOException {
		InputStream in = DataStructuresSynchronizer.class.getResourceAsStream("/META-INF/dirigible" + modelPath);
		try {
			String content = IOUtils.toString(in, StandardCharsets.UTF_8);
			return content;
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	/**
	 * Clear cache.
	 */
	private void clearCache() {
		TABLES_SYNCHRONIZED.clear();
		VIEWS_SYNCHRONIZED.clear();
		DATA_STRUCTURE_MODELS.clear();
		DATA_STRUCTURE_REPLACE_MODELS.clear();
		DATA_STRUCTURE_APPEND_MODELS.clear();
		DATA_STRUCTURE_DELETE_MODELS.clear();
		DATA_STRUCTURE_UPDATE_MODELS.clear();
		DATA_STRUCTURE_SCHEMA_MODELS.clear();
	}

	/**
	 * Synchronize predelivered.
	 *
	 * @throws SynchronizationException
	 *             the synchronization exception
	 */
	private void synchronizePredelivered() throws SynchronizationException {
		logger.trace("Synchronizing predelivered Data Structures...");
		// Tables
		for (DataStructureTableModel tableModel : TABLES_PREDELIVERED.values()) {
			try {
				synchronizeTable(tableModel);
			} catch (Exception e) {
				logger.error(format("Table [{0}] skipped due to an error: {1}", tableModel.getLocation(), e.getMessage()), e);
			}
		}
		// Views
		for (DataStructureViewModel viewModel : VIEWS_PREDELIVERED.values()) {
			try {
				synchronizeView(viewModel);
			} catch (Exception e) {
				logger.error(format("View [{0}] skipped due to an error: {1}", viewModel.getLocation(), e.getMessage()), e);
			}
		}
		// Replace
		for (DataStructureDataReplaceModel data : REPLACE_PREDELIVERED.values()) {
			try {
				synchronizeReplace(data);
			} catch (Exception e) {
				logger.error(format("Replace data [{0}] skipped due to an error: {1}", data, e.getMessage()), e);
			}
		}
		// Append
		for (DataStructureDataAppendModel data : APPEND_PREDELIVERED.values()) {
			try {
				synchronizeAppend(data);
			} catch (Exception e) {
				logger.error(format("Append data [{0}] skipped due to an error: {1}", data, e.getMessage()), e);
			}
		}
		// Delete
		for (DataStructureDataDeleteModel data : DELETE_PREDELIVERED.values()) {
			try {
				synchronizeDelete(data);
			} catch (Exception e) {
				logger.error(format("Delete data [{0}] skipped due to an error: {1}", data, e.getMessage()), e);
			}
		}
		// Update
		for (DataStructureDataUpdateModel data : UPDATE_PREDELIVERED.values()) {
			try {
				synchronizeUpdate(data);
			} catch (Exception e) {
				logger.error(format("Update data [{0}] skipped due to an error: {1}", data, e.getMessage()), e);
			}
		}
		// Schema
		for (DataStructureSchemaModel schema : SCHEMA_PREDELIVERED.values()) {
			try {
				synchronizeSchema(schema);
			} catch (Exception e) {
				logger.error(format("Update schema [{0}] skipped due to an error: {1}", schema, e.getMessage()), e);
			}
		}
		logger.trace("Done synchronizing predelivered Data Structures.");
	}

	/**
	 * Synchronize table.
	 *
	 * @param tableModel
	 *            the table model
	 * @throws SynchronizationException
	 *             the synchronization exception
	 */
	private void synchronizeTable(DataStructureTableModel tableModel) throws SynchronizationException {
		try {
			if (!dataStructuresCoreService.existsTable(tableModel.getLocation())) {
				DataStructureTableModel duplicated = dataStructuresCoreService.getTableByName(tableModel.getName());
				if (duplicated != null) {
					throw new SynchronizationException(
							format("Table [{0}] defined by the model at: [{1}] has already been defined by the model at: [{2}]", tableModel.getName(),
									tableModel.getLocation(), duplicated.getLocation()));
				}
				dataStructuresCoreService.createTable(tableModel.getLocation(), tableModel.getName(), tableModel.getHash());
				DATA_STRUCTURE_MODELS.put(tableModel.getName(), tableModel);
				logger.info("Synchronized a new Table [{}] from location: {}", tableModel.getName(), tableModel.getLocation());
			} else {
				DataStructureTableModel existing = dataStructuresCoreService.getTable(tableModel.getLocation());
				if (!tableModel.equals(existing)) {
					dataStructuresCoreService.updateTable(tableModel.getLocation(), tableModel.getName(), tableModel.getHash());
					DATA_STRUCTURE_MODELS.put(tableModel.getName(), tableModel);
					logger.info("Synchronized a modified Table [{}] from location: {}", tableModel.getName(), tableModel.getLocation());
				}
			}
			TABLES_SYNCHRONIZED.add(tableModel.getLocation());
		} catch (DataStructuresException e) {
			throw new SynchronizationException(e);
		}
	}

	/**
	 * Synchronize view.
	 *
	 * @param viewModel
	 *            the view model
	 * @throws SynchronizationException
	 *             the synchronization exception
	 */
	private void synchronizeView(DataStructureViewModel viewModel) throws SynchronizationException {
		try {
			if (!dataStructuresCoreService.existsView(viewModel.getLocation())) {
				DataStructureViewModel duplicated = dataStructuresCoreService.getViewByName(viewModel.getName());
				if (duplicated != null) {
					throw new SynchronizationException(
							format("View [{0}] defined by the model at: [{1}] has already been defined by the model at: [{2}]", viewModel.getName(),
									viewModel.getLocation(), duplicated.getLocation()));
				}
				dataStructuresCoreService.createView(viewModel.getLocation(), viewModel.getName(), viewModel.getHash());
				DATA_STRUCTURE_MODELS.put(viewModel.getName(), viewModel);
				logger.info("Synchronized a new View [{}] from location: {}", viewModel.getName(), viewModel.getLocation());
			} else {
				DataStructureViewModel existing = dataStructuresCoreService.getView(viewModel.getLocation());
				if (!viewModel.equals(existing)) {
					dataStructuresCoreService.updateView(viewModel.getLocation(), viewModel.getName(), viewModel.getHash());
					DATA_STRUCTURE_MODELS.put(viewModel.getName(), viewModel);
					logger.info("Synchronized a modified View [{}] from location: {}", viewModel.getName(), viewModel.getLocation());
				}
			}
			VIEWS_SYNCHRONIZED.add(viewModel.getLocation());
		} catch (DataStructuresException e) {
			throw new SynchronizationException(e);
		}
	}

	/**
	 * Synchronize replace.
	 *
	 * @param dataModel
	 *            the data model
	 * @throws SynchronizationException
	 *             the synchronization exception
	 */
	private void synchronizeReplace(DataStructureDataReplaceModel dataModel) throws SynchronizationException {
		try {
			if (!dataStructuresCoreService.existsReplace(dataModel.getLocation())) {
				dataStructuresCoreService.createReplace(dataModel.getLocation(), dataModel.getName(), dataModel.getHash());
				DATA_STRUCTURE_REPLACE_MODELS.put(dataModel.getName(), dataModel);
				logger.info("Synchronized a new Replace Data file [{}] from location: {}", dataModel.getName(), dataModel.getLocation());
			} else {
				DataStructureDataReplaceModel existing = dataStructuresCoreService.getReplace(dataModel.getLocation());
				if (!dataModel.equals(existing)) {
					dataStructuresCoreService.updateReplace(dataModel.getLocation(), dataModel.getName(), dataModel.getHash());
					DATA_STRUCTURE_REPLACE_MODELS.put(dataModel.getName(), dataModel);
					logger.info("Synchronized a modified Replace Data file [{}] from location: {}", dataModel.getName(), dataModel.getLocation());
				}
			}
			REPLACE_SYNCHRONIZED.add(dataModel.getLocation());
		} catch (DataStructuresException e) {
			throw new SynchronizationException(e);
		}
	}

	/**
	 * Synchronize append.
	 *
	 * @param dataModel
	 *            the data model
	 * @throws SynchronizationException
	 *             the synchronization exception
	 */
	private void synchronizeAppend(DataStructureDataAppendModel dataModel) throws SynchronizationException {
		try {
			if (!dataStructuresCoreService.existsAppend(dataModel.getLocation())) {
				dataStructuresCoreService.createAppend(dataModel.getLocation(), dataModel.getName(), dataModel.getHash());
				DATA_STRUCTURE_APPEND_MODELS.put(dataModel.getName(), dataModel);
				logger.info("Synchronized a new Append Data file [{}] from location: {}", dataModel.getName(), dataModel.getLocation());
			} else {
				DataStructureDataAppendModel existing = dataStructuresCoreService.getAppend(dataModel.getLocation());
				if (!dataModel.equals(existing)) {
					dataStructuresCoreService.updateAppend(dataModel.getLocation(), dataModel.getName(), dataModel.getHash());
					DATA_STRUCTURE_APPEND_MODELS.put(dataModel.getName(), dataModel);
					logger.info("Synchronized a modified Append Data file [{}] from location: {}", dataModel.getName(), dataModel.getLocation());
				}
			}
			APPEND_SYNCHRONIZED.add(dataModel.getLocation());
		} catch (DataStructuresException e) {
			throw new SynchronizationException(e);
		}
	}

	/**
	 * Synchronize delete.
	 *
	 * @param dataModel
	 *            the data model
	 * @throws SynchronizationException
	 *             the synchronization exception
	 */
	private void synchronizeDelete(DataStructureDataDeleteModel dataModel) throws SynchronizationException {
		try {
			if (!dataStructuresCoreService.existsDelete(dataModel.getLocation())) {
				dataStructuresCoreService.createDelete(dataModel.getLocation(), dataModel.getName(), dataModel.getHash());
				DATA_STRUCTURE_DELETE_MODELS.put(dataModel.getName(), dataModel);
				logger.info("Synchronized a new Delete Data file [{}] from location: {}", dataModel.getName(), dataModel.getLocation());
			} else {
				DataStructureDataDeleteModel existing = dataStructuresCoreService.getDelete(dataModel.getLocation());
				if (!dataModel.equals(existing)) {
					dataStructuresCoreService.updateDelete(dataModel.getLocation(), dataModel.getName(), dataModel.getHash());
					DATA_STRUCTURE_DELETE_MODELS.put(dataModel.getName(), dataModel);
					logger.info("Synchronized a modified Delete Data file [{}] from location: {}", dataModel.getName(), dataModel.getLocation());
				}
			}
			DELETE_SYNCHRONIZED.add(dataModel.getLocation());
		} catch (DataStructuresException e) {
			throw new SynchronizationException(e);
		}
	}

	/**
	 * Synchronize update.
	 *
	 * @param dataModel
	 *            the data model
	 * @throws SynchronizationException
	 *             the synchronization exception
	 */
	private void synchronizeUpdate(DataStructureDataUpdateModel dataModel) throws SynchronizationException {
		try {
			if (!dataStructuresCoreService.existsUpdate(dataModel.getLocation())) {
				dataStructuresCoreService.createUpdate(dataModel.getLocation(), dataModel.getName(), dataModel.getHash());
				DATA_STRUCTURE_UPDATE_MODELS.put(dataModel.getName(), dataModel);
				logger.info("Synchronized a new Update Data file [{}] from location: {}", dataModel.getName(), dataModel.getLocation());
			} else {
				DataStructureDataUpdateModel existing = dataStructuresCoreService.getUpdate(dataModel.getLocation());
				if (!dataModel.equals(existing)) {
					dataStructuresCoreService.updateUpdate(dataModel.getLocation(), dataModel.getName(), dataModel.getHash());
					DATA_STRUCTURE_UPDATE_MODELS.put(dataModel.getName(), dataModel);
					logger.info("Synchronized a modified Update Data file [{}] from location: {}", dataModel.getName(), dataModel.getLocation());
				}
			}
			UPDATE_SYNCHRONIZED.add(dataModel.getLocation());
		} catch (DataStructuresException e) {
			throw new SynchronizationException(e);
		}
	}
	
	/**
	 * Synchronize schema.
	 *
	 * @param schemaModel
	 *            the schema model
	 * @throws SynchronizationException
	 *             the synchronization exception
	 */
	private void synchronizeSchema(DataStructureSchemaModel schemaModel) throws SynchronizationException {
		try {
			if (!dataStructuresCoreService.existsSchema(schemaModel.getLocation())) {
				dataStructuresCoreService.createSchema(schemaModel.getLocation(), schemaModel.getName(), schemaModel.getHash());
				DATA_STRUCTURE_SCHEMA_MODELS.put(schemaModel.getName(), schemaModel);
				addDataStructureModelsFromSchema(schemaModel);
				logger.info("Synchronized a new Schema file [{}] from location: {}", schemaModel.getName(), schemaModel.getLocation());
			} else {
				DataStructureSchemaModel existing = dataStructuresCoreService.getSchema(schemaModel.getLocation());
				if (!schemaModel.equals(existing)) {
					dataStructuresCoreService.updateSchema(schemaModel.getLocation(), schemaModel.getName(), schemaModel.getHash());
					DATA_STRUCTURE_SCHEMA_MODELS.put(schemaModel.getName(), schemaModel);
					addDataStructureModelsFromSchema(schemaModel);
					logger.info("Synchronized a modified Schema file [{}] from location: {}", schemaModel.getName(), schemaModel.getLocation());
				}
			}
			SCHEMA_SYNCHRONIZED.add(schemaModel.getLocation());
		} catch (DataStructuresException e) {
			throw new SynchronizationException(e);
		}
	}

	private void addDataStructureModelsFromSchema(DataStructureSchemaModel schemaModel) {
		for (DataStructureTableModel tableModel : schemaModel.getTables()) {
			DATA_STRUCTURE_MODELS.put(tableModel.getName(), tableModel);
		}
		for (DataStructureViewModel viewModel : schemaModel.getViews()) {
			DATA_STRUCTURE_MODELS.put(viewModel.getName(), viewModel);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizer#synchronizeRegistry()
	 */
	@Override
	protected void synchronizeRegistry() throws SynchronizationException {
		logger.trace("Synchronizing Data Structures from Registry...");

		super.synchronizeRegistry();

		logger.trace("Done synchronizing Data Structures from Registry.");
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizer#synchronizeResource(org.eclipse.dirigible.
	 * repository.api.IResource)
	 */
	@Override
	protected void synchronizeResource(IResource resource) throws SynchronizationException {
		String resourceName = resource.getName();
		String registryPath = getRegistryPath(resource);
		byte[] content = resource.getContent();
		String contentAsString;
		try {
			contentAsString = IOUtils.toString(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new SynchronizationException(e);
		}
		if (resourceName.endsWith(IDataStructureModel.FILE_EXTENSION_TABLE)) {
			DataStructureTableModel tableModel = dataStructuresCoreService.parseTable(content);
			tableModel.setLocation(registryPath);
			synchronizeTable(tableModel);
			return;
		}

		if (resourceName.endsWith(IDataStructureModel.FILE_EXTENSION_VIEW)) {
			DataStructureViewModel viewModel = dataStructuresCoreService.parseView(content);
			viewModel.setLocation(registryPath);
			synchronizeView(viewModel);
			return;
		}

		if (resourceName.endsWith(IDataStructureModel.FILE_EXTENSION_REPLACE)) {
			DataStructureDataReplaceModel dataModel = dataStructuresCoreService.parseReplace(registryPath, contentAsString);
			dataModel.setLocation(registryPath);
			synchronizeReplace(dataModel);
			return;
		}

		if (resourceName.endsWith(IDataStructureModel.FILE_EXTENSION_APPEND)) {
			DataStructureDataAppendModel dataModel = dataStructuresCoreService.parseAppend(registryPath, contentAsString);
			dataModel.setLocation(registryPath);
			synchronizeAppend(dataModel);
			return;
		}

		if (resourceName.endsWith(IDataStructureModel.FILE_EXTENSION_DELETE)) {
			DataStructureDataDeleteModel dataModel = dataStructuresCoreService.parseDelete(registryPath, contentAsString);
			dataModel.setLocation(registryPath);
			synchronizeDelete(dataModel);
			return;
		}

		if (resourceName.endsWith(IDataStructureModel.FILE_EXTENSION_UPDATE)) {
			DataStructureDataUpdateModel dataModel = dataStructuresCoreService.parseUpdate(registryPath, contentAsString);
			dataModel.setLocation(registryPath);
			synchronizeUpdate(dataModel);
			return;
		}
		if (resourceName.endsWith(IDataStructureModel.FILE_EXTENSION_SCHEMA)) {
			DataStructureSchemaModel schemaModel = dataStructuresCoreService.parseSchema(registryPath, contentAsString);
			schemaModel.setLocation(registryPath);
			synchronizeSchema(schemaModel);
			return;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.core.scheduler.api.AbstractSynchronizer#cleanup()
	 */
	@Override
	protected void cleanup() throws SynchronizationException {
		logger.trace("Cleaning up Data Structures...");
		super.cleanup();

		try {
			Connection connection = null;
			try {
				connection = dataSource.getConnection();
				List<DataStructureTableModel> tableModels = dataStructuresCoreService.getTables();
				for (DataStructureTableModel tableModel : tableModels) {
					if (!TABLES_SYNCHRONIZED.contains(tableModel.getLocation())) {
						dataStructuresCoreService.removeTable(tableModel.getLocation());
						executeTableDrop(connection, tableModel);
						logger.warn("Cleaned up Table [{}] from location: {}", tableModel.getName(), tableModel.getLocation());
					}
				}

				List<DataStructureViewModel> viewModels = dataStructuresCoreService.getViews();
				for (DataStructureViewModel viewModel : viewModels) {
					if (!VIEWS_SYNCHRONIZED.contains(viewModel.getLocation())) {
						dataStructuresCoreService.removeView(viewModel.getLocation());
						executeViewDrop(connection, viewModel);
						logger.warn("Cleaned up View [{}] from location: {}", viewModel.getName(), viewModel.getLocation());
					}
				}

				List<DataStructureDataReplaceModel> dataReplaceModels = dataStructuresCoreService.getReplaces();
				for (DataStructureDataReplaceModel dataModel : dataReplaceModels) {
					if (!REPLACE_SYNCHRONIZED.contains(dataModel.getLocation())) {
						dataStructuresCoreService.removeReplace(dataModel.getLocation());
						logger.warn("Cleaned up Replace Data file [{}] from location: {}", dataModel.getName(), dataModel.getLocation());
					}
				}

				List<DataStructureDataAppendModel> dataAppendModels = dataStructuresCoreService.getAppends();
				for (DataStructureDataAppendModel dataModel : dataAppendModels) {
					if (!APPEND_SYNCHRONIZED.contains(dataModel.getLocation())) {
						dataStructuresCoreService.removeAppend(dataModel.getLocation());
						logger.warn("Cleaned up Append Data file [{}] from location: {}", dataModel.getName(), dataModel.getLocation());
					}
				}

				List<DataStructureDataDeleteModel> dataDeleteModels = dataStructuresCoreService.getDeletes();
				for (DataStructureDataDeleteModel dataModel : dataDeleteModels) {
					if (!DELETE_SYNCHRONIZED.contains(dataModel.getLocation())) {
						dataStructuresCoreService.removeDelete(dataModel.getLocation());
						logger.warn("Cleaned up Delete Data file [{}] from location: {}", dataModel.getName(), dataModel.getLocation());
					}
				}

				List<DataStructureDataUpdateModel> dataUpdateModels = dataStructuresCoreService.getUpdates();
				for (DataStructureDataUpdateModel dataModel : dataUpdateModels) {
					if (!UPDATE_SYNCHRONIZED.contains(dataModel.getLocation())) {
						dataStructuresCoreService.removeUpdate(dataModel.getLocation());
						logger.warn("Cleaned up Update Data file [{}] from location: {}", dataModel.getName(), dataModel.getLocation());
					}
				}
				
				List<DataStructureSchemaModel> schemaModels = dataStructuresCoreService.getSchemas();
				for (DataStructureSchemaModel schemaModel : schemaModels) {
					if (!SCHEMA_SYNCHRONIZED.contains(schemaModel.getLocation())) {
						dataStructuresCoreService.removeSchema(schemaModel.getLocation());
						logger.warn("Cleaned up Schema Data file [{}] from location: {}", schemaModel.getName(), schemaModel.getLocation());
					}
				}
			} finally {
				if (connection != null) {
					connection.close();
				}
			}
		} catch (DataStructuresException | SQLException e) {
			throw new SynchronizationException(e);
		}

		logger.trace("Done cleaning up Data Structures.");
	}

	/**
	 * Update database schema.
	 */
	private void updateDatabaseSchema() {

		if (DATA_STRUCTURE_MODELS.isEmpty()) {
			logger.trace("No Data Structures to update.");
			return;
		}

		List<String> errors = new ArrayList<String>();
		try {
			Connection connection = null;
			try {
				connection = dataSource.getConnection();
				// topology sort of dependencies
				List<String> sorted = new ArrayList<String>();
				List<String> external = new ArrayList<String>();
				try {
					DataStructureTopologicalSorter.sort(DATA_STRUCTURE_MODELS, sorted, external);

					logger.trace("topological sorting");

					for (String location : sorted) {
						logger.trace("location: " + location);
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					errors.add(e.getMessage());
					sorted.clear();
				}

				if (sorted.isEmpty()) {
					// something wrong happened with the sorting - probably cyclic dependencies
					// we go for the back-up list and try to apply what would succeed
					logger.warn("Probably there are cyclic dependencies!");
					sorted.addAll(DATA_STRUCTURE_MODELS.keySet());
				}

				// drop views first in a reverse order
				for (int i = sorted.size() - 1; i >= 0; i--) {
					String dsName = sorted.get(i);
					DataStructureModel model = DATA_STRUCTURE_MODELS.get(dsName);
					try {
						if (model instanceof DataStructureViewModel) {
							executeViewDrop(connection, (DataStructureViewModel) model);
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						errors.add(e.getMessage());
					}
				}
				
				// drop tables in a reverse order
				for (int i = sorted.size() - 1; i >= 0; i--) {
					String dsName = sorted.get(i);
					DataStructureModel model = DATA_STRUCTURE_MODELS.get(dsName);
					try {
						if (model instanceof DataStructureTableModel) {
							if (SqlFactory.getNative(connection).exists(connection, model.getName())) {
								executeTableForeignKeysDrop(connection, (DataStructureTableModel) model);
							}
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						errors.add(e.getMessage());
					}
				}
				
				// drop tables in a reverse order
				for (int i = sorted.size() - 1; i >= 0; i--) {
					String dsName = sorted.get(i);
					DataStructureModel model = DATA_STRUCTURE_MODELS.get(dsName);
					try {
						if (model instanceof DataStructureTableModel) {
							if (SqlFactory.getNative(connection).exists(connection, model.getName())) {
								if (SqlFactory.getNative(connection).count(connection, model.getName()) == 0) {
									executeTableDrop(connection, (DataStructureTableModel) model);
								} else {
									logger.warn(format("Table [{0}] cannot be deleted during the update process, because it is not empty", dsName));
								}
							}
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						errors.add(e.getMessage());
					}
				}
				
				
				// process tables in the proper order
				for (String dsName : sorted) {
					DataStructureModel model = DATA_STRUCTURE_MODELS.get(dsName);
					try {
						if (!SqlFactory.getNative(connection).exists(connection, model.getName())) {
							if (model instanceof DataStructureTableModel) {
								executeTableCreate(connection, (DataStructureTableModel) model);
							}
						} else {
							logger.warn(format("Table [{0}] already exists during the update process", dsName));
							if (model instanceof DataStructureTableModel && SqlFactory.getNative(connection).count(connection, model.getName()) != 0) {
								executeTableAlter(connection, (DataStructureTableModel) model);
							}
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						errors.add(e.getMessage());
					}
				}
				
				// process tables foreign keys
				for (String dsName : sorted) {
					DataStructureModel model = DATA_STRUCTURE_MODELS.get(dsName);
					try {
						if (model instanceof DataStructureTableModel) {
							executeTableForeignKeysCreate(connection, (DataStructureTableModel) model);
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						errors.add(e.getMessage());
					}
				}
				
				// process views in the proper order
				for (String dsName : sorted) {
					DataStructureModel model = DATA_STRUCTURE_MODELS.get(dsName);
					try {
						if (!SqlFactory.getNative(connection).exists(connection, model.getName())) {
							if (model instanceof DataStructureViewModel) {
								executeViewCreate(connection, (DataStructureViewModel) model);
							}
						} else {
							logger.warn(format("View [{0}] already exists during the update process", dsName));
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						errors.add(e.getMessage());
					}
				}

			} finally {
				if (connection != null) {
					connection.close();
				}
			}
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
			errors.add(e.getMessage());
		} finally {
			logger.error(concatenateListOfStrings(errors, "\n---\n"));
		}
	}

	/**
	 * Execute table update.
	 *
	 * @param connection
	 *            the connection
	 * @param tableModel
	 *            the table model
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void executeTableUpdate(Connection connection, DataStructureTableModel tableModel) throws SQLException {
		logger.info("Processing Update Table: " + tableModel.getName());
		if (SqlFactory.getNative(connection).exists(connection, tableModel.getName())) {
			if (SqlFactory.getNative(connection).count(connection, tableModel.getName()) == 0) {
				executeTableDrop(connection, tableModel);
				executeTableCreate(connection, tableModel);
			} else {
				executeTableAlter(connection, tableModel);
			}
		} else {
			executeTableCreate(connection, tableModel);
		}
	}

	/**
	 * Execute table create.
	 *
	 * @param connection
	 *            the connection
	 * @param tableModel
	 *            the table model
	 * @throws SQLException
	 *             the SQL exception
	 */
	private void executeTableCreate(Connection connection, DataStructureTableModel tableModel) throws SQLException {
		TableCreateProcessor.execute(connection, tableModel, true);
	}
	
	/**
	 * Execute table foreign keys create.
	 *
	 * @param connection
	 *            the connection
	 * @param tableModel
	 *            the table model
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void executeTableForeignKeysCreate(Connection connection, DataStructureTableModel tableModel) throws SQLException {
		TableForeignKeysCreateProcessor.execute(connection, tableModel);
	}

	/**
	 * Execute table alter.
	 *
	 * @param connection
	 *            the connection
	 * @param tableModel
	 *            the table model
	 * @throws SQLException 
	 */
	private void executeTableAlter(Connection connection, DataStructureTableModel tableModel) throws SQLException {
//		throw new NotImplementedException("Altering of a non-empty table is not implemented yet.");
		TableAlterProcessor.execute(connection, tableModel);
	}

	/**
	 * Execute table drop.
	 *
	 * @param connection
	 *            the connection
	 * @param tableModel
	 *            the table model
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void executeTableDrop(Connection connection, DataStructureTableModel tableModel) throws SQLException {
		TableDropProcessor.execute(connection, tableModel);
	}
	
	/**
	 * Execute table foreign keys drop.
	 *
	 * @param connection
	 *            the connection
	 * @param tableModel
	 *            the table model
	 * @throws SQLException
	 *             the SQL exception
	 */
	private void executeTableForeignKeysDrop(Connection connection, DataStructureTableModel tableModel) throws SQLException {
		TableForeignKeysDropProcessor.execute(connection, tableModel);
	}

	/**
	 * Execute view create.
	 *
	 * @param connection
	 *            the connection
	 * @param viewModel
	 *            the view model
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void executeViewCreate(Connection connection, DataStructureViewModel viewModel) throws SQLException {
		ViewCreateProcessor.execute(connection, viewModel);
	}

	/**
	 * Execute view drop.
	 *
	 * @param connection
	 *            the connection
	 * @param viewModel
	 *            the view model
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void executeViewDrop(Connection connection, DataStructureViewModel viewModel) throws SQLException {
		ViewDropProcessor.execute(connection, viewModel);
	}

	/**
	 * Concatenate list of strings.
	 *
	 * @param list
	 *            the list
	 * @param separator
	 *            the separator
	 * @return the string
	 */
	private static String concatenateListOfStrings(List<String> list, String separator) {
		StringBuffer buff = new StringBuffer();
		for (String s : list) {
			buff.append(s).append(separator);
		}
		return buff.toString();
	}

	// Content

	private static final String COLUMN_NAME = "COLUMN_NAME";

	private void updateDatabaseContent() {

		// Replace
		for (String dsName : DATA_STRUCTURE_REPLACE_MODELS.keySet()) {
			DataStructureDataReplaceModel model = DATA_STRUCTURE_REPLACE_MODELS.get(dsName);
			try {
				executeReplaceUpdate(model);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

		// Append
		for (String dsName : DATA_STRUCTURE_APPEND_MODELS.keySet()) {
			DataStructureDataAppendModel model = DATA_STRUCTURE_APPEND_MODELS.get(dsName);
			try {
				executeAppendUpdate(model);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

		// Delete
		for (String dsName : DATA_STRUCTURE_DELETE_MODELS.keySet()) {
			DataStructureDataDeleteModel model = DATA_STRUCTURE_DELETE_MODELS.get(dsName);
			try {
				executeDeleteUpdate(model);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

		// Update
		for (String dsName : DATA_STRUCTURE_UPDATE_MODELS.keySet()) {
			DataStructureDataUpdateModel model = DATA_STRUCTURE_UPDATE_MODELS.get(dsName);
			try {
				executeUpdateUpdate(model);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

	}

	/**
	 * Process the data rows in the 'replace' mode
	 *
	 * @param model
	 *            the model
	 * @throws Exception
	 *             in case of database error
	 */
	public void executeReplaceUpdate(DataStructureDataReplaceModel model) throws Exception {
		logger.info("Processing rows in mode 'replace': " + model.getLocation());
		String tableName = model.getName();
		deleteAllDataFromTable(tableName);

		byte[] content = model.getContent().getBytes();

		if (content.length != 0) {
			TableImporter tableDataInserter = new TableImporter(dataSource, content, tableName);
			tableDataInserter.insert();
			moveSequence(tableName); // move the sequence just in case
		}
	}

	/**
	 * Process the data rows in the 'append' mode
	 *
	 * @param model
	 *            the model
	 * @throws Exception
	 *             in case of database error
	 */
	public void executeAppendUpdate(DataStructureDataAppendModel model) throws Exception {
		logger.info("Processing rows in mode 'append': " + model.getLocation());
		String tableName = model.getName();
		int tableRowsCount = getTableRowsCount(tableName);
		if (tableRowsCount == 0) {
			byte[] content = model.getContent().getBytes();
			if (content.length != 0) {
				TableImporter tableDataInserter = new TableImporter(dataSource, content, tableName);
				tableDataInserter.insert();
				moveSequence(tableName); // move the sequence, to be able to add more records after the initial import
			}
		}
	}

	/**
	 * Process the data rows in the 'delete' mode
	 *
	 * @param model
	 *            the model
	 * @throws Exception
	 *             in case of database error
	 */
	public void executeDeleteUpdate(DataStructureDataDeleteModel model) throws Exception {
		logger.info("Processing rows in mode 'delete': " + model.getLocation());
		String tableName = model.getName();
		String primaryKey = getPrimaryKey(tableName);
		byte[] content = model.getContent().getBytes();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8));
		String firstLine = reader.readLine();
		if ((firstLine != null) && firstLine.trim().equals("*")) {
			deleteAllDataFromTable(tableName);
		} else {
			deleteRowsDataFromTable(tableName, primaryKey, content);
		}
	}

	/**
	 * Process the data rows in the 'update' mode
	 *
	 * @param model
	 *            the model
	 * @throws Exception
	 *             in case of database error
	 */
	public void executeUpdateUpdate(DataStructureDataUpdateModel model) throws Exception {
		logger.info("Processing rows in mode 'update': " + model.getLocation());
		String tableName = model.getName();
		String primaryKey = getPrimaryKey(tableName);
		byte[] content = model.getContent().getBytes();
		updateRowsDataInTable(DataStructuresUtils.getCaseSensitiveTableName(tableName), primaryKey, content);
	}

	private void deleteAllDataFromTable(String tableName) throws Exception {
		Connection connection = null;
		try {
			connection = dataSource.getConnection();
			String sql = SqlFactory.getNative(connection).delete().from(DataStructuresUtils.getCaseSensitiveTableName(tableName)).build();
			PreparedStatement deleteStatement = connection.prepareStatement(sql);
			deleteStatement.execute();
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	private int getTableRowsCount(String tableName) throws Exception {
		Connection connection = null;
		try {
			connection = dataSource.getConnection();
			String sql = SqlFactory.getNative(connection).select().column("COUNT(*)").from(tableName).build();
			PreparedStatement countStatement = connection.prepareStatement(sql);
			ResultSet rs = countStatement.executeQuery();
			if (rs.next()) {
				int count = rs.getInt(1);
				return count;
			}
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
		return -1;
	}

	private String getPrimaryKey(String tableName) throws Exception {
		String result = null;
		Connection connection = null;
		try {
			connection = this.dataSource.getConnection();
			ResultSet primaryKeys = TableMetadataHelper.getPrimaryKeys(connection, tableName);
			List<String> primaryKeysList = new ArrayList<String>();
			while (primaryKeys.next()) {
				String columnName = primaryKeys.getString(COLUMN_NAME);
				primaryKeysList.add(columnName);
			}
			if (primaryKeysList.size() == 0) {
				throw new Exception(String.format("Trying to manipulate data records for a table without a primary key: %s", tableName));
			}
			if (primaryKeysList.size() > 1) {
				throw new Exception(
						String.format("Trying to manipulate data records for a table with more than one columns in the primary key: %s", tableName));
			}
			result = primaryKeysList.get(0);
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
		return result;
	}

	private void deleteRowsDataFromTable(String tableName, String primaryKey, byte[] fileContent) throws Exception {
		Connection connection = null;
		try {
			connection = dataSource.getConnection();
			List<String[]> records = TableDataReader.readRecords(new ByteArrayInputStream(fileContent));
			for (String[] record : records) {
				if (record.length > 0) {
					String sql = SqlFactory.getNative(connection).delete().from(tableName).where(primaryKey + " = ?").build();
					PreparedStatement deleteStatement = connection.prepareStatement(sql);
					deleteStatement.setObject(1, record[0]);
					deleteStatement.execute();
				} else {
					logger.error(String.format("Skipping deletion of an empty data row for table: %s", tableName));
				}
			}

		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	private void updateRowsDataInTable(String tableName, String primaryKey, byte[] fileContent) throws Exception {
		Connection connection = null;
		try {
			connection = dataSource.getConnection();
			List<String[]> records = TableDataReader.readRecords(new ByteArrayInputStream(fileContent));
			for (String[] record : records) {
				if (record.length > 0) {
					String sql = SqlFactory.getNative(connection).select().column("*").from(tableName).where(primaryKey + " = ?").build();
					PreparedStatement stmt = connection.prepareStatement(sql);
					stmt.setObject(1, record[0]);
					ResultSet rs = stmt.executeQuery();
					if (!rs.next()) {
						StringBuffer buff = new StringBuffer();
						for (String value : record) {
							buff.append(value).append(TableExporter.DATA_DELIMETER);
						}
						buff.deleteCharAt(buff.length() - 1);
						buff.append("\n");
						TableImporter tableDataInserter = new TableImporter(dataSource, buff.toString().getBytes(), tableName);
						tableDataInserter.insert();
					}
				} else {
					logger.error(String.format("Skipping update of an empty data row for table: %s", tableName));
				}
			}

		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	protected void moveSequence(String tableName) throws Exception, SQLException {

		int tableRowsCount;
		tableRowsCount = getTableRowsCount(tableName);

		Connection connection = null;
		try {
			connection = dataSource.getConnection();
			connection.setAutoCommit(false);

			PersistenceManager<Identity> persistenceManager = new PersistenceManager<Identity>();
			if (!persistenceManager.tableExists(connection, Identity.class)) {
				persistenceManager.tableCreate(connection, Identity.class);
			}
			Identity identity = persistenceManager.find(connection, Identity.class, tableName);
			if (identity == null) {
				identity = new Identity();
				identity.setTable(tableName);
				identity.setValue(++tableRowsCount);
				persistenceManager.insert(connection, identity);
				return;
			}
			try {
				try {
					identity = persistenceManager.lock(connection, Identity.class, tableName);
					identity.setValue(++tableRowsCount);
					persistenceManager.update(connection, identity);
				} finally {
					connection.commit();
				}
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

}
