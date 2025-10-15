/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.csvim.synchronizer;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantBaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.data.csvim.domain.CsvFile;
import org.eclipse.dirigible.components.data.csvim.domain.Csvim;
import org.eclipse.dirigible.components.data.csvim.processor.CsvimProcessor;
import org.eclipse.dirigible.components.data.csvim.service.CsvFileService;
import org.eclipse.dirigible.components.data.csvim.service.CsvimService;
import org.eclipse.dirigible.components.data.sources.config.SystemDataSourceName;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The Class CSVIM Synchronizer.
 */
@Component
@Order(SynchronizersOrder.CSVIM)
public class CsvimSynchronizer extends MultitenantBaseSynchronizer<Csvim, Long> {

    /**
     * The Constant FILE_EXTENSION_CSVIM.
     */
    public static final String FILE_EXTENSION_CSVIM = ".csvim";
    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(CsvimSynchronizer.class);

    /** The csvim service. */
    private final CsvimService csvimService;

    private final CsvFileService csvFileService;

    /** The datasources manager. */
    private final DataSourcesManager datasourcesManager;

    /** The csvim processor. */
    private final CsvimProcessor csvimProcessor;

    /** The system data source name. */
    private final String systemDataSourceName;

    /** The callback. */
    private SynchronizerCallback callback;

    /**
     * Instantiates a new csvim synchronizer.
     *
     * @param csvimService the csvimsyncrhonizer service
     * @param datasourcesManager the datasources manager
     * @param csvimProcessor the csvim processor
     * @param systemDataSourceName the system data source name
     */
    @Autowired
    public CsvimSynchronizer(CsvimService csvimService, DataSourcesManager datasourcesManager, CsvimProcessor csvimProcessor,
            @SystemDataSourceName String systemDataSourceName, CsvFileService csvFileService) {
        this.csvimService = csvimService;
        this.csvFileService = csvFileService;
        this.datasourcesManager = datasourcesManager;
        this.csvimProcessor = csvimProcessor;
        this.systemDataSourceName = systemDataSourceName;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the type
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return Csvim.ARTEFACT_TYPE.equals(type);
    }

    /**
     * Load.
     *
     * @param location the location
     * @param content the content
     * @return the csvim
     * @throws ParseException the parse exception
     */
    @Override
    protected List<Csvim> parseImpl(String location, byte[] content) throws ParseException {
        Csvim csvim = JsonHelper.fromJson(new String(content, StandardCharsets.UTF_8), Csvim.class);
        validateCSVFiles(csvim);
        Configuration.configureObject(csvim);
        csvim.setLocation(location);
        csvim.setName(FilenameUtils.getBaseName(location));
        csvim.setType(Csvim.ARTEFACT_TYPE);
        csvim.updateKey();
        if (csvim.getFiles() != null) {
            csvim.getFiles()
                 .forEach(csvFile -> {
                     csvFile.setImported(false);
                     csvFile.setCsvim(csvim);
                     csvFile.setLocation(csvim.getLocation() + "/" + csvFile.getFile());
                     csvFile.setType(CsvFile.ARTEFACT_TYPE);
                     csvFile.setName(csvFile.getFile());
                     csvFile.updateKey();
                 });
        }

        try {
            Csvim maybe = getService().findByKey(csvim.getKey());
            if (maybe != null) {
                csvim.setId(maybe.getId());
                csvim.getFiles()
                     .forEach(cf -> {
                         Optional<CsvFile> foundFile = maybe.getFileByKey(cf.getKey());
                         if (foundFile.isPresent()) {
                             cf.setId(foundFile.get()
                                               .getId());
                         }
                     });
            }
            Csvim result = getService().save(csvim);
            return List.of(result);
        } catch (Exception e) {
            logger.error("Failed to save CSVIM [{}], content: [{}]", csvim, new String(content), e);
            throw new ParseException(e.getMessage(), 0);
        }
    }

