/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.anonymize.service;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.data.anonymize.domain.DataAnonymizeType;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.database.sql.DatabaseType;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import net.datafaker.Faker;

/**
 * The Class DataAnonymizeService.
 */
@Service
public class DataAnonymizeService {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(DataAnonymizeService.class);

    /**
     * The data sources manager.
     */
    private final DataSourcesManager datasourceManager;

    /** The batch size. */
    private static int BATCH_SIZE = 1000;

    /** The Constant DIRIGIBLE_DATABASE_TRANSFER_BATCH_SIZE. */
    private static final String DIRIGIBLE_DATABASE_TRANSFER_BATCH_SIZE = "DIRIGIBLE_DATABASE_TRANSFER_BATCH_SIZE";

    /** The Constant DEFAULT_BATCH_SIZE. */
    private static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * Instantiates a new data source endpoint.
     *
     * @param datasourceManager the datasource manager
     */
    @Autowired
    public DataAnonymizeService(DataSourcesManager datasourceManager) {
        this.datasourceManager = datasourceManager;
    }

    /**
     * Export structure.
     *
     * @param datasource the datasource
     * @param schema the schema
     * @param table the table
     * @param column the column
     * @param primaryKey the primary key
     * @param type the type
     */
    public void anonymizeColumn(String datasource, String schema, String table, String column, String primaryKey, String type) {
        javax.sql.DataSource dataSource = datasourceManager.getDataSource(datasource);
        if (dataSource != null) {

            Faker faker = new Faker();
            try {
                BATCH_SIZE = Configuration.getAsInt(DIRIGIBLE_DATABASE_TRANSFER_BATCH_SIZE, DEFAULT_BATCH_SIZE);
            } catch (NumberFormatException e1) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Wrong configuration for " + DIRIGIBLE_DATABASE_TRANSFER_BATCH_SIZE);
                }
            }

            DataAnonymizeType typeValue = DataAnonymizeType.valueOf(type);

