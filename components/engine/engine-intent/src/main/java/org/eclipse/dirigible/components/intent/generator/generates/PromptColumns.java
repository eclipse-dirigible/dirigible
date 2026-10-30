/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.generates;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.IntentEntities;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.edm.CrossModelSupport;
import org.eclipse.dirigible.components.intent.model.ActionPromptIntent;
import org.eclipse.dirigible.components.intent.model.ActionPromptOptionIntent;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.GeneratesIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.PromptFieldIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.UsesIntent;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;

/**
 * The controls of a prompt dialog whose target is NOT a detail of the view the button lives on
 * (issue #7748) - a standalone entity, or one owned by another model. Such a target has no detail
 * registration for the shared {@code customActions} store to type the dialog from, so the action
 * descriptor carries each prompted control itself: its widget, its caption and, for a to-one, the
 * controller its options are read from, its key and its label property.
 *
 * <p>
 * A {@code call:} action (issue #8090) has no target at all, so its controls are declared inline
 * and emitted in the same shape through {@link #declared}.
 */
public final class PromptColumns {

    private PromptColumns() {}

    /**
     * The prompted controls, or {@code null} when the target is a composition child of the button's
     * view - that target's detail registration types the dialog, its {@code dependsOn} cascade
     * included.
     *
     * @param g the create-from
     * @param model the model it is declared in
     * @param context the generation context, for the REST paths and a cross-model target's owner model
     * @return the controls, or null
     */
    static List<Map<String, Object>> of(GeneratesIntent g, IntentModel model, IntentGenerationContext context) {
        if (!g.hasPrompt()) {
            return null;
        }
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        boolean crossModel = g.getUses() != null && !g.getUses()
                                                      .isBlank();
        EntityIntent target = crossModel ? null : byName.get(g.getTo());
        if (target != null && isDetailOf(target, g.getForEntity())) {
            return null;
        }
        List<Map<String, Object>> columns = new ArrayList<>();
        if (target != null) {
            for (PromptFieldIntent p : g.getPrompt()) {
                columns.add(localColumn(p, target, model, context));
            }
            return columns;
        }
        UsesIntent uses = uses(model, g.getUses());
        CrossModelSupport.TargetInfo info = CrossModelSupport.resolve(context, uses, g.getTo());
        for (PromptFieldIntent p : g.getPrompt()) {
            columns.add(crossModelColumn(p, info, uses, context));
        }
        return columns;
    }

    /**
     * A control declared inline with its own type - a {@code call:} action's prompt input. A
     * {@code multiselect} is the {@code MULTISELECT} widget carrying its {@code options}; the
     * {@code help} line and the {@code default} the dialog opens with are carried when declared (issue
     * #8296), so an input without them is emitted as before.
     *
     * @param input the declared input
     * @return the control, in the shape the shell's prompt dialog renders
     */
    public static Map<String, Object> declared(ActionPromptIntent input) {
        String widget = input.isMultiselect() ? "MULTISELECT" : widgetOf(input.getType());
        Map<String, Object> column = column(input.getName(), caption(input.getLabel(), input.getName()), widget, null);
        if (input.isMultiselect()) {
            List<Map<String, Object>> options = new ArrayList<>();
            for (ActionPromptOptionIntent declared : input.getOptions()) {
                Map<String, Object> option = new LinkedHashMap<>();
                option.put("value", declared.getValue());
                option.put("label", declared.caption());
                if (declared.getHelp() != null && !declared.getHelp()
                                                           .isBlank()) {
                    option.put("help", declared.getHelp());
                }
                options.add(option);
            }
            column.put("options", options);
        }
        if (input.getHelp() != null && !input.getHelp()
                                             .isBlank()) {
            column.put("help", input.getHelp());
        }
        if (input.getDefaultValue() != null) {
            column.put("default", input.getDefaultValue());
        }
        return column;
    }

    /**
     * The lookup a to-one relation's display text is read through - the controller of its target, its
     * key and its label property.
     *
     * @param relation the to-one relation of an entity of the model
     * @param model the model the entity is declared in
     * @param context the generation context, for the REST paths and a cross-model target's owner model
     * @return the lookup, in the shape the shell's dialogs read
     */
    public static Map<String, Object> toOneLookup(RelationIntent relation, IntentModel model, IntentGenerationContext context) {
        if (relation.isCrossModel()) {
            UsesIntent uses = CrossModelSupport.owningModel(context, uses(model, relation.getModel()), relation.getTo());
            return crossModelLookup(context, uses, relation.getTo());
        }
        EntityIntent related = IntentEntities.byName(model)
                                             .get(relation.getTo());
        String perspective = IntentEntities.resolvePerspective(relation.getTo(), IntentEntities.compositionParents(model), model);
        return lookup(apiBase(context.getProjectName(), IntentNaming.javaModule(context)), perspective, relation.getTo(),
                IntentEntities.keyFieldName(related), IntentEntities.labelFieldOf(related));
    }

