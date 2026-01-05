/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantBaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.domain.View;
import org.eclipse.dirigible.components.data.structures.service.ViewService;
import org.eclipse.dirigible.components.data.structures.synchronizer.view.ViewCreateProcessor;
import org.eclipse.dirigible.components.data.structures.synchronizer.view.ViewDropProcessor;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

/**
 * The Class ViewsSynchronizer.
 */
@Component
@Order(SynchronizersOrder.VIEW)
public class ViewsSynchronizer extends MultitenantBaseSynchronizer<View, Long> {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ViewsSynchronizer.class);

    /** The Constant FILE_EXTENSION_VIEW. */
    private static final String FILE_EXTENSION_VIEW = ".view";

    /** The view service. */
    private final ViewService viewService;

    /** The datasources manager. */
    private final DataSourcesManager datasourcesManager;

    /** The synchronization callback. */
    private SynchronizerCallback callback;

    /**
     * Instantiates a new view synchronizer.
     *
     * @param viewService the view service
     * @param datasourcesManager the datasources manager
     */
    @Autowired
    public ViewsSynchronizer(ViewService viewService, DataSourcesManager datasourcesManager) {
        this.viewService = viewService;
        this.datasourcesManager = datasourcesManager;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the type
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return View.ARTEFACT_TYPE.equals(type);
    }

    /**
     * Load.
     *
     * @param location the location
     * @param content the content
     * @return the list
     * @throws ParseException the parse exception
     */
    @Override
    protected List<View> parseImpl(String location, byte[] content) throws ParseException {
        View view = JsonHelper.fromJson(new String(content, StandardCharsets.UTF_8), View.class);
        Configuration.configureObject(view);
        view.setLocation(location);
        if (view.getKind() == null) {
            view.setKind(view.getType());
        }
        view.setType(View.ARTEFACT_TYPE);
        view.updateKey();

        try {
            View maybe = getService().findByKey(view.getKey());
            if (maybe != null) {
                view.setId(maybe.getId());
            }
            view = getService().save(view);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            if (logger.isErrorEnabled()) {
                logger.error("view: {}", view);
            }
            if (logger.isErrorEnabled()) {
                logger.error("content: {}", new String(content));
            }
            throw new ParseException(e.getMessage(), 0);
        }
        return List.of(view);
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<View, Long> getService() {
        return viewService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<View> retrieve(String location) {
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
    public void setStatus(View artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    /**
     * Complete impl.
     *
     * @param wrapper the wrapper
     * @param flow the flow
     * @return true, if successful
     */
    @Override
    protected boolean completeImpl(TopologyWrapper<View> wrapper, ArtefactPhase flow) {
        View view = wrapper.getArtefact();
        try (Connection connection = datasourcesManager.getDefaultDataSource()
                                                       .getConnection()) {

            switch (flow) {
                case CREATE:
                    if (ArtefactLifecycle.NEW.equals(view.getLifecycle())) {
                        if (!SqlFactory.getNative(connection)
                                       .existsTable(connection, view.getName())) {
                            try {
                                executeViewCreate(connection, view);
                                callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                            } catch (Exception e) {
                                callback.registerState(this, wrapper, ArtefactLifecycle.CREATED, e);
                            }
                        } else {
                            if (logger.isWarnEnabled()) {
                                logger.warn(String.format("View [%s] already exists during the update process", view.getName()));
                            }
                            executeViewUpdate(connection, view);
                            callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                        }
                        callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                    }
                    break;
                case UPDATE:
                    if (ArtefactLifecycle.MODIFIED.equals(view.getLifecycle())) {
                        executeViewUpdate(connection, view);
                        callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                    }
                    if (ArtefactLifecycle.FAILED.equals(view.getLifecycle())) {
                        return false;
                    }
                    break;
                case DELETE:
                    if (ArtefactLifecycle.CREATED.equals(view.getLifecycle()) || ArtefactLifecycle.UPDATED.equals(view.getLifecycle())
                            || ArtefactLifecycle.FAILED.equals(view.getLifecycle())) {
                        if (SqlFactory.getNative(connection)
                                      .existsTable(connection, view.getName())) {
                            executeViewDrop(connection, view);
                            callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                        }
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                    }
                    break;
                case START:
                case STOP:
            }

            return true;
        } catch (SQLException e) {
            callback.addError(e.getMessage());
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
            return false;
        }
    }

    /**
     * Execute view create.
     *
     * @param connection the connection
     * @param viewModel the view model
     * @throws SQLException the SQL exception
     */
    public void executeViewCreate(Connection connection, View viewModel) throws SQLException {
        ViewCreateProcessor.execute(connection, viewModel);
    }

    /**
     * Execute view update.
     *
     * @param connection the connection
     * @param viewModel the view model
     * @throws SQLException the SQL exception
     */
    public void executeViewUpdate(Connection connection, View viewModel) throws SQLException {
        if (logger.isInfoEnabled()) {
            logger.info("Processing Update View: " + viewModel.getName());
        }
        if (SqlFactory.getNative(connection)
                      .existsTable(connection, viewModel.getName())) {
            executeViewDrop(connection, viewModel);
            executeViewCreate(connection, viewModel);
        } else {
            executeViewCreate(connection, viewModel);
        }
    }

    /**
     * Execute view drop.
     *
     * @param connection the connection
     * @param viewModel the view model
     * @throws SQLException the SQL exception
     */
    public void executeViewDrop(Connection connection, View viewModel) throws SQLException {
        ViewDropProcessor.execute(connection, viewModel);
    }

    /**
     * Cleanup.
     *
     * @param view the view
     */
    @Override
    public void cleanupImpl(View view) {
        try (Connection connection = datasourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            getService().delete(view);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, view, ArtefactLifecycle.DELETED, e);
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
        return FILE_EXTENSION_VIEW;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return View.ARTEFACT_TYPE;
    }

}
