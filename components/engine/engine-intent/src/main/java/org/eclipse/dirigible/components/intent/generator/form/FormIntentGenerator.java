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
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.FormIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;
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
 * A field written as a {@code relation.field} path (e.g. {@code book.price} on a form bound to
 * {@code Loan}) is a one-hop to-one relation of the bound entity. Such a control binds to the
 * {@code <relation>_<field>} process variable ({@code book_price}) that the process's resolver step
 * publishes - the form model of a BPM task form is the process variables, which hold the FK id but
 * not the related entity's own fields - and is rendered {@code readonly}, typed from the target
 * entity's field. See
 * {@link org.eclipse.dirigible.components.intent.generator.ProcessResolverSupport}.
 *
 * <p>
 * When the form backs a BPM user task it is rendered <b>read-only by default</b> (each control is
 * marked {@code readonly}; the runtime shows a Label: Value card), with only the fields listed in
 * {@code editable} kept as bound inputs - those edits are written back to the entity by the
 * process's writer service task. A {@code close} action is auto-appended (a non-completing button
 * that just closes the form/dialog, like the dialog frame's X), so the task stays open in the
 * inbox. The form's other actions are the task's choices: list {@code approve}/{@code reject} when
 * a decision branches on the chosen {@code action}, or a single action (e.g. {@code issue}) when
 * the task flows on linearly with no branching.
 * <p>
 * Actions become buttons in a trailing {@code container-hbox}. The button {@code type} is inferred
 * from the action name ({@code approve} -> positive, {@code reject}/{@code decline}/{@code delete}
 * -> negative, {@code save}/{@code submit} -> emphasized, {@code close} -> transparent, anything
 * else -> standard). Each button carries a {@code callback} like {@code onApproveClicked()} wired
 * in the {@code code} block to complete the current BPM user task: the Inbox/Process perspective
 * opens the form with {@code ?taskId=&processInstanceId=}, and the handler POSTs {@code COMPLETE}
 * to {@code /services/inbox/tasks/<taskId>} (the per-task permission-checked Inbox endpoint, so a
 * candidate-group user - not only ADMINISTRATOR/DEVELOPER/OPERATOR - can complete) with the action
 * name and the form model as process variables (so a downstream gateway can branch on the action).
 * On success the handler closes its host via both {@code DialogHub.closeWindow()} and
 * {@code window.close()} - the former closes the dialog when the form is opened from an entity
 * view, the latter a standalone (script-opened) window; each is a harmless no-op where it does not
 * apply, including the Inbox's inline iframe (which clears its own pane on its refresh cycle).
 * Forms opened outside a task report the missing {@code taskId} instead of failing silently.
 * Business logic beyond completing the task belongs in a hand-written form override under
 * {@code custom/}.
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
        // A form referenced by a userTask is a BPM task form: read-only by default (it presents the
        // entity for an Approve/Reject decision), with only the explicitly listed `editable` fields
        // bound. Forms not used by a user task (e.g. an inbound create form) keep editable controls.
        Set<String> taskFormNames = taskFormNames(model);
        Set<String> seenFiles = new HashSet<>();
        for (FormIntent form : model.getForms()) {
            if (form.getName() == null || form.getName()
                                              .isBlank()) {
                LOGGER.warn("Skipping unnamed form in intent [{}]", IntentNaming.baseName(context));
                continue;
            }
            String fileName = form.getName() + ".form";
            if (!seenFiles.add(fileName)) {
                LOGGER.warn("Duplicate form [{}] in intent [{}] - keeping the first occurrence", form.getName(),
                        IntentNaming.baseName(context));
                continue;
            }
            if (!context.getSettings()
                        .shouldGenerate("forms", form.getName())) {
                LOGGER.info("Settings opt-out: keeping existing form [{}] (not generated)", form.getName());
                continue;
            }
            EntityIntent boundEntity = form.getForEntity() == null ? null : entitiesByName.get(form.getForEntity());
            Map<String, Object> document = buildForm(form, boundEntity, entitiesByName, taskFormNames.contains(form.getName()));
            context.writeModelFile(fileName, JsonHelper.toJson(document));
        }
    }

    /** Names of forms referenced by a {@code userTask} step in any process (the BPM task forms). */
    private static Set<String> taskFormNames(IntentModel model) {
        Set<String> names = new HashSet<>();
        for (ProcessIntent process : model.getProcesses()) {
            for (StepIntent step : process.getSteps()) {
                if ("userTask".equals(step.getKind()) && step.getArgs() != null) {
                    Object form = step.getArgs()
                                      .get("form");
                    if (form != null) {
                        names.add(form.toString());
                    }
                }
            }
        }
        return names;
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

    private static Map<String, Object> buildForm(FormIntent form, EntityIntent entity, Map<String, EntityIntent> entitiesByName,
            boolean isTaskForm) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("metadata", buildMetadata(form, isTaskForm));
        document.put("feeds", new ArrayList<>());
        document.put("scripts", new ArrayList<>());
        document.put("code", buildCode(form, isTaskForm));
        document.put("form", buildControls(form, entity, entitiesByName, isTaskForm));
        return document;
    }

    /**
     * Form-builder metadata. For a BPM task form it records {@code taskForm: true} (so the runtime
     * renders the controls as a read-only Label: Value card and surfaces the change-warning) plus the
     * bound entity's logical name and the editable-field set. Only logical model names are emitted -
     * never a template-engine output path, which the intent layer must stay agnostic about.
     */
    private static Map<String, Object> buildMetadata(FormIntent form, boolean isTaskForm) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (isTaskForm) {
            metadata.put("taskForm", true);
            if (form.getForEntity() != null) {
                metadata.put("entity", form.getForEntity());
            }
            metadata.put("editable", new ArrayList<>(form.getEditable()));
        }
        return metadata;
    }

    /**
     * The form's actions, with a {@code close} action appended for a BPM task form when the author did
     * not declare one. {@code close} is a non-completing action - it just closes the form/dialog (the
     * same as the dialog frame's X), leaving the task open in the inbox - so every task form has an
     * explicit "leave without deciding" button next to its Approve/Reject. Other forms are unchanged.
     */
    private static List<String> effectiveActions(FormIntent form, boolean isTaskForm) {
        List<String> actions = new ArrayList<>();
        for (String action : form.getActions()) {
            if (action != null && !action.isBlank()) {
                actions.add(action);
            }
        }
        if (isTaskForm && actions.stream()
                                 .noneMatch(FormIntentGenerator::isCloseAction)) {
            actions.add("close");
        }
        return actions;
    }

    /**
     * {@code close} is the non-completing "just close the form" action (mirrors the dialog frame X).
     */
    private static boolean isCloseAction(String action) {
        return "close".equalsIgnoreCase(action);
    }

    private static String buildCode(FormIntent form, boolean isTaskForm) {
        List<String> actions = effectiveActions(form, isTaskForm);
        if (actions.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        // Process user-task form: the Inbox/Process perspective opens it with
        // ?taskId=&processInstanceId=. Each action completes the current task via the platform BPM API,
        // passing the action name plus the form model as process variables so a downstream gateway can
        // branch on them. A form opened outside a task (no taskId) reports it instead of failing silently.
        sb.append(
                """
                        const __taskParams = new URLSearchParams(window.location.search);
                        const __taskId = __taskParams.get('taskId');
                        const __notifications = new NotificationHub();
                        const __dialogs = new DialogHub();

                        function __completeTask(action) {
                            if (!__taskId) {
                                __notifications.show({ type: 'negative', title: 'Cannot submit', description: 'This form was not opened from a task (no taskId).' });
                                return;
                            }
                            $http.post('/services/inbox/tasks/' + __taskId, {
                                action: 'COMPLETE',
                                data: Object.assign({ action: action }, $scope.model || {})
                            }).then(() => {
                                __notifications.show({ type: 'positive', title: 'Task submitted', description: 'The task was completed (' + action + ').' });
                                __dialogs.closeWindow();
                                window.close();
                            }).catch((error) => {
                                const message = error && error.data && error.data.message ? error.data.message : 'Unknown error';
                                __notifications.show({ type: 'negative', title: 'Submit failed', description: message });
                            });
                        }

                        """);
        for (String action : actions) {
            if (isCloseAction(action)) {
                // Close does NOT complete the task: it just closes the dialog/window (same as the X), so
                // the task stays open in the inbox. closeWindow() covers the dialog/inbox iframe host;
                // window.close() covers a standalone window. Each is a harmless no-op where it doesn't apply.
                sb.append("$scope.on")
                  .append(pascalCase(action))
                  .append("Clicked = function () { __dialogs.closeWindow(); window.close(); };\n");
            } else {
                sb.append("$scope.on")
                  .append(pascalCase(action))
                  .append("Clicked = function () { __completeTask('")
                  .append(action)
                  .append("'); };\n");
            }
        }
        return sb.toString();
    }

    private static List<Map<String, Object>> buildControls(FormIntent form, EntityIntent entity, Map<String, EntityIntent> entitiesByName,
            boolean isTaskForm) {
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
        Set<String> editable = new HashSet<>(form.getEditable());
        for (String fieldName : form.getFields()) {
            if (fieldName == null || fieldName.isBlank()) {
                continue;
            }
            if (fieldName.indexOf('.') > 0) {
                // A relation.field is always read-only (editing it would not write back to the related
                // entity); on a task form it shows the resolved name/value variable.
                controls.add(relationFieldControl(fieldName, entity, entitiesByName));
            } else {
                // Task-form fields are read-only unless explicitly opted in via `editable`; other forms
                // keep the legacy "editable except a generated PK" behavior.
                boolean readonly = isTaskForm ? !editable.contains(fieldName) : isReadonlyByDefault(fieldsByName.get(fieldName));
                controls.add(fieldControl(fieldName, fieldsByName.get(fieldName), readonly));
            }
        }
        List<String> actions = effectiveActions(form, isTaskForm);
        if (!actions.isEmpty()) {
            controls.add(actionRow(actions));
        }
        return controls;
    }

    private static boolean isReadonlyByDefault(FieldIntent field) {
        return field != null && field.isPrimaryKey() && field.isGenerated();
    }

    /**
     * A control for a {@code relation.field} form field (e.g. {@code book.price} on an approval form
     * bound to {@code Loan}). The form model of a BPM task form is the process variables, which carry
     * the {@code Book} FK id but not the book's own fields; a resolver step generated for the process
     * (see {@link org.eclipse.dirigible.components.intent.generator.ProcessResolverSupport}) loads the
     * related entity and publishes the field as the {@code <relation>_<field>} process variable
     * ({@code book_price}), so this control binds its {@code model} to that variable. The control type
     * is picked from the <em>target</em> entity's field (so {@code book.price} is a number control),
     * and it is {@code readonly} - it is contextual related data shown to the reviewer, not an editable
     * property of the bound entity (editing it would not write back to the related entity).
     */
    private static Map<String, Object> relationFieldControl(String path, EntityIntent boundEntity,
            Map<String, EntityIntent> entitiesByName) {
        int dot = path.indexOf('.');
        String relationName = path.substring(0, dot);
        String fieldName = path.substring(dot + 1);
        FieldIntent targetField = null;
        RelationIntent relation = boundEntity == null ? null : toOneRelation(boundEntity, relationName);
        if (relation != null) {
            EntityIntent target = entitiesByName.get(relation.getTo());
            if (target != null) {
                targetField = fieldOf(target, fieldName);
            }
        }
        Control control = pickControl(targetField);
        String variable = relationName + "_" + fieldName; // matches the resolver-set process variable
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("controlId", control.controlId);
        map.put("groupId", "fb-controls");
        map.put("id", IntentNaming.pascalCase(relationName) + IntentNaming.pascalCase(fieldName) + "Id");
        map.put("label", humanizePath(path));
        map.put("horizontal", false);
        map.put("isCompact", false);
        map.put("readonly", true);
        if (control.htmlType != null) {
            map.put("type", control.htmlType);
        }
        map.put("model", variable);
        map.put("required", false);
        if ("input-number".equals(control.controlId)) {
            map.put("pattern", numberPattern(targetField));
        }
        if ("input-textfield".equals(control.controlId) || "input-textarea".equals(control.controlId)) {
            map.put("minLength", 0);
            map.put("maxLength", targetField != null && targetField.getLength() != null ? targetField.getLength() : -1);
            map.put("errorMessage", "Incorrect input");
        }
        return map;
    }

    /**
     * A DecimalFormat-style display pattern for a number field, derived from its decimal scale (a
     * grouped integer part plus that many decimals): {@code decimal scale 2 -> "#,##0.00"},
     * {@code integer -> "#,##0"}. The runtime's read-only Label: Value rendering formats with it, the
     * same way the document/list views format their numbers.
     */
    private static String numberPattern(FieldIntent field) {
        String type = field == null || field.getType() == null ? "decimal"
                : field.getType()
                       .toLowerCase(Locale.ROOT);
        int decimals;
        if ("integer".equals(type) || "int".equals(type) || "long".equals(type)) {
            decimals = 0;
        } else {
            decimals = field != null && field.getScale() != null ? field.getScale() : 2;
        }
        StringBuilder pattern = new StringBuilder("#,##0");
        if (decimals > 0) {
            pattern.append('.');
            for (int i = 0; i < decimals; i++) {
                pattern.append('0');
            }
        }
        return pattern.toString();
    }

    private static RelationIntent toOneRelation(EntityIntent owner, String name) {
        for (RelationIntent relation : owner.getRelations()) {
            boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
            if (toOne && name.equals(relation.getName())) {
                return relation;
            }
        }
        return null;
    }

    private static FieldIntent fieldOf(EntityIntent entity, String name) {
        for (FieldIntent field : entity.getFields()) {
            if (name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    /**
     * Humanize each dot-separated segment of a {@code relation.field} path: {@code book.price} -> "Book
     * Price".
     */
    private static String humanizePath(String path) {
        StringBuilder out = new StringBuilder();
        for (String segment : path.split("\\.")) {
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(humanize(segment));
        }
        return out.toString();
    }

    private static Map<String, Object> headerControl(FormIntent form) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("controlId", "header");
        header.put("groupId", "fb-display");
        String label = form.getDescription() != null && !form.getDescription()
                                                             .isBlank() ? form.getDescription() : humanize(form.getName());
        header.put("label", label);
        header.put("headerSize", 2);
        header.put("level", 1);
        header.put("padding", "tiny");
        header.put("side", "bottom");
        return header;
    }

    private static Map<String, Object> fieldControl(String fieldName, FieldIntent field, boolean readonly) {
        Control control = pickControl(field);
        // The control binds to the entity property, whose name the EDM generator emits in PascalCase
        // (loanedOn -> LoanedOn); the `model` binding (and the control id) must match it so the form
        // reads/writes the right field. Use the same IntentNaming.pascalCase the EDM uses.
        String property = IntentNaming.pascalCase(fieldName);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("controlId", control.controlId);
        map.put("groupId", "fb-controls");
        map.put("id", property + "Id");
        map.put("label", humanize(fieldName));
        map.put("horizontal", false);
        map.put("isCompact", false);
        map.put("readonly", readonly);
        if (control.htmlType != null) {
            map.put("type", control.htmlType);
        }
        map.put("model", property);
        map.put("required", field != null && field.isRequired());
        if ("input-number".equals(control.controlId)) {
            map.put("pattern", numberPattern(field));
        }
        if ("input-textfield".equals(control.controlId) || "input-textarea".equals(control.controlId)) {
            map.put("minLength", 0);
            map.put("maxLength", field != null && field.getLength() != null ? field.getLength() : -1);
            map.put("errorMessage", "Incorrect input");
        }
        return map;
    }

    private static Map<String, Object> actionRow(List<String> actions) {
        List<Map<String, Object>> buttons = new ArrayList<>();
        for (String action : actions) {
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
        // Close just dismisses the form (no task completion), so it must not act as a submit button.
        button.put("isSubmit", !isCloseAction(action));
        button.put("isCompact", false);
        button.put("callback", "on" + pascalCase(action) + "Clicked()");
        return button;
    }

    private static String buttonType(String action) {
        switch (action.toLowerCase(Locale.ROOT)) {
            case "close":
                // Non-completing dismiss: a bordered (outline) secondary button, distinct from the
                // primary action - "transparent" had no border until hover.
                return "outline";
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
            default:
                // Every other completing action (issue, send, save, submit, ...) is the task's primary
                // action -> emphasized, which the runtime renders as the primary (blue) button.
                return "emphasized";
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
            case "month":
                return new Control("input-month", "month");
            case "week":
                return new Control("input-week", "week");
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
