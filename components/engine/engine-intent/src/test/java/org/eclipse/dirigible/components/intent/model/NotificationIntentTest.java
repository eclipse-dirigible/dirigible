/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The embedded notify block's vocabulary - {@link NotificationIntent#BLOCK_KEYS} - is what the
 * parser rejects unknown keys against, so it must stay exactly the set {@code fromMap} reads.
 */
class NotificationIntentTest {

    /** {@code name} / {@code event} are the two properties an EMBEDDED block deliberately never has. */
    private static final Set<String> NOT_IN_A_BLOCK = Set.of("name", "event");

    /**
     * The drift guard: a property added to the block and read by {@code fromMap}, but not listed in
     * {@code BLOCK_KEYS}, would be rejected as an unknown key - i.e. unauthorable the day it ships.
     */
    @Test
    void theBlockVocabularyIsEveryEmbeddableProperty() {
        Set<String> properties = new LinkedHashSet<>();
        for (Field field : NotificationIntent.class.getDeclaredFields()) {
            if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers()) && !NOT_IN_A_BLOCK.contains(field.getName())) {
                properties.add(field.getName());
            }
        }
        assertEquals(properties, NotificationIntent.BLOCK_KEYS,
                "every embeddable property must be authorable, and nothing else may be listed");
    }

    @Test
    void everyListedKeyIsActuallyRead() {
        for (String key : NotificationIntent.BLOCK_KEYS) {
            NotificationIntent block = NotificationIntent.fromMap(Map.of(key, "x"));
            assertNotNull(block, "fromMap should read a map for key [" + key + "]");
            assertEquals("x", read(block, key), "fromMap ignores the declared block key [" + key + "]");
        }
    }

    @Test
    void aNonMapIsNoBlockAtAll() {
        assertNull(NotificationIntent.fromMap("member.email"));
        assertNull(NotificationIntent.fromMap(null));
    }

    private static Object read(NotificationIntent block, String property) {
        try {
            Field field = NotificationIntent.class.getDeclaredField(property);
            field.setAccessible(true);
            return field.get(block);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("no property [" + property + "] on the notify block", ex);
        }
    }
}
