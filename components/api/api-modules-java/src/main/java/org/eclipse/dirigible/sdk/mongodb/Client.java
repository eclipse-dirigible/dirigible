/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.mongodb;

import com.mongodb.DBObject;
import com.mongodb.client.MongoClient;
import org.eclipse.dirigible.components.api.mongodb.MongoDBFacade;

/**
 * Hands back a configured Mongo {@link MongoClient}. Callers can address any database / collection
 * on it directly and use the full driver surface — aggregation pipelines, transactions, GridFS.
 * <p>
 * {@link #getDefaultDatabaseName()} returns the database name configured in the platform properties
 * (often the entry point for application-managed collections); {@link #createBasicDBObject()}
 * returns a fresh empty {@link DBObject} for callers that use the legacy DBObject API.
 */
public final class Client {

    private Client() {}

    public static MongoClient getClient(String uri, String user, String password) {
        return MongoDBFacade.getClient(uri, user, password);
    }

    public static String getDefaultDatabaseName() {
        return MongoDBFacade.getDefaultDatabaseName();
    }

    public static DBObject createBasicDBObject() {
        return MongoDBFacade.createBasicDBObject();
    }
}
