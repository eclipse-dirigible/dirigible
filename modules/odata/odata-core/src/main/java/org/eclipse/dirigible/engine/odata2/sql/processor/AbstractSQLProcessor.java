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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.olingo.odata2.api.batch.BatchHandler;
import org.apache.olingo.odata2.api.batch.BatchRequestPart;
import org.apache.olingo.odata2.api.batch.BatchResponsePart;
import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.commons.InlineCount;
import org.apache.olingo.odata2.api.edm.*;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderBatchProperties;
import org.apache.olingo.odata2.api.ep.EntityProviderReadProperties;
import org.apache.olingo.odata2.api.ep.EntityProviderWriteProperties;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataBadRequestException;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.exception.ODataNotFoundException;
import org.apache.olingo.odata2.api.exception.ODataNotImplementedException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataRequest;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.processor.ODataSingleProcessor;
import org.apache.olingo.odata2.api.uri.KeyPredicate;
import org.apache.olingo.odata2.api.uri.NavigationPropertySegment;
import org.apache.olingo.odata2.api.uri.PathInfo;
import org.apache.olingo.odata2.api.uri.UriInfo;
import org.apache.olingo.odata2.api.uri.info.*;
import org.apache.olingo.odata2.core.commons.ContentType;
import org.apache.olingo.odata2.core.uri.KeyPredicateImpl;
import org.apache.olingo.odata2.core.uri.UriInfoImpl;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2EventHandler;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLProcessor;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatement;
import org.eclipse.dirigible.engine.odata2.sql.api.SQLStatementParam;
import org.eclipse.dirigible.engine.odata2.sql.builder.*;
import org.eclipse.dirigible.engine.odata2.sql.utils.OData2Utils;
import org.eclipse.dirigible.engine.odata2.sql.utils.SingleConnectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.eclipse.dirigible.engine.odata2.sql.builder.EdmUtils.getProperties;
import static org.eclipse.dirigible.engine.odata2.sql.builder.EdmUtils.getSelectedProperties;
import static org.eclipse.dirigible.engine.odata2.sql.utils.OData2Utils.hasExpand;

/**
 * The Class AbstractSQLProcessor.
 */
