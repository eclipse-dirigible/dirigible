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

import java.lang.reflect.Field;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

/**
 * https://github.com/galovics/hibernate-encryption-listener
 */
@Component
public class FieldDecrypter {

    /** The decrypter. */
    @Autowired
    private Decrypter decrypter;

    /**
     * Decrypt.
     *
     * @param state the state
     * @param propertyNames the property names
     * @param entity the entity
     */
    public void decrypt(Object[] state, String[] propertyNames, Object entity) {
        ReflectionUtils.doWithFields(entity.getClass(), field -> decryptField(field, state, propertyNames),
                EncryptionUtils::isFieldEncrypted);
    }

    /**
     * Decrypt field.
     *
     * @param field the field
     * @param state the state
     * @param propertyNames the property names
     */
    private void decryptField(Field field, Object[] state, String[] propertyNames) {
        int propertyIndex = EncryptionUtils.getPropertyIndex(field.getName(), propertyNames);
        Object currentValue = state[propertyIndex];
        if (currentValue != null) {
            if (!(currentValue instanceof String)) {
                throw new IllegalStateException("Encrypted annotation was used on a non-String field");
            }
            state[propertyIndex] = decrypter.decrypt(currentValue.toString());
        }
    }
}
