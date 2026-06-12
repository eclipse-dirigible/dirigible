/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.FormIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Emits one {@code <form>.form} per {@link FormIntent} declared in the intent. The output is the
 * JSON shape consumed by the form-builder editor in the IDE - a {@code form} array of typed
 * controls (header, input-textfield / input-number / input-date / input-checkbox, container-hbox
 * with buttons) plus the customary {@code metadata} / {@code feeds} / {@code scripts} /
 * {@code code} companions.
 *
 * <p>
 * When the form has a {@code forEntity}, every entry in the intent's {@code fields} list is looked
 * up against the bound entity to pick a typed control:
 * <ul>
 * <li>{@code string} / {@code uuid} -> {@code input-textfield}</li>
 * <li>{@code text} -> {@code input-textarea}</li>
 * <li>{@code integer} / {@code long} / {@code decimal} / {@code double} ->
 * {@code input-number}</li>
 * <li>{@code boolean} -> {@code input-checkbox}</li>
 * <li>{@code date} -> {@code input-date}</li>
 * <li>{@code timestamp} -> {@code input-datetime-local}</li>
 * </ul>
 * Fields that are not declared on the bound entity (or forms with no {@code forEntity}) fall back
 * to {@code input-textfield}.
 *
 * <p>
 * Actions become buttons in a trailing {@code container-hbox}. The button {@code type} is inferred
 * from the action name ({@code approve} -> positive, {@code reject}/{@code decline}/{@code delete}
 * -> negative, {@code save}/{@code submit} -> emphasized, anything else -> standard). Each button
 * carries a {@code callback} like {@code onApproveClicked()} pointing at a stub handler in the
 * {@code code} block. The stub does nothing; wiring it to an actual backend is left to the
 * downstream template engine or a hand-written form override under {@code custom/}.
 *
 * <p>
 * Idempotent: identical input always produces byte-identical output.
 */