    private static boolean isDetailOf(EntityIntent target, String forEntity) {
        for (RelationIntent relation : target.getRelations()) {
            if (relation.isComposition() && forEntity != null && forEntity.equals(relation.getTo())
                    && ("manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind()))) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> localColumn(PromptFieldIntent p, EntityIntent target, IntentModel model,
            IntentGenerationContext context) {
        String name = IntentNaming.pascalCase(p.getField());
        for (FieldIntent field : target.getFields()) {
            if (p.getField()
                 .equals(field.getName())) {
                return column(name, caption(field.getLabel(), p.getField()), widgetOf(field.getType()), null);
            }
        }
        RelationIntent relation = relation(target, p.getField());
        return column(name, caption(relation.getLabel(), p.getField()), "DROPDOWN", toOneLookup(relation, model, context));
    }

    private static Map<String, Object> crossModelColumn(PromptFieldIntent p, CrossModelSupport.TargetInfo info, UsesIntent uses,
            IntentGenerationContext context) {
        String name = IntentNaming.pascalCase(p.getField());
        String caption = IntentNaming.humanize(p.getField());
        String related = info.propertyRelations() == null ? null
                : info.propertyRelations()
                      .get(name);
        if (related != null) {
            UsesIntent owner = CrossModelSupport.owningModel(context, uses, related);
            return column(name, caption, "DROPDOWN", crossModelLookup(context, owner, related));
        }
        String dataType = info.propertyTypes() == null ? null
                : info.propertyTypes()
                      .get(name);
        if (dataType == null) {
            // promptFields (the glue generator, which runs first) refuses an undeclared property with the
            // reason; this is only reached by an owner model that cannot be read at all.
            throw new IntentValidationException(
                    List.of("prompt field [" + p.getField() + "] is not a property of the target in model [" + uses.getModel() + "]"));
        }
        return column(name, caption, widgetOfJdbcType(dataType), null);
    }

    private static Map<String, Object> crossModelLookup(IntentGenerationContext context, UsesIntent uses, String entity) {
        CrossModelSupport.TargetInfo info = CrossModelSupport.resolve(context, uses, entity);
        return lookup(apiBase(uses.resolveProject(), IntentNaming.javaIdentifier(uses.getModel())), info.perspectiveName(), entity,
                info.keyField(), info.labelField());
    }

    private static Map<String, Object> column(String name, String label, String widget, Map<String, Object> lookup) {
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("name", name);
        column.put("label", label);
        column.put("widget", widget);
        if (lookup != null) {
            column.put("lookup", lookup);
        }
        return column;
    }

    private static Map<String, Object> lookup(String apiBase, String perspective, String entity, String key, String text) {
        Map<String, Object> lookup = new LinkedHashMap<>();
        lookup.put("url", apiBase + "/" + IntentNaming.javaIdentifier(perspective) + "/" + entity + "Controller");
        lookup.put("key", key);
        lookup.put("text", text == null || text.isBlank() ? key : text);
        return lookup;
    }

    /** The REST base the generated controllers of a model are served under. */
    private static String apiBase(String project, String javaModule) {
        return "/services/java/" + project + "/gen/" + javaModule + "/api";
    }

    private static String caption(String label, String field) {
        return label != null && !label.isBlank() ? label : IntentNaming.humanize(field);
    }

    private static String widgetOf(String type) {
        return switch (type == null ? "string" : type) {
            case "integer", "int", "long", "decimal", "double" -> "NUMBER";
            case "boolean" -> "CHECKBOX";
            case "date" -> "DATE";
            default -> "TEXT";
        };
    }

    private static String widgetOfJdbcType(String dataType) {
        return switch (dataType) {
            case "INTEGER", "SMALLINT", "TINYINT", "BIGINT", "DECIMAL", "NUMERIC", "DOUBLE", "REAL", "FLOAT" -> "NUMBER";
            case "BOOLEAN", "BIT" -> "CHECKBOX";
            case "DATE" -> "DATE";
            default -> "TEXT";
        };
    }

    private static RelationIntent relation(EntityIntent target, String name) {
        for (RelationIntent relation : target.getRelations()) {
            if (name.equals(relation.getName())) {
                return relation;
            }
        }
        // The parser refuses a prompt naming neither a field nor a to-one relation of a local target.
        throw new IllegalStateException("prompt field [" + name + "] is not a property of [" + target.getName() + "]");
    }

    private static UsesIntent uses(IntentModel model, String alias) {
        for (UsesIntent uses : model.getUses()) {
            if (alias.equals(uses.getModel())) {
                return uses;
            }
        }
        // The parser refuses an undeclared alias.
        throw new IllegalStateException("model alias [" + alias + "] is not declared under uses:");
    }
}
