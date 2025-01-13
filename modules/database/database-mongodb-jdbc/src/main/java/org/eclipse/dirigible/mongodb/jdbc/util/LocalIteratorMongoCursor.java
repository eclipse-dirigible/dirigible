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

import java.util.Iterator;

import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.client.MongoCursor;

/**
 * The Class LocalIteratorMongoCursor.
 */
public class LocalIteratorMongoCursor implements MongoCursor<String> {

    /** The iterator. */
    Iterator<String> iterator;

    /**
     * Instantiates a new local iterator mongo cursor.
     *
     * @param iterator the iterator
     */
    LocalIteratorMongoCursor(Iterator<String> iterator) {
        this.iterator = iterator;
    }

    /**
     * Close.
     */
    @Override
    public void close() {}

    /**
     * Checks for next.
     *
     * @return true, if successful
     */
    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * Next.
     *
     * @return the string
     */
    @Override
    public String next() {
        return iterator.next();
    }

    /**
     * Try next.
     *
     * @return the string
     */
    @Override
    public String tryNext() {
        return null;
    }

    /**
     * Gets the server cursor.
     *
     * @return the server cursor
     */
    @Override
    public ServerCursor getServerCursor() {
        return null;
    }

    /**
     * Gets the server address.
     *
     * @return the server address
     */
    @Override
    public ServerAddress getServerAddress() {
        return null;
    }

}
