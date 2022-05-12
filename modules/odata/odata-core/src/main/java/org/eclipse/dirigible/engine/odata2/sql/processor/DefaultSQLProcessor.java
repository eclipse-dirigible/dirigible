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
package org.eclipse.dirigible.engine.odata2.sql.processor;

import org.apache.olingo.odata2.api.edm.*;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2EventHandler;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2Exception;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLInterceptor;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBindingProvider;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static org.apache.olingo.odata2.api.commons.HttpStatusCodes.INTERNAL_SERVER_ERROR;

public class DefaultSQLProcessor extends AbstractSQLProcessor {

    public static final String DEFAULT_DATA_SOURCE_CONTEXT_KEY = DataSource.class.getName();
    private static final Logger LOG = LoggerFactory.getLogger(DefaultSQLProcessor.class);
    private final SQLQueryBuilder sqlQueryBuilder;

    public DefaultSQLProcessor(EdmTableBindingProvider tableMappingProvider) {
        this.sqlQueryBuilder = new SQLQueryBuilder(tableMappingProvider);
    }

    public DefaultSQLProcessor(SQLQueryBuilder queryBuilder) {
        this.sqlQueryBuilder = queryBuilder;
    }

    public DefaultSQLProcessor(EdmTableBindingProvider tableMappingProvider, OData2EventHandler odata2EventHandler) {
        super(odata2EventHandler);
        this.sqlQueryBuilder = new SQLQueryBuilder(tableMappingProvider);
    }

    public DefaultSQLProcessor(SQLQueryBuilder queryBuilder, OData2EventHandler odata2EventHandler) {
        super(odata2EventHandler);
        this.sqlQueryBuilder = queryBuilder;
    }

    public void addInterceptors(List<SQLInterceptor> interceptors) {
        for (SQLInterceptor sqlInterceptor : interceptors) {
            this.sqlQueryBuilder.addInterceptor(sqlInterceptor);
        }
    }

    @Override
    public Map<String, Object> onCustomizeExpandedNavigatonProperty(EdmStructuralType entityType, EdmStructuralType expandType,
                                                                    Map<String, Object> expandInstance) {
        LOG.debug("Override this method to customize the navigation properties feed.");
        return expandInstance;

    }

    @Override
    public Object onCustomizePropertyValue(EdmStructuralType entityType, EdmProperty property, Object entityInstance, Object value) throws EdmException {
        EdmType propertyType = property.getType();

        if (value instanceof String && !propertyType.equals(EdmSimpleTypeKind.String.getEdmSimpleTypeInstance())) {
            if (propertyType.equals(EdmSimpleTypeKind.Byte.getEdmSimpleTypeInstance())) {
                value = Short.parseShort(value.toString());
            }
            else if (propertyType.equals(EdmSimpleTypeKind.SByte.getEdmSimpleTypeInstance())) {
                value = Byte.parseByte(value.toString());
            }
            else if (propertyType.equals(EdmSimpleTypeKind.Int16.getEdmSimpleTypeInstance())) {
                value = Short.parseShort(value.toString());
            }
            else if (propertyType.equals(EdmSimpleTypeKind.Int32.getEdmSimpleTypeInstance())) {
                value = Integer.parseInt(value.toString());
            }
            else if (propertyType.equals(EdmSimpleTypeKind.Int64.getEdmSimpleTypeInstance())) {
                value = Long.parseLong(value.toString());
            }
            else if (propertyType.equals(EdmSimpleTypeKind.Single.getEdmSimpleTypeInstance())) {
                value = Float.parseFloat(value.toString());
            }
            else if (propertyType.equals(EdmSimpleTypeKind.Double.getEdmSimpleTypeInstance())) {
                value = Double.parseDouble(value.toString());
            }
            else if (propertyType.equals(EdmSimpleTypeKind.Decimal.getEdmSimpleTypeInstance())) {
                value = new BigDecimal(value.toString());
            }
            else if (propertyType.equals(EdmSimpleTypeKind.Boolean.getEdmSimpleTypeInstance())) {
                value = Boolean.parseBoolean(value.toString());
            }
            else if (propertyType.equals(EdmSimpleTypeKind.Time.getEdmSimpleTypeInstance())) {
                value = Time.valueOf(value.toString());
            }
            else if (propertyType.equals(EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance())) {
                value = Timestamp.valueOf(value.toString());
            }
            else if (propertyType.equals(EdmSimpleTypeKind.DateTimeOffset.getEdmSimpleTypeInstance())) {
                value = Date.valueOf(value.toString());
            }
        }

        return value;
    }

    @Override
    public SQLQueryBuilder getSQLQueryBuilder() {
        return sqlQueryBuilder;
    }

    @Override
    public DataSource getDataSource() {
        ODataContext context = this.getContext();
        if (context.isInBatchMode()) {
            ODataContext parent = context.getBatchParentContext();
            while (parent.getBatchParentContext() != null) {
                parent = parent.getBatchParentContext();
            }
            context = parent;
        }
        DataSource ds = super.getDataSource();
        if (ds == null) {
            ds = (DataSource) context.getParameter(DEFAULT_DATA_SOURCE_CONTEXT_KEY);
        }
        if (ds == null) {
            throw new OData2Exception("Unable to get the dataSource from the context. Please provide the datasource.",
                    INTERNAL_SERVER_ERROR);
        }
        return ds;
    }
}