@Component
@Order(400)
public class FormIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormIntentGenerator.class);

    @Override
    public String name() {
        return "form";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        if (model.getForms()
                 .isEmpty()) {
            return;
        }
        Map<String, EntityIntent> entitiesByName = indexEntities(model);
        Set<String> seenFiles = new HashSet<>();
        for (FormIntent form : model.getForms()) {
            if (form.getName() == null || form.getName()
                                              .isBlank()) {
                LOGGER.warn("Skipping unnamed form in intent [{}]", context.getIntent()
                                                                           .getName());
                continue;
            }
            String fileName = form.getName() + ".form";
            if (!seenFiles.add(fileName)) {
                LOGGER.warn("Duplicate form [{}] in intent [{}] - keeping the first occurrence", form.getName(), context.getIntent()
                                                                                                                        .getName());
                continue;
            }
            EntityIntent boundEntity = form.getForEntity() == null ? null : entitiesByName.get(form.getForEntity());
            Map<String, Object> document = buildForm(form, boundEntity);
            context.writeModelFile(fileName, JsonHelper.toJson(document));
        }
    }

    private static Map<String, EntityIntent> indexEntities(IntentModel model) {
        Map<String, EntityIntent> index = new HashMap<>();
        for (EntityIntent entity : model.getEntities()) {
            if (entity.getName() != null) {
                index.put(entity.getName(), entity);
            }
        }
        return index;
    }

    private static Map<String, Object> buildForm(FormIntent form, EntityIntent entity) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("metadata", new LinkedHashMap<>());
        document.put("feeds", new ArrayList<>());
        document.put("scripts", new ArrayList<>());
        document.put("code", buildCode(form));
        document.put("form", buildControls(form, entity));
        return document;
    }

    private static String buildCode(FormIntent form) {
        if (form.getActions()
                .isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String action : form.getActions()) {
            if (action == null || action.isBlank()) {
                continue;
            }
            sb.append("$scope.on")
              .append(pascalCase(action))
              .append("Clicked = function () {\n");
            sb.append("    // TODO: wire ")
              .append(action)
              .append(" action\n");
            sb.append("};\n\n");
        }
        return sb.toString();
    }

    private static List<Map<String, Object>> buildControls(FormIntent form, EntityIntent entity) {
        List<Map<String, Object>> controls = new ArrayList<>();
        controls.add(headerControl(form));
        Map<String, FieldIntent> fieldsByName = new HashMap<>();
        if (entity != null) {
            for (FieldIntent field : entity.getFields()) {
                if (field.getName() != null) {
                    fieldsByName.put(field.getName(), field);
                }
            }
        }
        for (String fieldName : form.getFields()) {
            if (fieldName == null || fieldName.isBlank()) {
                continue;
            }
            controls.add(fieldControl(fieldName, fieldsByName.get(fieldName)));
        }
        if (!form.getActions()
                 .isEmpty()) {
            controls.add(actionRow(form));
        }
        return controls;
    }

    private static Map<String, Object> headerControl(FormIntent form) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("controlId", "header");
        header.put("groupId", "fb-display");
        String label = form.getDescription() != null && !form.getDescription()
                                                             .isBlank() ? form.getDescription() : form.getName();
        header.put("label", label);
        header.put("headerSize", 2);
        header.put("level", 1);
        header.put("padding", "tiny");
        header.put("side", "bottom");
        return header;
    }

    private static Map<String, Object> fieldControl(String fieldName, FieldIntent field) {
        Control control = pickControl(field);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("controlId", control.controlId);
        map.put("groupId", "fb-controls");
        map.put("id", fieldName + "Id");
        map.put("label", humanize(fieldName));
        map.put("horizontal", false);
        map.put("isCompact", false);
        map.put("readonly", field != null && field.isPrimaryKey() && field.isGenerated());
        if (control.htmlType != null) {
            map.put("type", control.htmlType);
        }
        map.put("model", fieldName);
        map.put("required", field != null && field.isRequired());
        if ("input-textfield".equals(control.controlId) || "input-textarea".equals(control.controlId)) {
            map.put("minLength", 0);
            map.put("maxLength", field != null && field.getLength() != null ? field.getLength() : -1);
            map.put("errorMessage", "Incorrect input");
        }
        return map;
    }

    private static Map<String, Object> actionRow(FormIntent form) {
        List<Map<String, Object>> buttons = new ArrayList<>();
        for (String action : form.getActions()) {
            if (action == null || action.isBlank()) {
                continue;
            }
            buttons.add(actionButton(action));
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("controlId", "container-hbox");
        row.put("groupId", "fb-containers");
        row.put("children", buttons);
        row.put("justify", "end");
        return row;
    }

    private static Map<String, Object> actionButton(String action) {
        Map<String, Object> button = new LinkedHashMap<>();
        button.put("controlId", "button");
        button.put("groupId", "fb-controls");
        button.put("label", humanize(action));
        button.put("type", buttonType(action));
        button.put("isSubmit", true);
        button.put("isCompact", false);
        button.put("callback", "on" + pascalCase(action) + "Clicked()");
        return button;
    }

    private static String buttonType(String action) {
        switch (action.toLowerCase(Locale.ROOT)) {
            case "approve":
            case "accept":
            case "confirm":
                return "positive";
            case "reject":
            case "decline":
            case "delete":
            case "remove":
            case "cancel":
                return "negative";
            case "save":
            case "submit":
            case "create":
            case "update":
                return "emphasized";
            default:
                return "standard";
        }
    }

    private static Control pickControl(FieldIntent field) {
        if (field == null || field.getType() == null) {
            return new Control("input-textfield", "text");
        }
        switch (field.getType()
                     .toLowerCase(Locale.ROOT)) {
            case "text":
                return new Control("input-textarea", "text");
            case "integer":
            case "int":
            case "long":
            case "decimal":
            case "double":
                return new Control("input-number", "number");
            case "boolean":
                return new Control("input-checkbox", null);
            case "date":
                return new Control("input-date", "date");
            case "timestamp":
                return new Control("input-datetime-local", "datetime-local");
            case "uuid":
            case "string":
            default:
                return new Control("input-textfield", "text");
        }
    }

    /**
     * Convert a camelCase / snake_case / kebab-case identifier to a human label. {@code firstName}
     * becomes {@code First Name}; {@code from_date} becomes {@code From Date}.
     */
    private static String humanize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(raw.length() + 4);
        boolean capitalizeNext = true;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '_' || c == '-') {
                out.append(' ');
                capitalizeNext = true;
                continue;
            }
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(raw.charAt(i - 1))) {
                out.append(' ');
            }
            if (capitalizeNext) {
                out.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Convert an action name to PascalCase suitable for an Angular callback identifier.
     * {@code submit-request} becomes {@code SubmitRequest}.
     */
    private static String pascalCase(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(raw.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '_' || c == '-' || c == ' ') {
                capitalizeNext = true;
                continue;
            }
            if (capitalizeNext) {
                out.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Internal pairing of form-builder control id with HTML input type. The HTML {@code type} attribute
     * is omitted for control IDs that don't take one (e.g. checkbox).
     */
    private static final class Control {
        final String controlId;
        final String htmlType;

        Control(String controlId, String htmlType) {
            this.controlId = controlId;
            this.htmlType = htmlType;
        }
    }
}
