/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.mongodb.jdbc.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.bson.Document;
import org.eclipse.dirigible.mongodb.jdbc.MongoDBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoCursor;

/**
 * The Class ExportImportUtil.
 */
public class ExportImportUtil {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ExportImportUtil.class);

    /**
     * Export collection.
     *
     * @param connection the connection
     * @param collection the collection
     * @param output the output
     * @throws Exception the exception
     */
    public static void exportCollection(MongoDBConnection connection, String collection, OutputStream output) throws Exception {
        MongoCursor<Document> cursor = connection.getMongoDatabase()
                                                 .getCollection(collection)
                                                 .find()
                                                 .iterator();
        try (Writer writer = new OutputStreamWriter(output)) {
            writer.append("[");
            while (cursor.hasNext()) {
                writer.write(cursor.next()
                                   .toJson()
                        + (cursor.hasNext() ? "," : ""));
            }
            writer.append("]");
        }
        output.flush();
        cursor.close();
    }

    /**
     * Import collection.
     *
     * @param connection the connection
     * @param collection the collection
     * @param input the input
     * @throws Exception the exception
     */
    public static void importCollection(MongoDBConnection connection, String collection, InputStream input) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        try (JsonParser jsonParser = mapper.getFactory()
                                           .createParser(input)) {

            if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected content to be an array");
            }

            while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                try {
                    Document document = Document.parse(jsonParser.readValueAsTree()
                                                                 .toString());
                    connection.getMongoDatabase()
                              .getCollection(collection)
                              .insertOne(document);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

    }

}
