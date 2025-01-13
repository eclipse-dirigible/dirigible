/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.mongodb.jdbc;

import java.net.URI;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * The Class MongoDBConnection.
 */
public class MongoDBConnection implements Connection {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(MongoDBConnection.class);

    /** The Constant MONGODB_DEFAULT_DB. */
    private static final String MONGODB_DEFAULT_DB = "db";

    /** The info. */
    private Properties info;

    /** The db name. */
    private String dbName;

    /** The collection name. */
    private final String collectionName;

    /** The is closed. */
    private boolean isClosed = true;

    /** The is readonly. */
    private boolean isReadonly = false;

    /** The client URI. */
    private MongoClientURI clientUri;

    /** The client. */
    private final MongoClient client;

    /** The client options. */
    private final MongoClientOptions clientOptions;

    /** The mongo atabase. */
    MongoDatabase mongoDatabase;

    /** The collection. */
    MongoCollection<Document> collection;

    /** The metadata. */
    private MongoDBDatabaseMetadata metadata;

    /**
     * Instantiates a new mongo DB connection.
     *
     * @param url the url
     * @param info the info
     */
    public MongoDBConnection(String url, Properties info) {
        String dbUrl = url.replace("jdbc:", "");
        String username = info.getProperty("user");
        String password = info.getProperty("password");
        this.client = createMongoClient(dbUrl, username, password);
        this.clientUri = new MongoClientURI(dbUrl);
        this.dbName = this.clientUri.getDatabase() != null ? this.clientUri.getDatabase() : MONGODB_DEFAULT_DB;
        this.collectionName = this.clientUri.getCollection();

        this.isClosed = false;

        this.info = info;
        if (this.info == null) {
            this.info = new Properties();
        }
        this.clientOptions = this.clientUri.getOptions();
        this.info.putAll(this.mongoClientOptionsAsProperties(this.clientOptions, this.info));

        // retrieve these from connected client
        this.dbName = this.clientUri.getDatabase();
        if (this.dbName != null) {
            this.mongoDatabase = this.client.getDatabase(this.dbName);
        }
        if (this.collectionName != null) {
            this.collection = this.mongoDatabase.getCollection(this.collectionName);
        }

        logger.debug("Connected with client properties: " + this.info.toString());
    }

    /**
     * Creates the mongo client.
     *
     * @param url the url
     * @param user the user
     * @param pass the pass
     * @return the mongo client
     */
    public static MongoClient createMongoClient(String url, String user, String pass) {
        URI dbUri = URI.create(url);
        String username;
        String password;
        if (dbUri.getUserInfo() != null && !dbUri.getUserInfo()
                                                 .isEmpty()) {
            username = dbUri.getUserInfo()
                            .substring(0, dbUri.getUserInfo()
                                               .indexOf(':'));
            password = dbUri.getUserInfo()
                            .substring(dbUri.getUserInfo()
                                            .indexOf(':')
                                    + 1);
        } else {
            username = user;
            password = pass;
        }
        MongoClient client;
        if (username != null && password != null) {
            MongoCredential credential = MongoCredential.createCredential(username, "admin", password.toCharArray());
            client = MongoClients.create(MongoClientSettings.builder()
                                                            .applyConnectionString(new ConnectionString(url))
                                                            .credential(credential)
                                                            .build());
        } else {
            client = MongoClients.create(new ConnectionString(url));
        }
        return client;
    }

    /**
     * Gets the mongo db.
     *
     * @return the mongo db
     */
    public MongoDatabase getMongoDatabase() {
        return this.mongoDatabase;
    }

    /**
     * Gets the collection name.
     *
     * @return the collection name
     */
    public String getCollectionName() {
        return this.collectionName;
    }

