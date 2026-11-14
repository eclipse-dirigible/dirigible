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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * A boolean property's words (#8564): its own {@code labels:} become the en-US catalog's
 * {@code <COLUMN>_TRUE} / {@code _FALSE} and the keys the views pass to {@code T()}; without them
 * the views read the shared {@code defaults.yes} / {@code defaults.no}, and the catalog gains
 * nothing.
 */
class BooleanWordsTest {

    @Test
    void aBooleanWithWordsIsTranslatedUnderItsOwnKeys() {
        Map<String, Object> active = active();
        active.put("widgetLabelTrue", "Active");
        active.put("widgetLabelFalse", "Owner's pick");

        ModelParameterProcessor.process(model(active), parameters());

        assertEquals("t.PRODUCT_ACTIVE_TRUE", active.get("booleanTrueKey"));
        assertEquals("Active", active.get("booleanTrueTextJs"));
        assertEquals("t.PRODUCT_ACTIVE_FALSE", active.get("booleanFalseKey"));
        assertEquals("Owner\\'s pick", active.get("booleanFalseTextJs"), "the word is printed into a JS string");
    }

    @Test
    void aBooleanWithoutWordsReadsTheSharedYesNo() {
        Map<String, Object> active = active();

        ModelParameterProcessor.process(model(active), parameters());

        assertEquals("defaults.yes", active.get("booleanTrueKey"));
        assertEquals("Yes", active.get("booleanTrueTextJs"));
        assertEquals("defaults.no", active.get("booleanFalseKey"));
        assertEquals("No", active.get("booleanFalseTextJs"));
    }

    @Test
    void aNonBooleanCarriesNoWords() {
        Map<String, Object> name = new LinkedHashMap<>();
        name.put("name", "Name");
        name.put("dataName", "PRODUCT_NAME");
        name.put("dataType", "VARCHAR");
        name.put("widgetType", "TEXTBOX");

        ModelParameterProcessor.process(model(name), parameters());

        assertNull(name.get("booleanTrueKey"));
    }

    @Test
    void theCatalogCarriesTheWordsNextToTheLabel() {
        Map<String, Object> active = active();
        active.put("widgetLabelTrue", "Active");
        active.put("widgetLabelFalse", "Inactive");
        Map<String, Object> catalog = new LinkedHashMap<>();

        ModelTranslations.fillEntityCatalog(model(active), catalog);

        @SuppressWarnings("unchecked")
        Map<String, Object> texts = (Map<String, Object>) catalog.get("t");
        assertEquals("Active", texts.get("PRODUCT_ACTIVE_TRUE"));
        assertEquals("Inactive", texts.get("PRODUCT_ACTIVE_FALSE"));
    }

    @Test
    void aCatalogWithoutWordsGainsNoKeys() {
        Map<String, Object> catalog = new LinkedHashMap<>();

        ModelTranslations.fillEntityCatalog(model(active()), catalog);

        @SuppressWarnings("unchecked")
        Map<String, Object> texts = (Map<String, Object>) catalog.get("t");
        assertFalse(texts.containsKey("PRODUCT_ACTIVE_TRUE"), "a model without labels generates as before");
    }

    private static Map<String, Object> active() {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", "Active");
        property.put("dataName", "PRODUCT_ACTIVE");
        property.put("dataType", "BOOLEAN");
        property.put("widgetType", "CHECKBOX");
        return property;
    }

    private static Map<String, Object> model(Map<String, Object> property) {
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("name", "Product");
        entity.put("dataName", "PRODUCT");
        entity.put("type", "PRIMARY");
        entity.put("perspectiveName", "catalog");
        entity.put("properties", List.of(property));
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("entities", List.of(entity));
        return model;
    }

    private static Map<String, Object> parameters() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("projectName", "shop");
        parameters.put("genFolderName", "shop");
        return parameters;
    }
}
