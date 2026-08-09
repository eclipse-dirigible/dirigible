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

import org.eclipse.dirigible.commons.api.helpers.NamingHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.asMap;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.asMaps;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.str;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.strOr;

/**
 * Builds the translation catalogs a generation emits, and migrates the two model formats that
 * predate them.
 *
 * <p>
 * A catalog is keyed by a translation id derived from the label itself, so the same label always
 * resolves to the same key across regenerations. The catalog is nested under the model file's own
 * prefix, which keeps two models in one project from colliding.
 */
final class ModelTranslations {

    /**
     * Not instantiable.
     */
    private ModelTranslations() {}

    /**
     * Derives the translation id of a label by stripping the characters that are not allowed in a key.
     *
     * @param value the label
     * @return the translation id
     */
    static String translationId(String value) {
        return value.replace(" ", "")
                    .replace("_", "")
                    .replace(".", "")
                    .replace(":", "");
    }

    /**
     * Derives the catalog prefix from the model file's path - its file name, with the extension
     * separator turned into a hyphen.
     *
     * @param filePath the model file path
     * @return the catalog prefix
     */
    static String catalogPrefix(String filePath) {
        return filePath.substring(filePath.lastIndexOf('/') + 1)
                       .replace(" ", "")
                       .replace("_", "")
                       .replace(".", "-")
                       .replace(":", "");
    }

    /**
     * Derives the path a catalog is written to.
     *
     * @param filePath the model file path
     * @return the project-relative catalog path
     */
    static String catalogPath(String filePath) {
        return "i18n/en-US/" + filePath.substring(filePath.lastIndexOf('/') + 1) + ".json";
    }

    /**
     * Collects every label and error message in a form model, replacing each with a reference to its
     * translation id. The model is annotated in place, so the rendered form points at the catalog.
     *
     * @param node the model, or any node of it
     * @return the collected translations
     */
    static Map<String, Object> formTranslations(Object node) {
        Map<String, Object> translations = new LinkedHashMap<>();
        collectFormTranslations(node, translations);
        return translations;
    }

    /**
     * Walks one node of a form model.
     *
     * @param node the node
     * @param translations the catalog being built
     */
    private static void collectFormTranslations(Object node, Map<String, Object> translations) {
        if (node instanceof List<?> list) {
            for (Object element : list) {
                collectFormTranslations(element, translations);
            }
            return;
        }
        Map<String, Object> map = asMap(node);
        if (map == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : new ArrayList<>(map.entrySet())) {
            String key = entry.getKey();
            Object value = entry.getValue();
            boolean translatable = "label".equals(key) || "errorMessage".equals(key);
            if (translatable && value != null && !"".equals(value)) {
                String id = translationId(value.toString());
                translations.put(id, value);
                map.put("errorMessage".equals(key) ? "errorTranslation" : "translation", id);
            } else if (!translatable && (value instanceof Map || value instanceof List)) {
                collectFormTranslations(value, translations);
            }
        }
    }

    /**
     * Collects a report's own label, its description and its column labels. The description is keyed
     * separately so it is localized too, and a report-attached dashboard widget contributes its tile
     * label.
     *
     * @param report the report model
     * @return the collected translations
     */
    static Map<String, Object> reportTranslations(Map<String, Object> report) {
        Map<String, Object> translations = new LinkedHashMap<>();
        translations.put(str(report, "tId"), report.get("label"));
        String descriptionId = str(report, "descriptionTId");
        if (descriptionId != null && ModelValues.truthy(report, "description")) {
            translations.put(descriptionId, report.get("description"));
        }
        for (Map<String, Object> column : asMaps(report.get("columns"))) {
            translations.put(str(column, "tId"), column.get("label"));
        }
        Map<String, Object> widget = asMap(report.get("widget"));
        if (widget != null && widget.get("tId") != null) {
            translations.put(str(widget, "tId"), strOr(widget, "label", str(report, "label")));
        }
        return translations;
    }

    /**
     * Fills the entity-model catalog: the entities' singular and plural display names, their property
     * labels, the perspective and navigation labels, the dashboard widget labels, and the custom action
     * and process task labels.
     *
     * @param model the entity model
     * @param catalog the parsed catalog to fill, as loaded from the template
     */
    static void fillEntityCatalog(Map<String, Object> model, Map<String, Object> catalog) {
        Map<String, Object> texts = asMap(catalog.get("t"));
        if (texts == null) {
            texts = new LinkedHashMap<>();
            catalog.put("t", texts);
        }
        for (Map<String, Object> entity : asMaps(model.get("entities"))) {
            String dataName = str(entity, "dataName");
            String name = str(entity, "name");
            if (dataName != null && name != null) {
                // The singular name feeds the form captions and the "New X" actions, the plural one
                // the sidebar entries and the list titles. A model that carries its own labels wins;
                // a hand-authored one gets them derived here.
                String singular = strOr(entity, "entityLabel", NamingHelper.humanizeIdentifier(name));
                texts.put(dataName, singular);
                texts.put(dataName + "_plural", strOr(entity, "menuLabel", NamingHelper.pluralizeLabel(singular)));
            }
            collectPropertyLabels(asMaps(entity.get("properties")), texts);
            collectMasterPropertyLabels(asMap(entity.get("masterProperties")), texts);
        }
        for (Map<String, Object> perspective : asMaps(model.get("perspectives"))) {
            if (ModelValues.truthy(perspective, "header")) {
                texts.put(str(perspective, "name") + "pheader", perspective.get("label"));
            }
            texts.put(str(perspective, "name"), perspective.get("label"));
        }
        for (Map<String, Object> navigation : asMaps(model.get("navigations"))) {
            if (ModelValues.truthy(navigation, "header")) {
                texts.put(str(navigation, "id") + "nheader", navigation.get("header"));
            }
            texts.put(str(navigation, "id"), navigation.get("label"));
        }
        for (Map<String, Object> widget : asMaps(model.get("widgets"))) {
            if (widget.get("tId") != null) {
                texts.put(str(widget, "tId"), strOr(widget, "label", str(widget, "name")));
            }
        }
        // Per-record custom action labels and BPM user-task labels live under their own sub-objects,
        // because the views resolve them through those namespaces.
        Map<String, Object> customActionLabels = asMap(model.get("customActionLabels"));
        if (customActionLabels != null) {
            catalog.put("actions", new LinkedHashMap<>(customActionLabels));
        }
        Map<String, Object> processTaskLabels = asMap(model.get("processTaskLabels"));
        if (processTaskLabels != null) {
            catalog.put("processes", new LinkedHashMap<>(processTaskLabels));
        }
    }

