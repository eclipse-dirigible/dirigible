/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import java.util.Collection;

public class TypesUtil {

    public static boolean isPrimitiveWrapperOrStringCollection(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                             .allMatch(TypesUtil::isPrimitiveWrapperOrString);
        }
        return false;
    }

    public static boolean isPrimitiveWrapperOrString(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }

    public static boolean isShortsCollection(Collection collection) {
        return collection.stream()
                         .allMatch(e -> null == e || e instanceof Short);
    }

    public static boolean isIntsCollection(Collection collection) {
        return collection.stream()
                         .allMatch(e -> null == e || e instanceof Integer);
    }

    public static boolean isLongsCollection(Collection collection) {
        return collection.stream()
                         .allMatch(e -> null == e || e instanceof Long);
    }

    public static boolean isBooleansCollection(Collection collection) {
        return collection.stream()
                         .allMatch(e -> null == e || e instanceof Boolean);
    }

    public static boolean isStringsCollection(Collection collection) {
        return collection.stream()
                         .allMatch(e -> null == e || e instanceof String);
    }

    public static boolean isDoublesCollection(Collection collection) {
        return collection.stream()
                         .allMatch(e -> null == e || e instanceof Double);
    }

    public static boolean isFloatsCollection(Collection collection) {
        return collection.stream()
                         .allMatch(e -> null == e || e instanceof Float);
    }

    public static boolean isBytesCollection(Collection<?> collection) {
        return collection.stream()
                         .allMatch(e -> null == e || e instanceof Byte);
    }
}
