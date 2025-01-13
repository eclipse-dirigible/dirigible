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
 * Triple.
 *
 * @param <T> T
 * @param <U> U
 * @param <V> V
 */
public class Triple<T, U, V> implements Serializable {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 8719382431393826469L;

    /** The first. */
    private final T first;

    /** The second. */
    private final U second;

    /** The third. */
    private final V third;

    /**
     * Instantiates a new triple.
     *
     * @param a a
     * @param b b
     * @param c c
     */
    public Triple(T a, U b, V c) {
        first = a;
        second = b;
        third = c;
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
     * Gets the third.
     *
     * @return V
     */
    public V getThird() {
        return third;
    }

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
        result = prime * result + ((third == null) ? 0 : third.hashCode());
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
        Triple other = (Triple) obj;
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
        if (third == null) {
            if (other.third != null)
                return false;
        } else if (!third.equals(other.third))
            return false;
        return true;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "[" + first + ", " + second + ", " + third + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /**
     * Creates the.
     *
     * @param <T> T
     * @param <U> U
     * @param <V> V
     * @param a a
     * @param b b
     * @param c c
     * @return Triple
     */
    public static <T, U, V> Triple<T, U, V> create(T a, U b, V c) {
        return new Triple<T, U, V>(a, b, c);
    }

    /**
     * Null triple.
     *
     * @param <T> T
     * @param <U> U
     * @param <V> V
     * @return Triple
     */
    @SuppressWarnings("unchecked")
    public static final <T, U, V> Triple<T, U, V> nullTriple() {
        return (Triple<T, U, V>) NULL_TRIPLE;
    }

    /** The Constant NULL_TRIPLE. */
    @SuppressWarnings("rawtypes")
    public static final Triple NULL_TRIPLE = new Triple<Object, Object, Object>(null, null, null);
}
