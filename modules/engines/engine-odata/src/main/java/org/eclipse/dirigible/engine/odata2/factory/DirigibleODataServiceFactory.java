/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2022 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.factory;

import static org.eclipse.dirigible.engine.odata2.sql.processor.DefaultSQLProcessor.DEFAULT_DATA_SOURCE_CONTEXT_KEY;

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
import org.eclipse.dirigible.api.v3.db.DatabaseFacade;
import org.eclipse.dirigible.commons.api.context.InvalidStateException;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.engine.js.api.IJavascriptEngineExecutor;
import org.eclipse.dirigible.engine.odata2.api.IODataCoreService;
import org.eclipse.dirigible.engine.odata2.handler.ScriptingOData2EventHandler;
import org.eclipse.dirigible.engine.odata2.mapping.DirigibleEdmTableMappingProvider;
import org.eclipse.dirigible.engine.odata2.service.ODataCoreService;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2EventHandler;
import org.eclipse.dirigible.engine.odata2.sql.processor.DefaultSQLProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

public class DirigibleODataServiceFactory extends ODataServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSQLProcessor.class);

    private static IODataCoreService odataCoreService = new ODataCoreService();

    @Override
    public ODataService createService(ODataContext ctx) throws ODataException {
        try {
            EdmProvider edmProvider = new EdmxProvider();
            ((EdmxProvider) edmProvider).parse(odataCoreService.getMetadata(), false);

            setDefaultDataSource(ctx);

            DirigibleEdmTableMappingProvider tableMappingProvider = new DirigibleEdmTableMappingProvider();

            DefaultSQLProcessor singleProcessor = new DefaultSQLProcessor(tableMappingProvider, getEventHandler());

            return createODataSingleProcessorService(edmProvider, singleProcessor);
        } catch (org.eclipse.dirigible.engine.odata2.api.ODataException e) {
            logger.error(e.getMessage(), e);
            throw new ODataException(e);
        }
    }

    @Override
    public <T extends ODataCallback> T getCallback(Class<T> callbackInterface) {
        if (callbackInterface.isAssignableFrom(ODataErrorCallback.class)) {
            return (T) new ODataDefaulErrorCallback();
        }
        return super.getCallback(callbackInterface);
    }

    private class ODataDefaulErrorCallback implements ODataErrorCallback {
        @Override
        public ODataResponse handleError(ODataErrorContext context) throws ODataApplicationException {
            logger.error(context.getMessage(), context.getException());
            return EntityProvider.writeErrorDocument(context);
        }
    }

    private void setDefaultDataSource(ODataContext ctx) throws ODataException {
        DataSource dataSource;
        dataSource = DatabaseFacade.getDefaultDataSource();
        ctx.setParameter(DEFAULT_DATA_SOURCE_CONTEXT_KEY, dataSource);
    }

    private OData2EventHandler getEventHandler() {
        ServiceLoader<OData2EventHandler> odata2EventHandlers = ServiceLoader.load(OData2EventHandler.class);

        String odata2EventHandlerName = Configuration.get(OData2EventHandler.DIRIGIBLE_ODATA_EVENT_HANDLER_NAME,
                OData2EventHandler.DEFAULT_ODATA_EVENT_HANDLER_NAME);
        for (OData2EventHandler next : odata2EventHandlers) {
            if(next.getName().equals(odata2EventHandlerName)) {
                return next;
            }
        }

        throw new InvalidStateException("No odata2 event handler found with name " + odata2EventHandlerName);
    }
}
