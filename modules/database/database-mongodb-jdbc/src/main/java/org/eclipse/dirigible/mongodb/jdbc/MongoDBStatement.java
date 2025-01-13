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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.BsonDocument;
import org.bson.Document;
import org.eclipse.dirigible.mongodb.jdbc.util.SingleColumnStaticResultSet;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;

/**
 * The Class MongoDBStatement.
 */
public class MongoDBStatement implements Statement {

    /** The conn. */
    protected MongoDBConnection conn;

    /** The is closed. */
    protected boolean isClosed = false;

    /** The batch list. */
    protected List<WriteModel<Document>> batchList = new ArrayList<WriteModel<Document>>();

    /** The current collection. */
    protected String currentCollection;



    /**
     * Instantiates a new mongo DB statement.
     *
     * @param conn the conn
     */
    public MongoDBStatement(MongoDBConnection conn) {
        this.conn = conn;
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
     * Input string: the document specification as defined in
     * https://docs.mongodb.org/manual/reference/command/find/#dbcmd.find
     *
     * @param sql the sql
     * @return the result set
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        MongoDatabase db = this.conn.getMongoDatabase();
        BsonDocument filterDocument = null;
        if (sql == null || sql.length() < 1) {
            filterDocument = new BsonDocument();
        } else {
            filterDocument = BsonDocument.parse(sql);
        }

        if (filterDocument.containsKey("find")) {
            String collectionName = filterDocument.getString("find")
                                                  .getValue();
            if (collectionName == null) {
                collectionName = this.conn.getCollectionName();// fallback if any
                currentCollection = collectionName;
            }
            if (collectionName == null) {
                throw new IllegalArgumentException("Specifying a collection is mandatory for query operations");
            }

            BsonDocument filter = filterDocument.containsKey("filter") ? filterDocument.getDocument("filter") : null;
            FindIterable<Document> searchHits = null;
            if (filter == null) {
                searchHits = db.getCollection(collectionName)
                               .find();
            } else {
                searchHits = db.getCollection(collectionName)
                               .find(filter);
            }
            if (filterDocument.containsKey("batchSize"))
                searchHits.batchSize(filterDocument.getInt32("batchSize")
                                                   .getValue());
            if (filterDocument.containsKey("limit"))
                searchHits.limit(filterDocument.getInt32("limit")
                                               .getValue());
            if (filterDocument.containsKey("sort"))
                searchHits.sort(filterDocument.getDocument("sort"));
            return new MongoDBResultSet(this, searchHits);
        } else if (filterDocument.containsKey("count")) {
            String collectionName = filterDocument.getString("count")
                                                  .getValue();
            if (collectionName == null) {
                collectionName = this.conn.getCollectionName();// fallback if any
            }
            if (collectionName == null) {
                throw new IllegalArgumentException("Specifying a collection is mandatory for query operations");
            }

            BsonDocument filter = filterDocument.containsKey("filter") ? filterDocument.getDocument("filter") : null;
            long count = -1;
            if (filter == null) {
                count = db.getCollection(collectionName)
                          .countDocuments();
            } else {
                count = db.getCollection(collectionName)
                          .countDocuments(filter);
            }
            ResultSet result = new SingleColumnStaticResultSet(Arrays.asList(new String[] {count + ""})
                                                                     .iterator());
            return result;
        }

        throw new IllegalArgumentException("Specifying a collection is mandatory for query operations");
    }

    /**
     * https://docs.mongodb.org/manual/reference/command/update/#dbcmd.update
     *
     * @param sql the sql
     * @return the int
     * @throws SQLException the SQL exception
     */
    @Override
    public int executeUpdate(String sql) throws SQLException {
        BsonDocument updateDocument = null;
        if (sql == null || sql.length() < 1)
            throw new IllegalArgumentException();
        else
            updateDocument = BsonDocument.parse(sql);

        Document response = this.conn.getMongoDatabase()
                                     .runCommand(updateDocument);
        int updatedDocuments = 0;
        if (response != null && response.get("ok") != null) {
            try {
                updatedDocuments = response.getInteger("nModified");
            } catch (NullPointerException e) {
                // e.g. create request
            }
            // TODO operation atomicity concerns? /errors/
        }
        return updatedDocuments;
    }

