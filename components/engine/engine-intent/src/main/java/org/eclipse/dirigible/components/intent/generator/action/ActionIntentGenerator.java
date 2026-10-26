/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.action;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.intent.generator.GeneratesGuardSupport;
import org.eclipse.dirigible.components.intent.generator.IntentEntities;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.generator.PermissionSupport;
import org.eclipse.dirigible.components.intent.generator.generates.PromptColumns;
import org.eclipse.dirigible.components.intent.model.ActionCallIntent;
import org.eclipse.dirigible.components.intent.model.ActionIntent;
import org.eclipse.dirigible.components.intent.model.ActionPromptIntent;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.LifecycleStages;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Materializes each {@code actions} entry into a contribution to the app's
 * {@code <project>-custom-action} extension point - one {@code <name>-action.extension}
 * (registering the point + module) plus one {@code <name>-action.json} (a JSON extension module
 * holding the action descriptor). The generated Harmonia views load that point through the shared
 * {@code customActions} store (see the Harmonia UI templates) and render the button on the entity
 * named by {@code forEntity}, opening the declared {@code page} in the app-wide dialog. Declaring
 * an action in intent therefore needs no hand-written {@code .extension}/{@code .json}; external
 * projects may still contribute to the same point.
 *
 * <p>
 * A {@code call:} action (issue #8090) carries the request instead of a {@code path}, together with
 * its dialog ({@code prompt} + {@code promptColumns}, or the {@code confirm} text), the advisory
 * from-status {@code guard}, the {@code roles} offered the button and the {@code success} toast.
 * The store sends the request to the application's own endpoint; nothing generated sits in between.
 *
 * <p>
 * Idempotent: identical input always produces byte-identical output. The {@code .extension} files
 * are intent-owned (see
 * {@link org.eclipse.dirigible.components.intent.generator.IntentGenerationService}), so an action
 * removed from the intent is scrubbed on the next Generate.
 */
@Component
@Order(450)
public class ActionIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionIntentGenerator.class);

    @Override
    public String name() {
        return "action";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        if (model.getActions()
                 .isEmpty()) {
            return;
        }
        String project = context.getProjectName();
        for (ActionIntent action : model.getActions()) {
            String name = action.getName();
            if (name == null || name.isBlank()) {
                LOGGER.warn("Skipping action with no name");
                continue;
            }
            String fileBase = name + "-action";
            String modulePath = project + "/" + fileBase + ".json";
            context.writeModelFile(fileBase + ".extension", buildExtensionJson(project, modulePath, action));
            context.writeModelFile(fileBase + ".json", buildDescriptorModule(project, action, model));
        }
    }

    private static String buildExtensionJson(String project, String modulePath, ActionIntent action) {
        Map<String, Object> extension = new LinkedHashMap<>();
        extension.put("module", modulePath);
        extension.put("extensionPoint", project + "-custom-action");
        extension.put("description", "Custom action [" + action.getName() + "] on [" + action.getForEntity() + "]");
        return JsonHelper.toJson(extension);
    }

    private static String buildDescriptorModule(String project, ActionIntent action, IntentModel model) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", project + "-" + action.getForEntity() + "-" + action.getName());
        String label = action.getLabel() == null || action.getLabel()
                                                          .isBlank() ? IntentNaming.humanize(action.getName()) : action.getLabel();
        view.put("label", label);
        if (action.getPage() != null) {
            view.put("path", action.getPage());
        }
        view.put("view", action.getForEntity());
        view.put("type", action.getScope());
        if (action.getIcon() != null && !action.getIcon()
                                               .isBlank()) {
            view.put("icon", action.getIcon());
        }
        if (action.getOrder() != null) {
            view.put("order", action.getOrder());
        }
        if (action.isPrimary()) {
            // The record's filled button when it applies (#8119) - emitted only when declared, so a
            // descriptor without it is byte-identical to before.
            view.put("primary", true);
        }
        if (action.getCall() != null) {
            putCall(view, action, model);
        }
        // A JSON extension module: the view object itself, read by the views aggregation without a script
        // engine.
        return JsonHelper.toJson(view);
    }

    private static void putCall(Map<String, Object> view, ActionIntent action, IntentModel model) {
        ActionCallIntent call = action.getCall();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("method", call.getMethod());
        request.put("path", call.getPath());
        if (call.getBody() != null) {
            request.put("body", call.getBody());
        }
        view.put("call", request);
        if (!action.getPrompt()
                   .isEmpty()) {
            List<Map<String, Object>> prompt = new ArrayList<>();
            List<Map<String, Object>> columns = new ArrayList<>();
            for (ActionPromptIntent input : action.getPrompt()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("name", input.getName());
                entry.put("required", input.isRequired());
                prompt.add(entry);
                columns.add(PromptColumns.declared(input.getName(), input.getLabel(), input.getType()));
            }
            view.put("prompt", prompt);
            view.put("promptColumns", columns);
        }
        if (action.getConfirm() != null && !action.getConfirm()
                                                  .isBlank()) {
            view.put("confirm", action.getConfirm());
        }
        if (!action.getFromStatus()
                   .isEmpty()) {
            // Advisory only: the endpoint is the application's, so its refusal stays the contract and the
            // platform adds no server check - the guard only stops offering a click it would refuse.
            view.put("guard", new GeneratesGuardSupport.Guard(statusProperty(action, model), List.copyOf(action.getFromStatus()),
                    List.of()).descriptor());
        }
        if (action.getCan() != null && !action.getCan()
                                              .isBlank()) {
            // The views aggregation drops the descriptor for a caller holding none of these roles.
            view.put("roles", PermissionSupport.rolesListing(model, action.getCan()));
        }
        if (action.getSuccess() != null && !action.getSuccess()
                                                  .isBlank()) {
            view.put("success", action.getSuccess());
        }
    }

    /**
     * The {@code forEntity}'s status FK, PascalCase - the parser refuses a fromStatus on an entity
     * without one.
     */
    private static String statusProperty(ActionIntent action, IntentModel model) {
        EntityIntent entity = IntentEntities.byName(model)
                                            .get(action.getForEntity());
        RelationIntent status = LifecycleStages.statusRelation(entity);
        return IntentNaming.pascalCase(status.getName());
    }
}
