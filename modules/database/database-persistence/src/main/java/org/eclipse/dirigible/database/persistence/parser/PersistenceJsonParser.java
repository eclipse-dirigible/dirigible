/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence.parser;

import org.eclipse.dirigible.database.persistence.PersistenceException;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;

import com.google.gson.Gson;

/**
 * The Persistence Json Parser.
 *
 * @param <T> the generic type
 */
public class PersistenceJsonParser<T> {

    /** The Constant gson. */
    private static final Gson gson = new Gson();

    /**
     * Parses the model.
     *
     * @param json the json
     * @return the persistence table model
     * @throws PersistenceException the persistence exception
     */
    public PersistenceTableModel parseModel(String json) throws PersistenceException {
        PersistenceTableModel persistenceTableModel = gson.fromJson(json, PersistenceTableModel.class);
        return persistenceTableModel;
    }

    /**
     * Serialize model.
     *
     * @param persistenceTableModel the persistence table model
     * @return the string
     */
    public String serializeModel(PersistenceTableModel persistenceTableModel) {
        return gson.toJson(persistenceTableModel);
    }

}