    /**
     * Mongo client options as properties.
     *
     * @param ops the ops
     * @param props the props
     * @return the properties
     */
    private Properties mongoClientOptionsAsProperties(MongoClientOptions ops, Properties props) {
        // TODO: write complex object properties too?
        if (ops.getDescription() != null)
            props.setProperty("description", ops.getDescription());
        if (ops.getRequiredReplicaSetName() != null)
            props.setProperty("requiredReplicaSetName", ops.getRequiredReplicaSetName());
        props.setProperty("connectionsPerHost", "" + ops.getConnectionsPerHost());
        props.setProperty("connectTimeout", "" + ops.getConnectTimeout());
        props.setProperty("heartbeatConnectTimeout", "" + ops.getHeartbeatConnectTimeout());
        props.setProperty("heartbeatFrequency", "" + ops.getHeartbeatFrequency());
        props.setProperty("heartbeatSocketTimeout", "" + ops.getHeartbeatSocketTimeout());
        props.setProperty("localThreshold", "" + ops.getLocalThreshold());
        props.setProperty("maxConnectionIdleTime", "" + ops.getMaxConnectionIdleTime());
        props.setProperty("maxConnectionLifeTime", "" + ops.getMaxConnectionLifeTime());
        props.setProperty("maxWaitTime", "" + ops.getMaxWaitTime());
        props.setProperty("minConnectionsPerHost", "" + ops.getMinConnectionsPerHost());
        props.setProperty("minHeartbeatFrequency", "" + ops.getMinHeartbeatFrequency());
        props.setProperty("serverSelectionTimeout", "" + ops.getServerSelectionTimeout());
        props.setProperty("socketTimeout", "" + ops.getSocketTimeout());
        return props;
    }

    /**
     * Unwrap.
     *
     * @param <T> the generic type
     * @param iface the iface
     * @return the t
     * @throws SQLException the SQL exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface)) {
            return (T) this;
        }
        throw new SQLException("No wrapper for " + iface);
    }

    /**
     * Checks if is wrapper for.
     *
     * @param iface the iface
     * @return true, if is wrapper for
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface != null && iface.isAssignableFrom(getClass());
    }

    /**
     * Creates the statement.
     *
     * @return the statement
     * @throws SQLException the SQL exception
     */
    @Override
    public Statement createStatement() throws SQLException {
        return new MongoDBStatement(this);
    }

