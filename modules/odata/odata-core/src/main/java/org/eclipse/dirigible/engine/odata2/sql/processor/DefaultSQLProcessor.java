/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.processor;

import com.sap.db.jdbc.HanaClob;
import com.sap.db.jdbc.HanaBlob;
import org.apache.commons.io.IOUtils;
import org.apache.olingo.odata2.api.edm.*;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.core.edm.EdmDouble;
import org.apache.olingo.odata2.core.edm.EdmInt16;
import org.apache.olingo.odata2.core.edm.EdmInt32;
import org.apache.olingo.odata2.core.edm.EdmInt64;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2EventHandler;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2Exception;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLInterceptor;
import org.eclipse.dirigible.engine.odata2.sql.binding.EdmTableBindingProvider;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.apache.olingo.odata2.api.commons.HttpStatusCodes.INTERNAL_SERVER_ERROR;

/**
 * The Class DefaultSQLProcessor.
 */
public class DefaultSQLProcessor extends AbstractSQLProcessor {

    /** The Constant DEFAULT_DATA_SOURCE_CONTEXT_KEY. */
    public static final String DEFAULT_DATA_SOURCE_CONTEXT_KEY = DataSource.class.getName();

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultSQLProcessor.class);

    /** The sql query builder. */
    private final SQLQueryBuilder sqlQueryBuilder;

    /**
     * Instantiates a new default SQL processor.
     *
     * @param tableMappingProvider the table mapping provider
     */
    public DefaultSQLProcessor(EdmTableBindingProvider tableMappingProvider) {
        this.sqlQueryBuilder = new SQLQueryBuilder(tableMappingProvider);
    }

    /**
     * Instantiates a new default SQL processor.
     *
     * @param queryBuilder the query builder
     */
    public DefaultSQLProcessor(SQLQueryBuilder queryBuilder) {
        this.sqlQueryBuilder = queryBuilder;
    }

    /**
     * Instantiates a new default SQL processor.
     *
     * @param tableMappingProvider the table mapping provider
     * @param odata2EventHandler the odata 2 event handler
     */
    public DefaultSQLProcessor(EdmTableBindingProvider tableMappingProvider, OData2EventHandler odata2EventHandler) {
        super(odata2EventHandler);
        this.sqlQueryBuilder = new SQLQueryBuilder(tableMappingProvider);
    }

    /**
     * Instantiates a new default SQL processor.
     *
     * @param queryBuilder the query builder
     * @param odata2EventHandler the odata 2 event handler
     */
    public DefaultSQLProcessor(SQLQueryBuilder queryBuilder, OData2EventHandler odata2EventHandler) {
        super(odata2EventHandler);
        this.sqlQueryBuilder = queryBuilder;
    }

    /**
     * Adds the interceptors.
     *
     * @param interceptors the interceptors
     */
    public void addInterceptors(List<SQLInterceptor> interceptors) {
        for (SQLInterceptor sqlInterceptor : interceptors) {
            this.sqlQueryBuilder.addInterceptor(sqlInterceptor);
        }
    }

    /**
     * On customize expanded navigaton property.
     *
     * @param entityType the entity type
     * @param expandType the expand type
     * @param expandInstance the expand instance
     * @return the map
     */
    @Override
    public Map<String, Object> onCustomizeExpandedNavigatonProperty(EdmStructuralType entityType, EdmStructuralType expandType,
            Map<String, Object> expandInstance) {
        LOG.debug("Override this method to customize the navigation properties feed.");
        return expandInstance;

    }

    /**
     * On customize property value.
     *
     * @param entityType the entity type
     * @param property the property
     * @param entityInstance the entity instance
     * @param value the value
     * @return the object
     * @throws EdmException the edm exception
     * @throws SQLException the SQL exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public Object onCustomizePropertyValue(EdmStructuralType entityType, EdmProperty property, Object entityInstance, Object value)
            throws EdmException, SQLException, IOException {
        EdmType propertyType = property.getType();

        if (value instanceof String && !propertyType.equals(EdmSimpleTypeKind.String.getEdmSimpleTypeInstance())) {
            if (propertyType.equals(EdmSimpleTypeKind.Byte.getEdmSimpleTypeInstance())) {
                value = Short.parseShort(value.toString());
            } else if (propertyType.equals(EdmSimpleTypeKind.SByte.getEdmSimpleTypeInstance())) {
                value = Byte.parseByte(value.toString());
            } else if (propertyType.equals(EdmSimpleTypeKind.Int16.getEdmSimpleTypeInstance())) {
                value = Short.parseShort(value.toString());
            } else if (propertyType.equals(EdmSimpleTypeKind.Int32.getEdmSimpleTypeInstance())) {
                value = Integer.parseInt(value.toString());
            } else if (propertyType.equals(EdmSimpleTypeKind.Int64.getEdmSimpleTypeInstance())) {
                value = Long.parseLong(value.toString());
            } else if (propertyType.equals(EdmSimpleTypeKind.Single.getEdmSimpleTypeInstance())) {
                value = Float.parseFloat(value.toString());
            } else if (propertyType.equals(EdmSimpleTypeKind.Double.getEdmSimpleTypeInstance())) {
                value = Double.parseDouble(value.toString());
            } else if (propertyType.equals(EdmSimpleTypeKind.Decimal.getEdmSimpleTypeInstance())) {
                value = new BigDecimal(value.toString());
            } else if (propertyType.equals(EdmSimpleTypeKind.Boolean.getEdmSimpleTypeInstance())) {
                value = Boolean.parseBoolean(value.toString());
            } else if (propertyType.equals(EdmSimpleTypeKind.Time.getEdmSimpleTypeInstance())) {
                value = Time.valueOf(value.toString());
            } else if (propertyType.equals(EdmSimpleTypeKind.DateTime.getEdmSimpleTypeInstance())) {
                value = Timestamp.valueOf(value.toString());
            } else if (propertyType.equals(EdmSimpleTypeKind.DateTimeOffset.getEdmSimpleTypeInstance())) {
                value = Date.valueOf(value.toString());
            }
        } else if (value instanceof BigDecimal) {
            BigDecimal dec = (BigDecimal) value;
            if (property.getType()
                        .equals(EdmInt32.getInstance())) {
                return dec.toBigInteger()
                          .intValue();
            } else if (property.getType()
                               .equals(EdmInt64.getInstance())) {
                return dec.toBigInteger()
                          .longValue();
            } else if (property.getType()
                               .equals(EdmInt16.getInstance())) {
                return dec.toBigInteger()
                          .shortValue();
            } else if (property.getType()
                               .equals(EdmDouble.getInstance())) {
                return dec.doubleValue();
            }
        } else if (value instanceof HanaClob) {
            return IOUtils.toString(((HanaClob) value).getCharacterStream());
        } else if (value instanceof HanaBlob) {
            return IOUtils.toString(((HanaBlob) value).getBinaryStream(), StandardCharsets.UTF_8);
        }

        return value;
    }

    /**
     * Gets the SQL query builder.
     *
     * @return the SQL query builder
     */
    @Override
    public SQLQueryBuilder getSQLQueryBuilder() {
        return sqlQueryBuilder;
    }

    /**
     * Gets the data source.
     *
     * @return the data source
     */
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