    /**
     * Collects the labels of a property list, preferring the perspective label, then the widget label,
     * then the property name.
     *
     * @param properties the properties
     * @param texts the catalog being filled
     */
    private static void collectPropertyLabels(List<Map<String, Object>> properties, Map<String, Object> texts) {
        for (Map<String, Object> property : properties) {
            String dataName = str(property, "dataName");
            if (dataName != null) {
                if (ModelValues.truthy(property, "perspectiveHeader")) {
                    texts.put(str(property, "perspectiveName") + "pheader", property.get("perspectiveHeader"));
                }
                if (ModelValues.truthy(property, "perspectiveLabel")) {
                    texts.put(dataName, property.get("perspectiveLabel"));
                } else if (ModelValues.truthy(property, "widgetLabel")) {
                    texts.put(dataName, property.get("widgetLabel"));
                } else if (ModelValues.truthy(property, "name")) {
                    texts.put(dataName, property.get("name"));
                }
            }
            collectMasterPropertyLabels(asMap(property.get("masterProperties")), texts);
        }
    }

    /**
     * Collects the labels of a master layout's object header.
     *
     * @param masterProperties the master properties, may be null
     * @param texts the catalog being filled
     */
    private static void collectMasterPropertyLabels(Map<String, Object> masterProperties, Map<String, Object> texts) {
        if (masterProperties == null) {
            return;
        }
        Map<String, Object> title = asMap(masterProperties.get("title"));
        if (title != null && title.get("dataName") != null) {
            if (ModelValues.truthy(title, "widgetLabel")) {
                texts.put(str(title, "dataName"), title.get("widgetLabel"));
            } else if (ModelValues.truthy(title, "name")) {
                texts.put(str(title, "dataName"), title.get("name"));
            }
        }
        if (masterProperties.get("properties") != null) {
            collectPropertyLabels(asMaps(masterProperties.get("properties")), texts);
        }
    }

    /**
     * Brings a form model authored before the current field names up to date, in place.
     *
     * @param model the form model
     * @param fileName the model's file name, used when the form declares no name
     */
    static void migrateForm(Map<String, Object> model, String fileName) {
        Map<String, Object> metadata = asMap(model.get("metadata"));
        if (metadata == null) {
            metadata = new LinkedHashMap<>();
            metadata.put("name", formName(model, fileName));
            model.put("metadata", metadata);
        } else if (!metadata.containsKey("name")) {
            metadata.put("name", formName(model, fileName));
        }
        for (Map<String, Object> item : asMaps(model.get("form"))) {
            rename(item, "title", "label");
            rename(item, "name", "label");
            rename(item, "errorState", "errorMessage");
            rename(item, "size", "headerSize");
            if ("header".equals(str(item, "controlId")) && !item.containsKey("level")) {
                ModelValues.putNumber(item, "level", 1);
            }
        }
    }

    /**
     * Derives a form's display name from its top-level header, falling back to the file name.
     *
     * @param model the form model
     * @param fileName the model's file name
     * @return the form name
     */
    private static String formName(Map<String, Object> model, String fileName) {
        for (Map<String, Object> item : asMaps(model.get("form"))) {
            Object headerSize = item.get("headerSize");
            if ("header".equals(str(item, "controlId")) && headerSize instanceof Number number && number.doubleValue() == 1d) {
                return str(item, "label") + " Form";
            }
        }
        return fileName;
    }

    /**
     * Brings a report model authored before the translation ids up to date, in place.
     *
     * @param report the report model
     */
    static void migrateReport(Map<String, Object> report) {
        if (!report.containsKey("tId")) {
            report.put("tId", translationId(strOr(report, "alias", "")));
            report.put("label", report.get("alias"));
        }
        for (Map<String, Object> column : asMaps(report.get("columns"))) {
            if (!column.containsKey("tId")) {
                column.put("tId", translationId(strOr(column, "alias", "")));
                column.put("label", column.get("alias"));
            }
        }
    }

    /**
     * Moves a value to its current key, leaving an already-present target key in place so the model's
     * key order - which the catalog's key order follows - does not shift.
     *
     * @param item the node
     * @param from the legacy key
     * @param to the current key
     */
    private static void rename(Map<String, Object> item, String from, String to) {
        if (item.containsKey(from)) {
            item.put(to, item.get(from));
            item.remove(from);
        }
    }

}
