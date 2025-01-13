/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.factory;

import static org.eclipse.dirigible.engine.odata2.sql.processor.DefaultSQLProcessor.DEFAULT_DATA_SOURCE_CONTEXT_KEY;
import java.io.InputStream;
import java.util.ServiceLoader;
import javax.sql.DataSource;
import org.apache.olingo.odata2.api.ODataCallback;
import org.apache.olingo.odata2.api.ODataService;
import org.apache.olingo.odata2.api.ODataServiceFactory;
import org.apache.olingo.odata2.api.edm.provider.EdmProvider;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataErrorCallback;
import org.apache.olingo.odata2.api.processor.ODataErrorContext;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.core.edm.provider.EdmxProvider;
import org.eclipse.dirigible.commons.api.context.InvalidStateException;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.odata.service.ODataMetadataService;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2EventHandler;
import org.eclipse.dirigible.engine.odata2.sql.processor.DefaultSQLProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A factory for creating DirigibleODataService objects.
 */
@Component
public class DirigibleODataServiceFactory extends ODataServiceFactory {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DirigibleODataServiceFactory.class);

    /** The data sources manager. */
    private static DataSourcesManager dataSourcesManager;

    /** The odata metadata service. */
    private static ODataMetadataService odataMetadataService;

    /**
     * Instantiates a new dirigible O data service factory.
     */
    public DirigibleODataServiceFactory() {
        // called by ODataServlet
    }

    /**
     * Instantiates a new dirigible O data service factory.
     *
     * @param dataSourcesManager the data sources manager
     * @param odataMetadataService the odata metadata service
     */
    @Autowired
    public DirigibleODataServiceFactory(DataSourcesManager dataSourcesManager, ODataMetadataService odataMetadataService) {
        DirigibleODataServiceFactory.dataSourcesManager = dataSourcesManager;
        DirigibleODataServiceFactory.odataMetadataService = odataMetadataService;
    }

    /**
     * Gets the edm table mapping provider.
     *
     * @return the edm table mapping provider
     * @throws ODataException the o data exception
     */
    public ODataEdmTableMappingProvider getEdmTableMappingProvider() throws ODataException {
        return new ODataEdmTableMappingProvider();
    }

    /**
     * Creates a new DirigibleODataService object.
     *
     * @param ctx the ctx
     * @return the o data service
     * @throws ODataException the o data exception
     */
    @Override
    public ODataService createService(ODataContext ctx) throws ODataException {
        try {
            EdmProvider edmProvider = new EdmxProvider();
            InputStream metadata = odataMetadataService.getMetadata();
            ((EdmxProvider) edmProvider).parse(metadata, false);

            setDefaultDataSource(ctx);

            DefaultSQLProcessor singleProcessor = new DefaultSQLProcessor(getEdmTableMappingProvider(), getEventHandler());

            return createODataSingleProcessorService(edmProvider, singleProcessor);
        } catch (ODataException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            throw new ODataException(e);
        }
    }

    /**
     * Gets the callback.
     *
     * @param <T> the generic type
     * @param callbackInterface the callback interface
     * @return the callback
     */
    @Override
    public <T extends ODataCallback> T getCallback(Class<T> callbackInterface) {
        if (callbackInterface.isAssignableFrom(ODataErrorCallback.class)) {
            return (T) new ODataDefaulErrorCallback();
        }
        return super.getCallback(callbackInterface);
    }

    /**
     * The Class ODataDefaulErrorCallback.
     */
    private class ODataDefaulErrorCallback implements ODataErrorCallback {

        /**
         * Handle error.
         *
         * @param context the context
         * @return the o data response
         * @throws ODataApplicationException the o data application exception
         */
        @Override
        public ODataResponse handleError(ODataErrorContext context) throws ODataApplicationException {
            if (logger.isErrorEnabled()) {
                logger.error(context.getMessage(), context.getException());
            }
            return EntityProvider.writeErrorDocument(context);
        }
    }

    /**
     * Sets the default data source.
     *
     * @param ctx the new default data source
     * @throws ODataException the o data exception
     */
    private void setDefaultDataSource(ODataContext ctx) throws ODataException {
        DataSource dataSource;
        dataSource = dataSourcesManager.getDefaultDataSource();
        ctx.setParameter(DEFAULT_DATA_SOURCE_CONTEXT_KEY, dataSource);
    }

    /**
     * Gets the event handler.
     *
     * @return the event handler
     */
    private OData2EventHandler getEventHandler() {
        ServiceLoader<OData2EventHandler> odata2EventHandlers = ServiceLoader.load(OData2EventHandler.class);

        String odata2EventHandlerName = Configuration.get(OData2EventHandler.DIRIGIBLE_ODATA_EVENT_HANDLER_NAME,
                OData2EventHandler.DEFAULT_ODATA_EVENT_HANDLER_NAME);
        for (OData2EventHandler next : odata2EventHandlers) {
            if (next.getName()
                    .equals(odata2EventHandlerName)) {
                return next;
            }
        }

        throw new InvalidStateException("No odata2 event handler found with name " + odata2EventHandlerName);
    }

}