    /**
     * Creates the statement.
     *
     * @param resultSetType the result set type
     * @param resultSetConcurrency the result set concurrency
     * @return the statement
     * @throws SQLException the SQL exception
     */
    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Creates the statement.
     *
     * @param resultSetType the result set type
     * @param resultSetConcurrency the result set concurrency
     * @param resultSetHoldability the result set holdability
     * @return the statement
     * @throws SQLException the SQL exception
     */
    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Prepare statement.
     *
     * @param sql the sql
     * @return the prepared statement
     * @throws SQLException the SQL exception
     */
    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return new MongoDBPreparedStatement(this, sql);
    }

    /**
     * Prepare statement.
     *
     * @param sql the sql
     * @param resultSetType the result set type
     * @param resultSetConcurrency the result set concurrency
     * @return the prepared statement
     * @throws SQLException the SQL exception
     */
    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Prepare statement.
     *
     * @param sql the sql
     * @param resultSetType the result set type
     * @param resultSetConcurrency the result set concurrency
     * @param resultSetHoldability the result set holdability
     * @return the prepared statement
     * @throws SQLException the SQL exception
     */
    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Prepare statement.
     *
     * @param sql the sql
     * @param autoGeneratedKeys the auto generated keys
     * @return the prepared statement
     * @throws SQLException the SQL exception
     */
    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Prepare statement.
     *
     * @param sql the sql
     * @param columnIndexes the column indexes
     * @return the prepared statement
     * @throws SQLException the SQL exception
     */
    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Prepare statement.
     *
     * @param sql the sql
     * @param columnNames the column names
     * @return the prepared statement
     * @throws SQLException the SQL exception
     */
    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Prepare call.
     *
     * @param sql the sql
     * @return the callable statement
     * @throws SQLException the SQL exception
     */
    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Prepare call.
     *
     * @param sql the sql
     * @param resultSetType the result set type
     * @param resultSetConcurrency the result set concurrency
     * @param resultSetHoldability the result set holdability
     * @return the callable statement
     * @throws SQLException the SQL exception
     */
    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Prepare call.
     *
     * @param sql the sql
     * @param resultSetType the result set type
     * @param resultSetConcurrency the result set concurrency
     * @return the callable statement
     * @throws SQLException the SQL exception
     */
    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Native SQL.
     *
     * @param sql the sql
     * @return the string
     * @throws SQLException the SQL exception
     */
    @Override
    public String nativeSQL(String sql) throws SQLException {
        // TODO: currently works only ofr queries
        BsonDocument filterDocument = null;
        if (sql == null || sql.length() < 1)// that is a call to find() in terms of mongodb queries
            filterDocument = new BsonDocument();
        else
            filterDocument = BsonDocument.parse(sql);
        return filterDocument.toJson();
    }

    /**
     * Sets the auto commit.
     *
     * @param autoCommit the new auto commit
     * @throws SQLException the SQL exception
     */
    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        // silently ignore
    }

    /**
     * Gets the auto commit.
     *
     * @return the auto commit
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean getAutoCommit() throws SQLException {
        return false;
    }

    /**
     * Commit.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void commit() throws SQLException {
        // silently ignore
    }

    /**
     * Rollback.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void rollback() throws SQLException {
        // silently ignore
    }

    /**
     * Close.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void close() throws SQLException {
        this.client.close();
        this.isClosed = true;
    }

    /**
     * Checks if is closed.
     *
     * @return true, if is closed
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isClosed() throws SQLException {
        return this.isClosed;
    }

    /**
     * Gets the meta data.
     *
     * @return the meta data
     * @throws SQLException the SQL exception
     */
    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        if (metadata == null) {
            metadata = new MongoDBDatabaseMetadata(this);
            Document response = mongoDatabase.runCommand(BsonDocument.parse("{ buildInfo: 1 }"));
            metadata.setDatabaseProductName("MongoDB");
            metadata.setDatabaseProductVersion(response.getString("version"));
            metadata.setDriverName("MongoDB JDBC Driver");
            metadata.setURL(this.clientUri.getURI());
        }
        // metadata.setIsReadOnly(client.isLocked());
        return metadata;
    }

    /**
     * Sets the read only.
     *
     * @param readOnly the new read only
     * @throws SQLException the SQL exception
     */
    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        this.isReadonly = readOnly;
    }

    /**
     * Checks if is read only.
     *
     * @return true, if is read only
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isReadOnly() throws SQLException {
        return this.isReadonly;
    }

    /**
     * Sets the catalog.
     *
     * @param catalog the new catalog
     * @throws SQLException the SQL exception
     */
    @Override
    public void setCatalog(String catalog) throws SQLException {
        // silently ignore
    }

    /**
     * Gets the catalog.
     *
     * @return the catalog
     * @throws SQLException the SQL exception
     */
    @Override
    public String getCatalog() throws SQLException {
        return null;
    }

    /**
     * Sets the transaction isolation.
     *
     * @param level the new transaction isolation
     * @throws SQLException the SQL exception
     */
    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        // silently ignore
    }

    /**
     * Gets the transaction isolation.
     *
     * @return the transaction isolation
     * @throws SQLException the SQL exception
     */
    @Override
    public int getTransactionIsolation() throws SQLException {
        return Connection.TRANSACTION_NONE;
    }

    /**
     * Gets the warnings.
     *
     * @return the warnings
     * @throws SQLException the SQL exception
     */
    @Override
    public SQLWarning getWarnings() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Clear warnings.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void clearWarnings() throws SQLException {
        // silently ignore
    }

    /**
     * Gets the type map.
     *
     * @return the type map
     * @throws SQLException the SQL exception
     */
    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Sets the type map.
     *
     * @param map the map
     * @throws SQLException the SQL exception
     */
    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the holdability.
     *
     * @param holdability the new holdability
     * @throws SQLException the SQL exception
     */
    @Override
    public void setHoldability(int holdability) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Gets the holdability.
     *
     * @return the holdability
     * @throws SQLException the SQL exception
     */
    @Override
    public int getHoldability() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Sets the savepoint.
     *
     * @return the savepoint
     * @throws SQLException the SQL exception
     */
    @Override
    public Savepoint setSavepoint() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Sets the savepoint.
     *
     * @param name the name
     * @return the savepoint
     * @throws SQLException the SQL exception
     */
    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Rollback.
     *
     * @param savepoint the savepoint
     * @throws SQLException the SQL exception
     */
    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        // silenty ignore
    }

    /**
     * Release savepoint.
     *
     * @param savepoint the savepoint
     * @throws SQLException the SQL exception
     */
    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Creates the clob.
     *
     * @return the clob
     * @throws SQLException the SQL exception
     */
    @Override
    public Clob createClob() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Creates the blob.
     *
     * @return the blob
     * @throws SQLException the SQL exception
     */
    @Override
    public Blob createBlob() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Creates the N clob.
     *
     * @return the n clob
     * @throws SQLException the SQL exception
     */
    @Override
    public NClob createNClob() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Creates the SQLXML.
     *
     * @return the sqlxml
     * @throws SQLException the SQL exception
     */
    @Override
    public SQLXML createSQLXML() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Checks if is valid.
     *
     * @param timeout the timeout
     * @return true, if is valid
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isValid(int timeout) throws SQLException {
        Document response = this.mongoDatabase.runCommand(new BsonDocument("ping", new BsonInt32(1)));
        return response.getDouble("ok") == 1.0;
    }

    /**
     * Sets the client info.
     *
     * @param name the name
     * @param value the value
     * @throws SQLClientInfoException the SQL client info exception
     */
    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        // silently ingore
    }

    /**
     * Sets the client info.
     *
     * @param properties the new client info
     * @throws SQLClientInfoException the SQL client info exception
     */
    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        // silently ingore
    }

    /**
     * Gets the client info.
     *
     * @param name the name
     * @return the client info
     * @throws SQLException the SQL exception
     */
    @Override
    public String getClientInfo(String name) throws SQLException {
        return this.info.getProperty(name);
    }

    /**
     * Gets the client info.
     *
     * @return the client info
     * @throws SQLException the SQL exception
     */
    @Override
    public Properties getClientInfo() throws SQLException {
        return this.info;
    }

    /**
     * Creates the array of.
     *
     * @param typeName the type name
     * @param elements the elements
     * @return the array
     * @throws SQLException the SQL exception
     */
    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Creates the struct.
     *
     * @param typeName the type name
     * @param attributes the attributes
     * @return the struct
     * @throws SQLException the SQL exception
     */
    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Sets the schema.
     *
     * @param schema the new schema
     * @throws SQLException the SQL exception
     */
    @Override
    public void setSchema(String schema) throws SQLException {
        if (schema == null || schema.length() < 1)
            throw new IllegalArgumentException();
        this.collection = this.mongoDatabase.getCollection(schema);
    }

    /**
     * Gets the schema.
     *
     * @return the schema
     * @throws SQLException the SQL exception
     */
    @Override
    public String getSchema() throws SQLException {
        return this.collectionName;
    }

    /**
     * Abort.
     *
     * @param executor the executor
     * @throws SQLException the SQL exception
     */
    @Override
    public void abort(Executor executor) throws SQLException {
        executor.execute(new AsyncAbort(this));
    }

    /**
     * The Class AsyncAbort.
     */
    private class AsyncAbort implements Runnable {

        /** The mongodb jdbc connection. */
        MongoDBConnection mongodbJdbcConnection;

        /**
         * Instantiates a new async abort.
         *
         * @param mongodbJdbcConnection the mongodb jdbc connection
         */
        public AsyncAbort(MongoDBConnection mongodbJdbcConnection) {
            this.mongodbJdbcConnection = mongodbJdbcConnection;
        }

        /**
         * Run.
         */
        @Override
        public void run() {
            try {
                if (!this.mongodbJdbcConnection.isClosed()) {
                    this.mongodbJdbcConnection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Sets the network timeout.
     *
     * @param executor the executor
     * @param milliseconds the milliseconds
     * @throws SQLException the SQL exception
     */
    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Gets the network timeout.
     *
     * @return the network timeout
     * @throws SQLException the SQL exception
     */
    @Override
    public int getNetworkTimeout() throws SQLException {
        return this.clientOptions.getConnectTimeout();
    }

    /**
     * Gets the client.
     *
     * @return the client
     */
    public MongoClient getClient() {
        return client;
    }

}