            try (Connection connection = dataSource.getConnection()) {
                if (SqlFactory.deriveDialect(connection)
                              .getDatabaseType(connection)
                              .equals(DatabaseType.NOSQL.getName())) {
                    anonymizeNoSQLColumn(schema, table, column, primaryKey, dataSource, faker, typeValue, connection);
                } else {
                    anonymizeRDBMSColumn(schema, table, column, primaryKey, dataSource, faker, typeValue, connection);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Anonymize RDBMS column.
     *
     * @param schema the schema
     * @param table the table
     * @param column the column
     * @param primaryKey the primary key
     * @param dataSource the data source
     * @param faker the faker
     * @param typeValue the type value
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    public void anonymizeRDBMSColumn(String schema, String table, String column, String primaryKey, javax.sql.DataSource dataSource,
            Faker faker, DataAnonymizeType typeValue, Connection connection) throws SQLException {

        String tableName = "\"" + schema + "\".\"" + table + "\"";
        String columnName = "\"" + column + "\"";
        String primaryKeyName = "\"" + primaryKey + "\"";
        String select = "SELECT " + primaryKeyName + ", " + columnName + " FROM " + tableName;
        String update = "UPDATE " + tableName + " SET " + columnName + " = ? WHERE " + primaryKeyName + " = ? ";

        try (Statement statement = connection.createStatement()) {
            int updatedRecords = 0;
            try (PreparedStatement preparedStatement = connection.prepareStatement(update)) {
                ResultSet rs = statement.executeQuery(select);
                int size = rs.getMetaData()
                             .getColumnDisplaySize(2);
                while (rs.next()) {
                    String value = rs.getString(2);
                    int length = value != null ? value.length() : 0;
                    switch (typeValue) {
                        case FULL_NAME: {
                            preparedStatement.setString(1, truncate(faker.name()
                                                                         .fullName(),
                                    size));
                            break;
                        }
                        case FIRST_NAME: {
                            preparedStatement.setString(1, truncate(faker.name()
                                                                         .firstName(),
                                    size));
                            break;
                        }
                        case LAST_NAME: {
                            preparedStatement.setString(1, truncate(faker.name()
                                                                         .lastName(),
                                    size));
                            break;
                        }
                        case USER_NAME: {
                            preparedStatement.setString(1, truncate(faker.internet()
                                                                         .username(),
                                    size));
                            break;
                        }
                        case EMAIL: {
                            preparedStatement.setString(1, truncate((faker.internet()
                                                                          .username()
                                    + "@acme.com"), size));
                            break;
                        }
                        case PHONE: {
                            if (value != null) {
                                preparedStatement.setString(1, faker.examplify(value));
                            } else {
                                preparedStatement.setNull(1, Types.VARCHAR);
                            }
                            break;
                        }
                        case ADDRESS: {
                            preparedStatement.setString(1, truncate(faker.address()
                                                                         .streetAddress(),
                                    size));
                            break;
                        }
                        case CITY: {
                            preparedStatement.setString(1, truncate(faker.address()
                                                                         .city(),
                                    size));
                            break;
                        }
                        case COUNTRY: {
                            preparedStatement.setString(1, truncate(faker.address()
                                                                         .country(),
                                    size));
                            break;
                        }
                        case DATE: {
                            Date date = rs.getDate(2);
                            if (date != null) {
                                java.util.Date past = faker.date()
                                                           .past(10, TimeUnit.DAYS, new java.util.Date(date.getTime()));
                                preparedStatement.setDate(1, new Date(past.getTime()));
                            } else {
                                preparedStatement.setNull(1, Types.DATE);
                            }
                            break;
                        }
                        case RANDOM: {
                            if (value != null) {
                                preparedStatement.setString(1, faker.examplify(value));
                            } else {
                                preparedStatement.setNull(1, Types.VARCHAR);
                            }
                            break;
                        }
                        case MASK: {
                            preparedStatement.setString(1, "*".repeat(length));
                            break;
                        }
                        case EMPTY: {
                            preparedStatement.setString(1, "");
                            break;
                        }
                        case NULL: {
                            preparedStatement.setNull(1, Types.VARCHAR);
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("Unexpected value: " + typeValue);
                    }
                    updatedRecords++;

                    Object key = rs.getObject(1);
                    if (key != null) {
                        preparedStatement.setObject(2, key);
                        preparedStatement.addBatch();
                        if (updatedRecords % BATCH_SIZE == 0) {
                            preparedStatement.executeBatch();
                        }
                    } else {
                        logger.error("Primary key cannot be null for the record: " + updatedRecords);
                    }
                }
                if (updatedRecords % BATCH_SIZE != 0) {
                    preparedStatement.executeBatch();
                }
            }
        }

    }

    /**
     * Anonymize no SQL column.
     *
     * @param db the db
     * @param collection the collection
     * @param column the column
     * @param key the key
     * @param dataSource the data source
     * @param faker the faker
     * @param typeValue the type value
     * @param connection the connection
     * @throws SQLException the SQL exception
     */
    public void anonymizeNoSQLColumn(String db, String collection, String column, String key, javax.sql.DataSource dataSource, Faker faker,
            DataAnonymizeType typeValue, Connection connection) throws SQLException {

        String find = "{find:\"" + collection + "\"}";

        try (Statement statement = connection.createStatement()) {
            int updatedRecords = 0;
            statement.setCursorName(collection);
            boolean nested = false;
            String name = null;
            if (column.indexOf('.') > 0) {
                name = column.substring(column.lastIndexOf(".") + 1);
                nested = true;
            } else {
                name = column;
            }
            ResultSet rs = statement.executeQuery(find);
            while (rs.next()) {
                String json = rs.getString(-100);
                JsonObject document = JsonHelper.parseJson(json)
                                                .getAsJsonObject();
                JsonObject object;
                if (nested) {
                    object = extractObject(document, column);
                } else {
                    object = document;
                }
                if (object == null) {
                    continue;
                }
                JsonElement jsonElement = object.get(name);
                if (jsonElement == null || jsonElement instanceof JsonNull) {
                    continue;
                }
                if (jsonElement instanceof JsonArray) {
                    if (DataAnonymizeType.NULL.equals(typeValue)) {
                        object.remove(name);
                    } else if (DataAnonymizeType.EMPTY.equals(typeValue)) {
                        object.remove(name);
                        object.add(name, new JsonArray());
                    }
                } else {
                    String value = jsonElement.getAsString();
                    if (null == value) {
                        continue;
                    }

                    switch (typeValue) {
                        case FULL_NAME: {
                            object.remove(name);
                            object.addProperty(name, faker.name()
                                                          .fullName());
                            break;
                        }
                        case FIRST_NAME: {
                            object.remove(name);
                            object.addProperty(name, faker.name()
                                                          .firstName());
                            break;
                        }
                        case LAST_NAME: {
                            object.remove(name);
                            object.addProperty(name, faker.name()
                                                          .lastName());
                            break;
                        }
                        case USER_NAME: {
                            object.remove(name);
                            object.addProperty(name, faker.internet()
                                                          .username());
                            break;
                        }
                        case EMAIL: {
                            object.remove(name);
                            object.addProperty(name, faker.internet()
                                                          .username()
                                    + "@acme.com");
                            break;
                        }
                        case PHONE: {
                            object.remove(name);
                            object.addProperty(name, faker.examplify(value));
                            break;
                        }
                        case ADDRESS: {
                            object.remove(name);
                            object.addProperty(name, faker.address()
                                                          .streetAddress());
                            break;
                        }
                        case CITY: {
                            object.remove(name);
                            object.addProperty(name, faker.address()
                                                          .city());
                            break;
                        }
                        case COUNTRY: {
                            object.remove(name);
                            object.addProperty(name, faker.address()
                                                          .country());
                            break;
                        }
                        case DATE: {
                            Date date = rs.getDate(2);
                            if (date != null) {
                                java.util.Date past = faker.date()
                                                           .past(10, TimeUnit.DAYS, new java.util.Date(date.getTime()));
                                object.remove(name);
                                object.addProperty(name, past.getTime());
                            }
                            break;
                        }
                        case RANDOM: {
                            object.remove(name);
                            object.addProperty(name, faker.examplify(value));
                            break;
                        }
                        case MASK: {
                            object.remove(name);
                            int length = value.length();
                            object.addProperty(name, "*".repeat(length));
                            break;
                        }
                        case EMPTY: {
                            object.remove(name);
                            object.addProperty(name, "");
                            break;
                        }
                        case NULL: {
                            object.remove(name);
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("Unexpected value: " + typeValue);
                    }
                }
                updatedRecords++;

                statement.addBatch("UPDATE" + document.toString());

                if (updatedRecords % BATCH_SIZE == 0) {
                    statement.executeBatch();
                }
            }
            if (updatedRecords % BATCH_SIZE != 0) {
                statement.executeBatch();
            }
        }

    }

    /**
     * Extract object.
     *
     * @param document the document
     * @param column the column
     * @return the json object
     */
    private JsonObject extractObject(JsonObject document, String column) {
        JsonObject object = document;
        StringTokenizer tokens = new StringTokenizer(column, ".");
        while (tokens.hasMoreTokens()) {
            String name = tokens.nextToken();
            if (object.get(name) == null) {
                return null;
            }
            if (!object.get(name)
                       .isJsonObject()) {
                break;
            }
            object = object.get(name)
                           .getAsJsonObject();
        }
        return object;
    }

    /**
     * Truncate.
     *
     * @param value the value
     * @param size the size
     * @return the string
     */
    String truncate(String value, int size) {
        if (value != null) {
            if (value.length() > size) {
                return value.substring(0, size);
            }
        }
        return value;
    }

}