public abstract class AbstractSQLProcessor extends ODataSingleProcessor implements SQLProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(AbstractSQLProcessor.class);
    /** The Constant SQL_BUILDER_CONTEXT_KEY. */
    private static final String SQL_BUILDER_CONTEXT_KEY = "sqlBuilder";
    /** The Constant SQL_CONTEXT_CONTEXT_KEY. */
    private static final String SQL_CONTEXT_CONTEXT_KEY = "sqlContext";
    /** The Constant DATASOURCE_CONTEXT_KEY. */
    private static final String DATASOURCE_CONTEXT_KEY = "datasource";
    /** The Constant ODATA_CONTEXT_CONTEXT_KEY. */
    private static final String ODATA_CONTEXT_CONTEXT_KEY = "oDataContext";
    /** The Constant MAPPED_KEYS_CONTEXT_KEY. */
    private static final String MAPPED_KEYS_CONTEXT_KEY = "mappedKeys";
    /** The Constant ENTRY_CONTEXT_KEY. */
    private static final String ENTRY_CONTEXT_KEY = "entry";
    /** The Constant ENTRY_JSON_CONTEXT_KEY. */
    private static final String ENTRY_JSON_CONTEXT_KEY = "entryJSON";
    /** The odata 2 event handler. */
    private final OData2EventHandler odata2EventHandler;
    /** The result set reader. */
    private final ResultSetReader resultSetReader;
    /** The data source. */
    private DataSource dataSource;

    /**
     * Instantiates a new abstract SQL processor.
     */
    public AbstractSQLProcessor() {
        this(new DummyOData2EventHandler());
    }

    /**
     * Instantiates a new abstract SQL processor.
     *
     * @param odata2EventHandler the odata 2 event handler
     */
    public AbstractSQLProcessor(OData2EventHandler odata2EventHandler) {
        this.resultSetReader = new ResultSetReader(this);
        this.odata2EventHandler = odata2EventHandler;
    }

    /**
     * Update entity simple property value.
     *
     * @param uriInfo the uri info
     * @param content the content
     * @param requestContentType the request content type
     * @param contentType the content type
     * @return the o data response
     * @throws ODataException the o data exception
     */
    @Override
    public ODataResponse updateEntitySimplePropertyValue(PutMergePatchUriInfo uriInfo, InputStream content, String requestContentType,
            String contentType) throws ODataException {
        return super.updateEntitySimplePropertyValue(uriInfo, content, requestContentType, contentType);
    }

    /**
     * Count entity set.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @return the o data response
     * @throws ODataException the o data exception
     */
    @Override
    public ODataResponse countEntitySet(final GetEntitySetCountUriInfo uriInfo, final String contentType) throws ODataException {
        if (uriInfo.getTop() != null || uriInfo.getSkip() != null) {
            throw new ODataNotImplementedException();
        }
        try {
            SQLSelectBuilder sqlQuery = this.getSQLQueryBuilder()
                                            .buildSelectCountQuery((UriInfo) uriInfo, getContext());

            try (Connection connection = getDataSource().getConnection()) {
                int count = doCountEntitySet(sqlQuery, connection);
                return ODataResponse.fromResponse(EntityProvider.writeText(String.valueOf(count)))
                                    .build();
            }
        } catch (SQLException e) {
            throw new ODataException(e);
        }
    }

    /**
     * Do count entity set.
     *
     * @param sqlQuery the sql query
     * @param connection the connection
     * @return the int
     * @throws ODataException the o data exception
     * @throws SQLException the SQL exception
     */
    protected int doCountEntitySet(SQLSelectBuilder sqlQuery, final Connection connection) throws ODataException, SQLException {
        // TODO cache the metadata, because it fires a DB query every time
        // TODO do we really need to select the entities?
        String sql = sqlQuery.buildSelect(createSQLContext(connection));
        logger.info("Counting with sql [{}]", sql);
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            setParamsOnStatement(preparedStatement, sqlQuery.getStatementParams());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                // TODO do we need to assert that resultSet.next() == true and subsequently
                // resultSet.next() == false here?
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    /**
     * Creates the SQL context.
     *
     * @param connection the connection
     * @return the SQL context
     * @throws SQLException the SQL exception
     */
    protected SQLContext createSQLContext(final Connection connection) throws SQLException {
        return new SQLContext(connection, this.getContext());
    }

    /**
     * Sets the params on statement.
     *
     * @param preparedStatement the prepared statement
     * @param params the params
     * @throws SQLException the SQL exception
     */
    public void setParamsOnStatement(PreparedStatement preparedStatement, List<SQLStatementParam> params) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("SQL Params: {}", params);
        }
        SQLUtils.setParamsOnStatement(preparedStatement, params);
    }

    /**
     * Gets the data source.
     *
     * @return the data source
     */
    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Read entity complex property.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @return the o data response
     * @throws ODataException the o data exception
     */
    @Override
    public ODataResponse readEntityComplexProperty(final GetComplexPropertyUriInfo uriInfo, final String contentType)
            throws ODataException {
        throw new ODataException("readEntityComplexProperty not implemented: " + uriInfo.toString());
    }

    /**
     * Read entity.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @return the o data response
     * @throws ODataException the o data exception
     */
    @Override
    public ODataResponse readEntity(final GetEntityUriInfo uriInfo, final String contentType) throws ODataException {

        final EdmEntitySet targetEntitySet = uriInfo.getTargetEntitySet();
        final EdmEntityType targetEntityType = targetEntitySet.getEntityType();

        SQLSelectBuilder query = this.getSQLQueryBuilder()
                                     .buildSelectEntityQuery((UriInfo) uriInfo, getContext());
        ResultSetReader.ExpandAccumulator currentAccumulator = new ResultSetReader.ExpandAccumulator(targetEntityType);

        Collection<EdmProperty> properties = getSelectedProperties(uriInfo.getSelect(), targetEntityType);
        try (Connection connection = getDataSource().getConnection()) {
            try (PreparedStatement statement = createSelectStatement(query, connection)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        boolean hasGeneratedId = query.hasKeyGeneratedPresent(targetEntitySet.getEntityType());
                        ResultSetReader.ResultSetEntity currentTargetEntity =
                                resultSetReader.getResultSetEntity(query, targetEntityType, properties, resultSet, hasGeneratedId);

                        if (!currentAccumulator.isAccumulatorFor(currentTargetEntity)) {
                            currentAccumulator = new ResultSetReader.ExpandAccumulator(currentTargetEntity);
                        }
                        List<ArrayList<NavigationPropertySegment>> expandEntities = uriInfo.getExpand();
                        if (hasExpand(expandEntities)) {
                            resultSetReader.accumulateExpandedEntities(query, resultSet, currentAccumulator, expandEntities);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new ODataException("Unable to read entity", e);
        }
        return ExpandCallBack.writeEntryWithExpand(getContext(), (UriInfo) uriInfo, currentAccumulator, contentType);
    }

    /**
     * Creates the select statement.
     *
     * @param selectQuery the select query
     * @param connection the connection
     * @return the prepared statement
     * @throws SQLException the SQL exception
     * @throws ODataException the o data exception
     */
    protected PreparedStatement createSelectStatement(SQLSelectBuilder selectQuery, final Connection connection)
            throws SQLException, ODataException {
        String sql = selectQuery.buildSelect(createSQLContext(connection));
        logger.info("Created select statement: [{}]", sql);

        PreparedStatement preparedStatement = connection.prepareStatement(sql);

        setParamsOnStatement(preparedStatement, selectQuery.getStatementParams());
        return preparedStatement;
    }

    /**
     * Read entity set.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @return the o data response
     * @throws ODataException the o data exception
     */
    @Override
    public ODataResponse readEntitySet(final GetEntitySetUriInfo uriInfo, final String contentType) throws ODataException {
        final InlineCount inlineCountType = uriInfo.getInlineCount();
        final EdmEntitySet targetEntitySet = uriInfo.getTargetEntitySet();
        final EdmEntityType targetEntityType = targetEntitySet.getEntityType();

        Collection<EdmProperty> properties = getSelectedProperties(uriInfo.getSelect(), targetEntityType);
        List<ResultSetReader.ExpandAccumulator> entitiesFeed = new ArrayList<>();
        Integer count;
        String nextLink;
        try (Connection connection = getDataSource().getConnection()) {
            if (inlineCountType == InlineCount.ALLPAGES) {
                SQLSelectBuilder countEntitySet = this.getSQLQueryBuilder()
                                                      .buildSelectCountQuery((UriInfo) uriInfo, getContext());
                count = doCountEntitySet(countEntitySet, connection); // does not close the connection
            } else {
                count = null;
            }
            List<String> readIdsForExpand = new ArrayList<>();
            if (OData2Utils.hasExpand((UriInfo) uriInfo)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Reading the ids that will be used for $expand");
                }
                readIdsForExpand = readIdsForExpand(uriInfo);
                if (logger.isInfoEnabled()) {
                    logger.info("Using IDs for $expand: {}", readIdsForExpand);
                }
            }

            SQLSelectBuilder query = this.getSQLQueryBuilder()
                                         .buildSelectEntitySetQuery((UriInfo) uriInfo, readIdsForExpand, getContext());
            try (PreparedStatement statement = createSelectStatement(query, connection)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    ResultSetReader.ExpandAccumulator currentAccumulator = new ResultSetReader.ExpandAccumulator(targetEntityType);
                    while (resultSet.next()) {
                        boolean hasGeneratedId = query.hasKeyGeneratedPresent(targetEntitySet.getEntityType());
                        ResultSetReader.ResultSetEntity currentTargetEntity =
                                resultSetReader.getResultSetEntity(query, targetEntityType, properties, resultSet, hasGeneratedId);
                        logger.info("Current entity set object is {}", currentTargetEntity);
                        if (!currentAccumulator.isAccumulatorFor(currentTargetEntity) || currentTargetEntity.keys.isEmpty()) {
                            currentAccumulator = new ResultSetReader.ExpandAccumulator(currentTargetEntity);
                            entitiesFeed.add(currentAccumulator);
                        }

                        List<ArrayList<NavigationPropertySegment>> expandEntities = uriInfo.getExpand();
                        if (hasExpand(expandEntities)) {
                            resultSetReader.accumulateExpandedEntities(query, resultSet, currentAccumulator, expandEntities);
                        }
                    }
                    boolean needsNextLink = query.isServersidePaging() && entitiesFeed.size() == this.getSQLQueryBuilder()
                                                                                                     .getEntityPagingSize(targetEntityType);
                    nextLink = needsNextLink ? generateNextLink(query, targetEntityType) : null;
                }
            }
        } catch (Exception e) {
            throw new ODataException("Unable to read entity set", e);
        }
        return ExpandCallBack.writeFeedWithExpand(getContext(), (UriInfo) uriInfo, entitiesFeed, contentType, count, nextLink);
    }

    /**
     * Read ids for expand.
     *
     * @param uriInfo the uri info
     * @return the list
     * @throws ODataException the o data exception
     */
    public List<String> readIdsForExpand(final GetEntitySetUriInfo uriInfo) throws ODataException {
        List<String> idsOfLeadingEntities = new ArrayList<>();
        try (Connection connection = getDataSource().getConnection()) {
            SQLSelectBuilder queryForIdsInExpand = this.getSQLQueryBuilder()
                                                       .buildSelectEntitySetIdsForTopAndExpandQuery((UriInfo) uriInfo, getContext());
            try (PreparedStatement statement = createSelectStatement(queryForIdsInExpand, connection)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {// TODO remove the duplication here
                        // we select only the ids here (we assume that only one key property is in the
                        // ID)
                        // Override this method if this is not the case
                        idsOfLeadingEntities.add(resultSet.getString(1));
                    }
                }
            }
        } catch (Exception e) {
            throw new ODataException(e);
        }
        return idsOfLeadingEntities;
    }

    /**
     * Generates the next link for server-side paging. The next-link is based on the URI of the current
     * request, except that {@code $skip} or {@code $skiptoken} will be removed.
     *
     * @param query the query
     * @param targetEntityType the target entity type
     * @return the link
     * @throws ODataException in case of an error
     */
    protected String generateNextLink(SQLSelectBuilder query, EdmEntityType targetEntityType) throws ODataException {
        int top = query.getSelectExpression()
                       .getTop();
        int pagingSize = this.getSQLQueryBuilder()
                             .getEntityPagingSize(targetEntityType);
        return OData2Utils.generateNextLink(getContext(), top, pagingSize);
    }

    /**
     * Read entity simple property.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @return the o data response
     * @throws ODataException the o data exception
     */
    @Override
    public ODataResponse readEntitySimpleProperty(final GetSimplePropertyUriInfo uriInfo, final String contentType) throws ODataException {
        return readEntitySimplePropertyValue(uriInfo, contentType);
    }

    /**
     * Read entity simple property value.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @return the o data response
     * @throws ODataException the o data exception
     */
    @Override
    public ODataResponse readEntitySimplePropertyValue(final GetSimplePropertyUriInfo uriInfo, final String contentType)
            throws ODataException {
        final EdmEntitySet targetEntitySet = uriInfo.getTargetEntitySet();
        final EdmEntityType targetEntityType = targetEntitySet.getEntityType();
        SQLSelectBuilder query = this.getSQLQueryBuilder()
                                     .buildSelectEntityQuery((UriInfo) uriInfo, getContext());
        ResultSetReader.ResultSetEntity currentTargetEntity = new ResultSetReader.ResultSetEntity(targetEntityType, Collections.emptyMap());
        Collection<EdmProperty> properties = uriInfo.getPropertyPath();
        try (Connection connection = getDataSource().getConnection()) {
            try (PreparedStatement statement = createSelectStatement(query, connection)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        boolean hasGeneratedId = query.hasKeyGeneratedPresent(targetEntitySet.getEntityType());
                        currentTargetEntity =
                                resultSetReader.getResultSetEntity(query, targetEntityType, properties, resultSet, hasGeneratedId);
                    }
                }
            }
        } catch (Exception e) {
            throw new ODataException("Unable to read entity simple property value", e);
        }
        return writeEntryPropertyValue(uriInfo.getPropertyPath()
                                              .iterator()
                                              .next(),
                currentTargetEntity.data, contentType);
    }

    /**
     * Write entry property value.
     *
     * @param property the property
     * @param data the data
     * @param contentType the content type
     * @return the o data response
     * @throws ODataException the o data exception
     */
    private static ODataResponse writeEntryPropertyValue(EdmProperty property, Map<String, Object> data, final String contentType)
            throws ODataException {
        if (data == null || data.isEmpty()) {
            return OData2Utils.noContentResponse(contentType);
        } else {
            return EntityProvider.writePropertyValue(property, data.get(property.getName()));
        }
    }

    /**
     * Read entity media.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @return the o data response
     * @throws ODataException the o data exception
     */
    @Override
    public ODataResponse readEntityMedia(final GetMediaResourceUriInfo uriInfo, final String contentType) throws ODataException {
        throw new ODataNotImplementedException();
    }

    /**
     * Read entity links.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @return the o data response
     * @throws ODataException the o data exception
     */
    @Override
    public ODataResponse readEntityLinks(final GetEntitySetLinksUriInfo uriInfo, final String contentType) throws ODataException {
        throw new ODataNotImplementedException();
    }

    /**
     * Creates the entity.
     *
     * @param uriInfo the uri info
     * @param content the content
     * @param requestContentType the request content type
     * @param contentType the content type
     * @return the o data response
     * @throws ODataException the o data exception
     */
    @Override
    public ODataResponse createEntity(final PostUriInfo uriInfo, final InputStream content, final String requestContentType,
            final String contentType) throws ODataException {

        if (this.odata2EventHandler.forbidCreateEntity(uriInfo, requestContentType, contentType)) {
            throw new ODataException(
                    String.format("Create operation on entity: %s is forbidden.", OData2Utils.fqn(uriInfo.getTargetType())));
        }

        final EdmEntitySet entitySet = uriInfo.getTargetEntitySet();
        final EdmEntityType entityType = entitySet.getEntityType();
        ODataEntry entry = parseEntry(entitySet, content, requestContentType, false);
        List<EdmProperty> keyProperties = entityType.getKeyProperties();

        Map<Object, Object> handlerContext = new HashMap<>();
        ODataResponse response;
        try (Connection connection = getDataSource().getConnection()) {
            initializeEventHandlerContext(handlerContext, connection, (UriInfo) uriInfo);
            response = this.odata2EventHandler.beforeCreateEntity(uriInfo, requestContentType, contentType, entry, handlerContext);
            if (isErroneousResponse(response)) {
                return response;
            }

            if (this.odata2EventHandler.isUsingOnCreateEntity(uriInfo, requestContentType, contentType)) {
                updateEventHandlerContext(handlerContext, entry);
                response = this.odata2EventHandler.onCreateEntity(uriInfo, content, requestContentType, contentType, handlerContext);
                if (isErroneousResponse(response)) {
                    return response;
                }
            } else {
                checkForKeys(entry, keyProperties);

                SQLInsertBuilder insertBuilder = this.getSQLQueryBuilder()
                                                     .buildInsertEntityQuery((UriInfo) uriInfo, entry, getContext());
                try (PreparedStatement statement = createInsertStatement(insertBuilder, connection)) {
                    statement.executeUpdate();
                }
            }
        } catch (Exception e) {
            throw new ODataException("Unable to create entity. " + ExceptionUtils.getRootCauseMessage(e), e);
        }

        try (Connection connection = getDataSource().getConnection()) {
            if (odata2EventHandler.isUsingAfterCreateEntity(uriInfo, requestContentType, contentType)) {
                if (response != null) {
                    // Read entry from response and set entity again since it is consumed after being parsed
                    try (ByteArrayOutputStream entityOutputStream = new ByteArrayOutputStream()) {
                        ((InputStream) response.getEntity()).transferTo(entityOutputStream);
                        try (InputStream entityInputStream = new ByteArrayInputStream(entityOutputStream.toByteArray());
                                InputStream responseInputStream = new ByteArrayInputStream(entityOutputStream.toByteArray())) {
                            entry = parseEntry(entitySet, entityInputStream, response.getContentHeader(), false);
                            updateUriInfoKeyPredicates((UriInfo) uriInfo, entry, keyProperties);
                            response = ODataResponse.fromResponse(response)
                                                    .entity(responseInputStream)
                                                    .build();
                        }
                    }
                }
                ODataResponse afterCreateEntityResponse =
                        this.odata2EventHandler.afterCreateEntity(uriInfo, requestContentType, contentType, entry, handlerContext);
                if (isErroneousResponse(afterCreateEntityResponse)) {
                    return afterCreateEntityResponse;
                }
            }

            if (response != null) {
                return response;
            } else {
                // Re-read the inserted entity to get the auto-generated properties
                SQLSelectBuilder query = this.getSQLQueryBuilder()
                                             .buildSelectEntityQuery((UriInfo) uriInfo, getContext());
                ResultSetReader.ResultSetEntity currentTargetEntity =
                        new ResultSetReader.ResultSetEntity(entityType, Collections.emptyMap());
                Collection<EdmProperty> properties = getProperties(entityType);
                try (PreparedStatement statement = createSelectStatement(query, connection)) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            boolean hasGeneratedId = query.hasKeyGeneratedPresent(entitySet.getEntityType());
                            currentTargetEntity =
                                    resultSetReader.getResultSetEntity(query, entityType, properties, resultSet, hasGeneratedId);
                        }
                    }

                    return ExpandCallBack.writeEntryWithExpand(getContext(), //
                            (UriInfo) uriInfo, //
                            new ResultSetReader.ExpandAccumulator(currentTargetEntity), //
                            contentType);
                } catch (Throwable t) {
                    if (logger.isErrorEnabled()) {
                        logger.error("Unable to get back the created entity", t);
                    }
                }
            }
        } catch (Exception e) {
            throw new ODataException("Couldn't handle after create event", e);
        }

        ODataContext context = getContext();
        EntityProviderWriteProperties writeProperties = EntityProviderWriteProperties.serviceRoot(context.getPathInfo()
                                                                                                         .getServiceRoot())
                                                                                     .expandSelectTree(entry.getExpandSelectTree())
                                                                                     .build();
        return EntityProvider.writeEntry(contentType, entitySet, entry.getProperties(), writeProperties);
    }

    /**
     * Parses the entry.
     *
     * @param entitySet the entity set
     * @param content the content
     * @param requestContentType the request content type
     * @param merge the merge
     * @return the o data entry
     * @throws ODataBadRequestException the o data bad request exception
     */
    public final ODataEntry parseEntry(final EdmEntitySet entitySet, final InputStream content, final String requestContentType,
            final boolean merge) throws ODataBadRequestException {
        try {
            EntityProviderReadProperties entityProviderProperties = EntityProviderReadProperties.init()
                                                                                                .mergeSemantic(merge)
                                                                                                .build();
            return EntityProvider.readEntry(requestContentType, entitySet, content, entityProviderProperties);
        } catch (Exception e) {
            throw new ODataBadRequestException(ODataBadRequestException.BODY, e);
        }
    }

    /**
     * Creates the insert statement.
     *
     * @param builder the builder
     * @param connection the connection
     * @return the prepared statement
     * @throws SQLException the SQL exception
     * @throws ODataException the o data exception
     */
    protected PreparedStatement createInsertStatement(SQLInsertBuilder builder, final Connection connection)
            throws SQLException, ODataException {
        return createPreparedStatement(connection, builder.build(createSQLContext(connection)));
    }

    /**
     * Creates the prepared statement.
     *
     * @param connection the connection
     * @param statement the statement
     * @return the prepared statement
     * @throws ODataException the o data exception
     * @throws SQLException the SQL exception
     */
    protected PreparedStatement createPreparedStatement(Connection connection, SQLStatement statement) throws ODataException, SQLException {
        String sql = statement.sql();
        logger.info("Creating prepared statement for [{}]", sql);

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        setParamsOnStatement(preparedStatement, statement.getStatementParams());
        return preparedStatement;
    }

    /**
     * Checks if is erroneous response.
     *
     * @param response the response
     * @return true, if is erroneous response
     */
    private boolean isErroneousResponse(ODataResponse response) {
        // erroneous requests are >= 400 (BAD_REQUEST)
        return response != null && response.getStatus()
                                           .getStatusCode() >= HttpStatusCodes.BAD_REQUEST.getStatusCode();
    }

    /**
     * Check for keys.
     *
     * @param entry the entry
     * @param keyProperties the key properties
     * @throws ODataException the o data exception
     */
    private void checkForKeys(ODataEntry entry, List<EdmProperty> keyProperties) throws ODataException {
        if (!keyProperties.isEmpty()) {
            for (EdmProperty keyProperty : keyProperties) {
                if (entry.getProperties()
                         .get(keyProperty.getName()) == null) {
                    throw new ODataException("Missing entity key: " + keyProperty.getName());
                }
            }
        }
    }

    /**
     * Update uri info key predicates.
     *
     * @param uriInfo the uri info
     * @param entry the entry
     * @param keyProperties the key properties
     * @throws EdmException the edm exception
     */
    private void updateUriInfoKeyPredicates(UriInfo uriInfo, ODataEntry entry, List<EdmProperty> keyProperties) throws EdmException {
        List<KeyPredicate> keyPredicates = new ArrayList<>();
        if (!keyProperties.isEmpty()) {
            for (EdmProperty keyProperty : keyProperties) {
                keyPredicates.add(new KeyPredicateImpl(entry.getProperties()
                                                            .get(keyProperty.getName())
                                                            .toString(),
                        keyProperty));
            }
        }

        ((UriInfoImpl) uriInfo).setKeyPredicates(keyPredicates);
    }

    /**
     * Initialize event handler context.
     *
     * @param context the context
     * @param connection the connection
     * @param uriInfo the uri info
     * @throws SQLException the SQL exception
     * @throws EdmException the edm exception
     */
    private void initializeEventHandlerContext(Map<Object, Object> context, Connection connection, UriInfo uriInfo)
            throws SQLException, EdmException {
        context.put(SQL_BUILDER_CONTEXT_KEY, this.getSQLQueryBuilder());
        context.put(SQL_CONTEXT_CONTEXT_KEY, createSQLContext(connection));
        context.put(DATASOURCE_CONTEXT_KEY, getDataSource());
        context.put(ODATA_CONTEXT_CONTEXT_KEY, getContext());
        context.put(MAPPED_KEYS_CONTEXT_KEY, mapKeys(uriInfo.getKeyPredicates()));
    }

    /**
     * Map keys.
     *
     * @param keys the keys
     * @return the map
     * @throws EdmException the edm exception
     */
    public static Map<String, Object> mapKeys(final List<KeyPredicate> keys) throws EdmException {
        Map<String, Object> keyMap = new HashMap<>();
        for (final KeyPredicate key : keys) {
            final EdmProperty property = key.getProperty();
            final EdmSimpleType type = (EdmSimpleType) property.getType();
            keyMap.put(property.getName(),
                    type.valueOfString(key.getLiteral(), EdmLiteralKind.DEFAULT, property.getFacets(), type.getDefaultType()));
        }
        return keyMap;
    }

    /**
     * Update event handler context.
     *
     * @param context the context
     * @param entry the entry
     */
    private void updateEventHandlerContext(Map<Object, Object> context, ODataEntry entry) {
        context.put(ENTRY_CONTEXT_KEY, entry);
    }

    /**
     * Delete entity.
     *
     * @param uriInfo the uri info
     * @param contentType the content type
     * @return the o data response
     * @throws ODataException the o data exception
     */
    @Override
    public ODataResponse deleteEntity(final DeleteUriInfo uriInfo, final String contentType) throws ODataException {
        if (this.odata2EventHandler.forbidDeleteEntity(uriInfo, contentType)) {
            throw new ODataException("Delete operation on entity: " + OData2Utils.fqn(uriInfo.getTargetType()) + " is forbidden.");
        }

        Map<String, Object> keys = mapKeys(uriInfo.getKeyPredicates());
        SQLDeleteBuilder deleteBuilder = this.getSQLQueryBuilder()
                                             .buildDeleteEntityQuery((UriInfo) uriInfo, keys, getContext());
        ODataResponse response;
        try (Connection connection = getDataSource().getConnection()) {
            Map<Object, Object> handlerContext = new HashMap<>();
            final EdmEntitySet entitySet = uriInfo.getTargetEntitySet();
            initializeEventHandlerContext(handlerContext, connection, (UriInfo) uriInfo);
            response = this.odata2EventHandler.beforeDeleteEntity(uriInfo, contentType, handlerContext);
            if (isErroneousResponse(response)) {
                return response;
            }

            if (this.odata2EventHandler.isUsingOnDeleteEntity(uriInfo, contentType)) {
                response = this.odata2EventHandler.onDeleteEntity(uriInfo, contentType, handlerContext);
                if (isErroneousResponse(response)) {
                    return response;
                }
            } else {
                try (PreparedStatement statement = createDeleteStatement(deleteBuilder, connection)) {
                    statement.executeUpdate();
                }
            }

            updateEventHandlerContext(handlerContext, entitySet);
            response = this.odata2EventHandler.afterDeleteEntity(uriInfo, contentType, handlerContext);
            if (isErroneousResponse(response)) {
                return response;
            }
            return ODataResponse.newBuilder()
                                .build();
        } catch (Exception e) {
            throw new ODataException("Unable to delete entry. " + ExceptionUtils.getRootCauseMessage(e), e);
        }
    }

    /**
     * Creates the delete statement.
     *
     * @param builder the builder
     * @param connection the connection
     * @return the prepared statement
     * @throws SQLException the SQL exception
     * @throws ODataException the o data exception
     */
    protected PreparedStatement createDeleteStatement(SQLDeleteBuilder builder, final Connection connection)
            throws SQLException, ODataException {
        return createPreparedStatement(connection, builder.build(createSQLContext(connection)));
    }

    /**
     * Update event handler context.
     *
     * @param context the context
     * @param entitySet the entity set
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ODataBadRequestException the o data bad request exception
     */
    private void updateEventHandlerContext(Map<Object, Object> context, EdmEntitySet entitySet)
            throws IOException, ODataBadRequestException {
        if (context.containsKey(ENTRY_JSON_CONTEXT_KEY)) {
            String beforeUpdateEntryJSON = (String) context.get(ENTRY_JSON_CONTEXT_KEY);
            try (InputStream entryContent = new ByteArrayInputStream(beforeUpdateEntryJSON.getBytes(StandardCharsets.UTF_8))) {
                ODataEntry parsedEntry = parseEntry(entitySet, entryContent, ContentType.APPLICATION_JSON.toContentTypeString(), false);
                context.put(ENTRY_CONTEXT_KEY, parsedEntry);
            }
        }
    }

    /**
     * Update entity.
     *
     * @param uriInfo the uri info
     * @param content the content
     * @param requestContentType the request content type
     * @param merge the merge
     * @param contentType the content type
     * @return the o data response
     * @throws ODataException the o data exception
     */
    @Override
    public ODataResponse updateEntity(final PutMergePatchUriInfo uriInfo, final InputStream content, final String requestContentType,
            final boolean merge, final String contentType) throws ODataException {
        if (uriInfo.getFilter() != null) {
            throw new ODataException("Update operation is not allowed with $filter.");
        }
        if (this.odata2EventHandler.forbidUpdateEntity(uriInfo, requestContentType, merge, contentType)) {
            throw new ODataException(String.format("Update operation forbidden on entity: %s", OData2Utils.fqn(uriInfo.getTargetType())));
        }

        final EdmEntitySet targetEntitySet = uriInfo.getTargetEntitySet();
        final EdmEntityType targetEntityType = targetEntitySet.getEntityType();

        ODataEntry entry = parseEntry(targetEntitySet, content, requestContentType, false);
        SQLSelectBuilder query = this.getSQLQueryBuilder()
                                     .buildSelectEntityQuery((UriInfo) uriInfo, getContext());
        Map<Object, Object> handlerContext = new HashMap<>();
        ODataResponse response;
        try (Connection connection = getDataSource().getConnection()) {
            initializeEventHandlerContext(handlerContext, connection, (UriInfo) uriInfo);
            response = this.odata2EventHandler.beforeUpdateEntity(uriInfo, requestContentType, merge, contentType, entry, handlerContext);
            if (isErroneousResponse(response)) {
                return response;
            }

            if (this.odata2EventHandler.isUsingOnUpdateEntity(uriInfo, requestContentType, merge, contentType)) {
                updateEventHandlerContext(handlerContext, entry);
                response = this.odata2EventHandler.onUpdateEntity(uriInfo, content, requestContentType, merge, contentType, handlerContext);
                if (isErroneousResponse(response)) {
                    return response;
                }
            } else {
                ResultSetReader.ResultSetEntity currentTargetEntity =
                        new ResultSetReader.ResultSetEntity(targetEntityType, Collections.emptyMap());
                Collection<EdmProperty> properties = targetEntityType.getKeyProperties();
                try (PreparedStatement statement = createSelectStatement(query, connection)) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            boolean hasGeneratedId = query.hasKeyGeneratedPresent(targetEntitySet.getEntityType());
                            currentTargetEntity =
                                    resultSetReader.getResultSetEntity(query, targetEntityType, properties, resultSet, hasGeneratedId);
                        }
                        if (currentTargetEntity.data.isEmpty()) {
                            throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
                        }
                    }
                }

                SQLUpdateBuilder updateBuilder = this.getSQLQueryBuilder()
                                                     .buildUpdateEntityQuery((UriInfo) uriInfo, entry, mapKeys(uriInfo.getKeyPredicates()),
                                                             getContext());
                try (PreparedStatement statement = createUpdateStatement(updateBuilder, connection)) {
                    statement.executeUpdate();
                }
            }

            updateEventHandlerContext(handlerContext, targetEntitySet);
            response = this.odata2EventHandler.afterUpdateEntity(uriInfo, requestContentType, merge, contentType, entry, handlerContext);
            if (isErroneousResponse(response)) {
                return response;
            }
            return ODataResponse.newBuilder()
                                .status(HttpStatusCodes.NO_CONTENT)
                                .build();
        } catch (Exception e) {
            throw new ODataException("Unable to update entity. " + ExceptionUtils.getRootCauseMessage(e), e);
        }
    }

    /**
     * Creates the update statement.
     *
     * @param builder the builder
     * @param connection the connection
     * @return the prepared statement
     * @throws SQLException the SQL exception
     * @throws ODataException the o data exception
     */
    protected PreparedStatement createUpdateStatement(SQLUpdateBuilder builder, final Connection connection)
            throws SQLException, ODataException {
        return createPreparedStatement(connection, builder.build(createSQLContext(connection)));
    }

    /**
     * Execute batch.
     *
     * @param handler the handler
     * @param contentType the content type
     * @param content the content
     * @return the o data response
     * @throws ODataException the o data exception
     */
    @Override
    public ODataResponse executeBatch(final BatchHandler handler, final String contentType, final InputStream content)
            throws ODataException {
        try {
            ODataResponse batchResponse;
            PathInfo pathInfo = getContext().getPathInfo();
            EntityProviderBatchProperties batchProperties = EntityProviderBatchProperties.init()
                                                                                         .pathInfo(pathInfo)
                                                                                         .build();
            List<BatchRequestPart> batchParts = EntityProvider.parseBatchRequest(contentType, content, batchProperties);
            List<BatchResponsePart> parts = new ArrayList<>();
            for (BatchRequestPart batchPart : batchParts) {
                parts.add(handler.handleBatchPart(batchPart));
            }
            batchResponse = EntityProvider.writeBatchResponse(parts);
            return batchResponse;
        } catch (Exception e) {
            throw new ODataException("Problem during batch processing", e);
        }
    }

    /**
     * Execute change set.
     *
     * @param handler the handler
     * @param requests the requests
     * @return the batch response part
     * @throws ODataException the o data exception
     */
    @Override
    public BatchResponsePart executeChangeSet(final BatchHandler handler, final List<ODataRequest> requests) throws ODataException {
        DataSource originalDataSource = this.getDataSource();
        try (Connection connection = getDataSource().getConnection()) {
            SingleConnectionDataSource singleConnectionDataSource = new SingleConnectionDataSource(connection);
            this.setDataSource(singleConnectionDataSource);
            boolean originalAutoCommit = connection.getAutoCommit();
            try {
                // override the setDataSource so that all requests in a change set run in the same connection
                return doExecuteChangeSet(handler, requests);
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (Exception e) {
            throw new ODataException("Unable to execute the change set ", e);
        } finally {
            // restore the original data source
            this.setDataSource(originalDataSource);
        }
    }

    /**
     * Sets the data source.
     *
     * @param dataSource the new data source
     */
    void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Do execute change set.
     *
     * @param handler the handler
     * @param requests the requests
     * @return the batch response part
     * @throws ODataException the o data exception
     * @throws SQLException the SQL exception
     */
    public BatchResponsePart doExecuteChangeSet(final BatchHandler handler, final List<ODataRequest> requests)
            throws ODataException, SQLException {
        boolean changeSetFailed = false;
        try (Connection nonClosableConnectionUsedForChangeSet = this.getDataSource()
                                                                    .getConnection()) { //
            try {
                if (nonClosableConnectionUsedForChangeSet.getAutoCommit()) {
                    nonClosableConnectionUsedForChangeSet.setAutoCommit(false);
                }
                List<ODataResponse> responses = new ArrayList<>();
                for (ODataRequest request : requests) {
                    ODataResponse response = handler.handleRequest(request);
                    if (isErroneousResponse(response)) {
                        List<ODataResponse> errorResponses = new ArrayList<>();
                        errorResponses.add(response);
                        changeSetFailed = true;
                        return BatchResponsePart.responses(errorResponses)
                                                .changeSet(false)
                                                .build();
                    }
                    responses.add(response);
                }
                return BatchResponsePart.responses(responses)
                                        .changeSet(true)
                                        .build();
            } catch (ODataException e) {
                changeSetFailed = true;
                throw e;
            } catch (RuntimeException e) {
                changeSetFailed = true;
                throw new ODataException("Unable to process change set", e);
            } finally {
                if (!nonClosableConnectionUsedForChangeSet.getAutoCommit()) {
                    if (changeSetFailed) {
                        nonClosableConnectionUsedForChangeSet.rollback();
                    } else {
                        nonClosableConnectionUsedForChangeSet.commit();
                    }
                } else {
                    throw new IllegalStateException(
                            "Invalid implementation - the OData operations must not unset the auto commit to false on the connection!");
                }
            }
        }
    }
}