    /**
     * Close.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void close() throws SQLException {
        this.isClosed = true;
        this.conn.close();
    }

    /**
     * Gets the max field size.
     *
     * @return the max field size
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxFieldSize() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Sets the max field size.
     *
     * @param max the new max field size
     * @throws SQLException the SQL exception
     */
    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        // TODO Auto-generated method stub
    }

    /**
     * Gets the max rows.
     *
     * @return the max rows
     * @throws SQLException the SQL exception
     */
    @Override
    public int getMaxRows() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Sets the max rows.
     *
     * @param max the new max rows
     * @throws SQLException the SQL exception
     */
    @Override
    public void setMaxRows(int max) throws SQLException {
        // TODO Auto-generated method stub
    }

    /**
     * Sets the escape processing.
     *
     * @param enable the new escape processing
     * @throws SQLException the SQL exception
     */
    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        // TODO Auto-generated method stub
    }

    /**
     * Gets the query timeout.
     *
     * @return the query timeout
     * @throws SQLException the SQL exception
     */
    @Override
    public int getQueryTimeout() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Sets the query timeout.
     *
     * @param seconds the new query timeout
     * @throws SQLException the SQL exception
     */
    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        // TODO Auto-generated method stub
    }

    /**
     * Cancel.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void cancel() throws SQLException {
        // TODO Auto-generated method stub
    }

    /**
     * Gets the warnings.
     *
     * @return the warnings
     * @throws SQLException the SQL exception
     */
    @Override
    public SQLWarning getWarnings() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Clear warnings.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void clearWarnings() throws SQLException {
        // TODO Auto-generated method stub
    }

    /**
     * Sets the cursor name.
     *
     * @param name the new cursor name
     * @throws SQLException the SQL exception
     */
    @Override
    public void setCursorName(String name) throws SQLException {
        this.currentCollection = name;
    }

    /**
     * Execute.
     *
     * @param sql the sql
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean execute(String sql) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Gets the result set.
     *
     * @return the result set
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getResultSet() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Gets the update count.
     *
     * @return the update count
     * @throws SQLException the SQL exception
     */
    @Override
    public int getUpdateCount() throws SQLException {
        return -1;
    }

    /**
     * Gets the more results.
     *
     * @return the more results
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean getMoreResults() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Sets the fetch direction.
     *
     * @param direction the new fetch direction
     * @throws SQLException the SQL exception
     */
    @Override
    public void setFetchDirection(int direction) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Gets the fetch direction.
     *
     * @return the fetch direction
     * @throws SQLException the SQL exception
     */
    @Override
    public int getFetchDirection() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Sets the fetch size.
     *
     * @param rows the new fetch size
     * @throws SQLException the SQL exception
     */
    @Override
    public void setFetchSize(int rows) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Gets the fetch size.
     *
     * @return the fetch size
     * @throws SQLException the SQL exception
     */
    @Override
    public int getFetchSize() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Gets the result set concurrency.
     *
     * @return the result set concurrency
     * @throws SQLException the SQL exception
     */
    @Override
    public int getResultSetConcurrency() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Gets the result set type.
     *
     * @return the result set type
     * @throws SQLException the SQL exception
     */
    @Override
    public int getResultSetType() throws SQLException {
        return ResultSet.FETCH_FORWARD;
    }

    /**
     * Adds the batch.
     *
     * @param sql the sql
     * @throws SQLException the SQL exception
     */
    @Override
    public void addBatch(String sql) throws SQLException {
        if (sql != null) {
            WriteModel model;
            if (sql.startsWith("INSERT")) {
                model = new InsertOneModel<>(Document.parse(sql.substring("INSERT".length())));
            } else if (sql.startsWith("UPDATE")) {
                Document document = Document.parse(sql.substring("UPDATE".length()));
                model = new UpdateOneModel<>(new Document().append("_id", document.get("_id")), new Document("$set", document),
                        new UpdateOptions().upsert(true));
            } else if (sql.startsWith("DELETE")) {
                Document document = Document.parse(sql.substring("UPDATE".length()));
                model = new DeleteOneModel<>(new Document().append("_id", document.get("_id")));
            } else {
                model = new InsertOneModel<>(Document.parse(sql));
            }
            batchList.add(model);
        }

    }

    /**
     * Clear batch.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void clearBatch() throws SQLException {
        batchList.clear();
    }

    /**
     * Execute batch.
     *
     * @return the int[]
     * @throws SQLException the SQL exception
     */
    @Override
    public int[] executeBatch() throws SQLException {
        MongoDatabase db = this.conn.getMongoDatabase();
        BulkWriteOptions options = new BulkWriteOptions();
        options.ordered(false);
        options.bypassDocumentValidation(true);
        BulkWriteResult result = db.getCollection(currentCollection)
                                   .bulkWrite(batchList, options);
        clearBatch();
        return new int[] {result.getInsertedCount() + result.getModifiedCount() + result.getDeletedCount()};
    }

    /**
     * Gets the connection.
     *
     * @return the connection
     * @throws SQLException the SQL exception
     */
    @Override
    public Connection getConnection() throws SQLException {
        return this.conn;
    }

    /**
     * Gets the more results.
     *
     * @param current the current
     * @return the more results
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean getMoreResults(int current) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the generated keys.
     *
     * @return the generated keys
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Execute update.
     *
     * @param sql the sql
     * @param autoGeneratedKeys the auto generated keys
     * @return the int
     * @throws SQLException the SQL exception
     */
    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Execute update.
     *
     * @param sql the sql
     * @param columnIndexes the column indexes
     * @return the int
     * @throws SQLException the SQL exception
     */
    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Execute update.
     *
     * @param sql the sql
     * @param columnNames the column names
     * @return the int
     * @throws SQLException the SQL exception
     */
    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Execute.
     *
     * @param sql the sql
     * @param autoGeneratedKeys the auto generated keys
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Execute.
     *
     * @param sql the sql
     * @param columnIndexes the column indexes
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Execute.
     *
     * @param sql the sql
     * @param columnNames the column names
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Gets the result set holdability.
     *
     * @return the result set holdability
     * @throws SQLException the SQL exception
     */
    @Override
    public int getResultSetHoldability() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
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
     * Sets the poolable.
     *
     * @param poolable the new poolable
     * @throws SQLException the SQL exception
     */
    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Checks if is poolable.
     *
     * @return true, if is poolable
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isPoolable() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Close on completion.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void closeOnCompletion() throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Checks if is close on completion.
     *
     * @return true, if is close on completion
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

}
