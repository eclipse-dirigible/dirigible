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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.mongodb.Block;
import com.mongodb.Function;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;

/**
 * The Class ListMongoIterable.
 */
public class ListMongoIterable implements MongoIterable<String> {

    /** The entries. */
    List<String> entries;

    /**
     * Instantiates a new list mongo iterable.
     *
     * @param backingList the backing list
     */
    public ListMongoIterable(List<String> backingList) {
        this.entries = backingList;
    }

    /**
     * Map.
     *
     * @param <U> the generic type
     * @param mapper the mapper
     * @return the mongo iterable
     */
    @Override
    public <U> MongoIterable<U> map(Function<String, U> mapper) {
        return null;
    }

    /**
     * Iterator.
     *
     * @return the mongo cursor
     */
    @Override
    public MongoCursor<String> iterator() {
        return new LocalIteratorMongoCursor(entries.iterator());
    }

    /**
     * Into.
     *
     * @param <A> the generic type
     * @param target the target
     * @return the a
     */
    @Override
    public <A extends Collection<? super String>> A into(A target) {
        return null;
    }

    /**
     * For each.
     *
     * @param block the block
     */
    @Override
    public void forEach(Block<? super String> block) {}

    /**
     * First.
     *
     * @return the string
     */
    @Override
    public String first() {
        Iterator<String> iter = this.iterator();
        return iter.hasNext() ? this.iterator()
                                    .next()
                : null;
    }

    /**
     * Batch size.
     *
     * @param batchSize the batch size
     * @return the mongo iterable
     */
    @Override
    public MongoIterable<String> batchSize(int batchSize) {
        return this;
    }

    /**
     * Cursor.
     *
     * @return the mongo cursor
     */
    @Override
    public MongoCursor<String> cursor() {
        return null;
    }

}