    private void validateCSVFiles(Csvim csvim) throws ParseException {
        List<CsvFile> csvFiles = csvim.getFiles();
        for (CsvFile csvFile : csvFiles) {
            if (csvFile.hasConfiguredLocale() && !csvFile.hasValidLocaleConfigured()) {
                throw new ParseException("CSV file " + csvFile + " in CSVIM " + csvim + " has invalid locale: " + csvFile.getLocale(), 0);
            }
        }
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<Csvim, Long> getService() {
        return csvimService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the csvim
     */
    @Override
    public List<Csvim> retrieve(String location) {
        return getService().findByLocation(location);
    }

    /**
     * Sets the status.
     *
     * @param artefact the artefact
     * @param lifecycle the lifecycle
     * @param error the error
     */
    @Override
    public void setStatus(Csvim artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    /**
     * Complete.
     *
     * @param wrapper the wrapper
     * @param flow the flow
     * @return true, if successful
     */
    @Override
    protected boolean completeImpl(TopologyWrapper<Csvim> wrapper, ArtefactPhase flow) {
        Csvim csvim = wrapper.getArtefact();
        ArtefactLifecycle lifecycle = csvim.getLifecycle();
        try {
            switch (flow) {
                case CREATE:
                    switch (lifecycle) {
                        case FAILED:
                        case CREATED:
                        case NEW: {
                            importCsvim(csvim);
                            callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                            return true;
                        }
                    }

                case UPDATE:
                    switch (lifecycle) {
                        case MODIFIED:
                        case FAILED: {
                            importCsvim(csvim);
                            callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                            return true;
                        }
                    }
                case DELETE:
                    if (csvim.getLifecycle()
                             .equals(ArtefactLifecycle.CREATED)
                            || csvim.getLifecycle()
                                    .equals(ArtefactLifecycle.UPDATED)
                            || csvim.getLifecycle()
                                    .equals(ArtefactLifecycle.FAILED)) {
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                        return true;
                    }
                case START:
                case STOP:
            }

            return true;
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
            return false;
        }
    }

    /**
     * Import csvim.
     *
     * @param csvim the csvim
     * @throws Exception the exception
     */
    private void importCsvim(Csvim csvim) throws Exception {
        List<CsvFile> csvFiles = csvim.getFiles();

        if (csvFiles == null) {
            logger.warn("There are no files in [{}]. Nothing will be imported.", csvim);
            return;
        }
        List<Exception> errors = new ArrayList<>();
        for (CsvFile csvFile : csvFiles) {
            try {
                IResource resource = CsvimProcessor.getCsvResource(csvFile);
                if (!resource.exists()) {
                    throw new Exception("CSV does not exist: " + csvFile.getFile());
                }
                byte[] content = csvimProcessor.getCsvContent(resource);

                csvimProcessor.process(csvFile, content, csvim.getDatasource());

                csvFile.setImported(true);
                csvFileService.save(csvFile);

            } catch (RuntimeException ex) {
                errors.add(ex);
            }
        }
        if (!errors.isEmpty()) {
            CsvimProcessingException ex = new CsvimProcessingException("Failed to import csvim " + csvim.getKey());
            errors.forEach(ex::addSuppressed);
            throw ex;
        }
    }

    /**
     * Cleanup.
     *
     * @param csvim the csvim
     */
    @Override
    public void cleanupImpl(Csvim csvim) {
        try (Connection connection = datasourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            csvimService.delete(csvim);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, csvim, ArtefactLifecycle.DELETED, "Failed to cleanup csvim: " + csvim, e);
        }
    }

    /**
     * Sets the callback.
     *
     * @param callback the new callback
     */
    @Override
    public void setCallback(SynchronizerCallback callback) {
        this.callback = callback;
    }

    /**
     * Gets the file extension.
     *
     * @return the file extension
     */
    @Override
    public String getFileExtension() {
        return FILE_EXTENSION_CSVIM;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return Csvim.ARTEFACT_TYPE;
    }

    /**
     * Checks if is multitenant artefact.
     *
     * @param csvim the csvim
     * @return true, if is multitenant artefact
     */
    @Override
    protected boolean isMultitenantArtefact(Csvim csvim) {
        return !Objects.equals(systemDataSourceName, csvim.getDatasource());
    }
}
