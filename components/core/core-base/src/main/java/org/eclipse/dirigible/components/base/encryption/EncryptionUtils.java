/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.encryption;

import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Field;

/**
 * https://github.com/galovics/hibernate-encryption-listener
 */
public abstract class EncryptionUtils {

    /**
     * Checks if is field encrypted.
     *
     * @param field the field
     * @return true, if is field encrypted
     */
    public static boolean isFieldEncrypted(Field field) {
        return AnnotationUtils.findAnnotation(field, Encrypted.class) != null;
    }

    /**
     * Gets the property index.
     *
     * @param name the name
     * @param properties the properties
     * @return the property index
     */
    public static int getPropertyIndex(String name, String[] properties) {
        for (int i = 0; i < properties.length; i++) {
            if (name.equals(properties[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("No property was found for name " + name);
    }
}
