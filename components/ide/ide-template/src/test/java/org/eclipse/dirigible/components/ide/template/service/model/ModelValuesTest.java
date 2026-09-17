/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link ModelValues#isTrue(Map, String)} - the single reader for every {@code .edm} boolean
 * flag (dirigible #7231): it parses the string the model persists rather than presence-testing it,
 * in any case and trimmed, so every flag obeys one rule.
 */
class ModelValuesTest {

    @Test
    void lowercaseTrueHolds() {
        assertThat(ModelValues.isTrue(flag("true"), "flag")).isTrue();
    }

    @Test
    void aBooleanValueIsReadDirectly() {
        assertThat(ModelValues.isTrue(flag(Boolean.TRUE), "flag")).isTrue();
        assertThat(ModelValues.isTrue(flag(Boolean.FALSE), "flag")).isFalse();
    }

    @Test
    void trueHoldsHoweverItIsCasedOrPadded() {
        assertThat(ModelValues.isTrue(flag("TRUE"), "flag")).isTrue();
        assertThat(ModelValues.isTrue(flag("True"), "flag")).isTrue();
        assertThat(ModelValues.isTrue(flag(" true "), "flag")).isTrue();
    }

    @Test
    void aWrittenOutFalseIsFalseInAnyCase() {
        assertThat(ModelValues.isTrue(flag("false"), "flag")).isFalse();
        assertThat(ModelValues.isTrue(flag("FALSE"), "flag")).isFalse();
    }

    @Test
    void anAbsentKeyOrMapIsFalse() {
        assertThat(ModelValues.isTrue(new LinkedHashMap<>(), "flag")).isFalse();
        assertThat(ModelValues.isTrue(null, "flag")).isFalse();
    }

    private static Map<String, Object> flag(Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("flag", value);
        return map;
    }
}
