/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.mongodb;

import java.net.URI;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.mongodb.jdbc.MongoDBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoClient;

/**
 * The Class MongoDBFacade.
 */
@Component
public class MongoDBFacade {

    /** The Constant DIRIGIBLE_MONGODB_CLIENT_URI. */
    private static final String DIRIGIBLE_MONGODB_CLIENT_URI = "DIRIGIBLE_MONGODB_CLIENT_URI";

    /** The Constant CLIENT_URI. */
    private static final String CLIENT_URI = "mongodb://localhost:27017";

    /** The Constant DIRIGIBLE_MONGODB_DATABASE_DEFAULT. */
    private static final String DIRIGIBLE_MONGODB_DATABASE_DEFAULT = "DIRIGIBLE_MONGODB_DATABASE_DEFAULT";

    /** The Constant DIRIGIBLE_MONGODB_DATABASE_DEFAULT_DB. */
    private static final String DIRIGIBLE_MONGODB_DATABASE_DEFAULT_DB = "db";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(MongoDBFacade.class);


    /**
     * Gets the client.
     *
     * @param uri the uri
     * @param user the user
     * @param password the password
     * @return the client
     */
    public static MongoClient getClient(String uri, String user, String password) {
        String defaultUri = Configuration.get(DIRIGIBLE_MONGODB_CLIENT_URI, CLIENT_URI);
        URI dbUri = URI.create(uri != null ? uri : defaultUri);
        MongoClient mongoClient = MongoDBConnection.createMongoClient(dbUri.toString(), user, password);
        return mongoClient;
    }

    /**
     * Creates the basic DB object.
     *
     * @return the DB object
     */
    public static DBObject createBasicDBObject() {
        return new BasicDBObject();
    }

    /**
     * Gets the default database name.
     *
     * @return the default database name
     */
    public static String getDefaultDatabaseName() {
        return Configuration.get(DIRIGIBLE_MONGODB_DATABASE_DEFAULT, DIRIGIBLE_MONGODB_DATABASE_DEFAULT_DB);
    }

}
