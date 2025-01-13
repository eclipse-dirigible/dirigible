/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.test.util;

import java.io.Serializable;

/**
 * Pair.
 *
 * @param <T> t
 * @param <U> u
 */
public class Pair<T, U> implements Serializable {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 8719382431393826469L;

    /** The first. */
    private final T first;

    /** The second. */
    private final U second;

    /**
     * Instantiates a new pair.
     */
    // To Support Ion Serialization
    private Pair() {
        this.first = null;
        this.second = null;
    }

    /**
     * Instantiates a new pair.
     *
     * @param a a
     * @param b b
     */
    public Pair(T a, U b) {
        this.first = a;
        this.second = b;
    }

    /**
     * Gets the first.
     *
     * @return T
     */
    public T getFirst() {
        return first;
    }

    /**
     * Gets the second.
     *
     * @return U
     */
    public U getSecond() {
        return second;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "[" + first + ", " + second + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /**
     * Creates the.
     *
     * @param <T> T
     * @param <U> U
     * @param a a
     * @param b b
     * @return Pair
     */
    public static <T, U> Pair<T, U> create(T a, U b) {
        return new Pair<T, U>(a, b);
    }

    /**
     * Null pair.
     *
     * @param <T> T
     * @param <U> U
     * @return Pair
     */
    @SuppressWarnings("unchecked")
    public static final <T, U> Pair<T, U> nullPair() {
        return (Pair<T, U>) NULL_PAIR;
    }

    /** The Constant NULL_PAIR. */
    @SuppressWarnings("rawtypes")
    public static final Pair NULL_PAIR = new Pair<Object, Object>(null, null);

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((first == null) ? 0 : first.hashCode());
        result = prime * result + ((second == null) ? 0 : second.hashCode());
        return result;
    }

    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Pair other = (Pair) obj;
        if (first == null) {
            if (other.first != null)
                return false;
        } else if (!first.equals(other.first))
            return false;
        if (second == null) {
            if (other.second != null)
                return false;
        } else if (!second.equals(other.second))
            return false;
        return true;
    }
}
