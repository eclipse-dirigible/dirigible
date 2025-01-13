/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence;

import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.eclipse.dirigible.database.persistence.parser.PersistenceAnnotationsParser;
import org.eclipse.dirigible.database.persistence.parser.PersistenceJsonParser;

/**
 * A factory for creating Persistence objects.
 */
public class PersistenceFactory {

    /**
     * Creates a new Persistence object.
     *
     * @param pojo the pojo
     * @return the persistence table model
     * @throws PersistenceException the persistence exception
     */
    public static PersistenceTableModel createModel(Object pojo) throws PersistenceException {
        PersistenceAnnotationsParser parser = new PersistenceAnnotationsParser();
        PersistenceTableModel persistenceModel = parser.parsePojo(pojo);
        return persistenceModel;
    }

    /**
     * Creates a new Persistence object.
     *
     * @param clazz the clazz
     * @return the persistence table model
     * @throws PersistenceException the persistence exception
     */
    public static PersistenceTableModel createModel(Class<? extends Object> clazz) throws PersistenceException {
        PersistenceAnnotationsParser parser = new PersistenceAnnotationsParser();
        PersistenceTableModel persistenceModel = parser.parsePojo(clazz);
        return persistenceModel;
    }

    /**
     * Creates a new Persistence object.
     *
     * @param json the json
     * @return the persistence table model
     * @throws PersistenceException the persistence exception
     */
    public static PersistenceTableModel createModel(String json) throws PersistenceException {
        PersistenceJsonParser<?> parser = new PersistenceJsonParser<>();
        PersistenceTableModel persistenceModel = parser.parseModel(json);
        return persistenceModel;
    }

}
