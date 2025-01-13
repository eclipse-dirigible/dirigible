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

import java.util.List;

import org.eclipse.dirigible.database.persistence.model.PersistenceTableColumnModel;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;

import com.google.gson.Gson;

/**
 * Serialization Utility.
 */
public class Serializer {

    /** The gson. */
    private static Gson gson = new Gson();

    /**
     * Serializes a table model.
     *
     * @param tableModel the model
     * @return the serialized model
     */
    public static String serializeTableModel(PersistenceTableModel tableModel) {
        try {
            return gson.toJson(tableModel);
        } catch (Throwable e) {
            return "Error in serialization of the table model";
        }
    }

    /**
     * Serializes a POJO instance.
     *
     * @param pojo the POJO instance
     * @return the serialized POJO instance
     */
    public static String serializePojo(Object pojo) {
        try {
            return gson.toJson(pojo);
        } catch (Throwable e) {
            return "Error in serialization of the pojo instance";
        }
    }

    /**
     * Serialize column model.
     *
     * @param columnModel the column model
     * @return the string
     */
    public static String serializeColumnModel(PersistenceTableColumnModel columnModel) {
        try {
            return gson.toJson(columnModel);
        } catch (Throwable e) {
            return "Error in serialization of the column model";
        }
    }

    /**
     * Serialize list of objects.
     *
     * @param objects the objects
     * @return the string
     */
    public static String serializeListOfObjects(List<Object> objects) {
        if (objects == null || objects.isEmpty()) {
            return "List is empty or null";
        }
        StringBuilder builder = new StringBuilder();
        for (Object object : objects) {
            try {
                builder.append(gson.toJson(object));
            } catch (Throwable e) {
                return "Error in serialization of an object from the list";
            }
        }
        return builder.toString();
    }

}
