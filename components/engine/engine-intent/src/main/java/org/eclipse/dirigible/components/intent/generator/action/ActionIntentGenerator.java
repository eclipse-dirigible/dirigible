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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.model.ActionIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Materializes each {@code actions} entry into a contribution to the app's
 * {@code <project>-custom-action} extension point - one {@code <name>-action.extension}
 * (registering the point + module) plus one {@code <name>-action.js} (a module exporting
 * {@code getView()} with the action descriptor). The generated Harmonia views load that point
 * through the shared {@code customActions} store (see the Harmonia UI templates) and render the
 * button on the entity named by {@code forEntity}, opening the declared {@code page} in the
 * app-wide dialog. Declaring an action in intent therefore needs no hand-written
 * {@code .extension}/{@code .js}; external projects may still contribute to the same point.
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
            String modulePath = project + "/" + fileBase + ".js";
            context.writeModelFile(fileBase + ".extension", buildExtensionJson(project, modulePath, action));
            context.writeModelFile(fileBase + ".js", buildDescriptorModule(project, action));
        }
    }

    private static String buildExtensionJson(String project, String modulePath, ActionIntent action) {
        Map<String, Object> extension = new LinkedHashMap<>();
        extension.put("module", modulePath);
        extension.put("extensionPoint", project + "-custom-action");
        extension.put("description", "Custom action [" + action.getName() + "] on [" + action.getForEntity() + "]");
        return JsonHelper.toJson(extension);
    }

    private static String buildDescriptorModule(String project, ActionIntent action) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", project + "-" + action.getForEntity() + "-" + action.getName());
        String label = action.getLabel() == null || action.getLabel()
                                                          .isBlank() ? IntentNaming.humanize(action.getName()) : action.getLabel();
        view.put("label", label);
        view.put("path", action.getPage());
        view.put("view", action.getForEntity());
        view.put("type", action.getScope());
        if (action.getIcon() != null && !action.getIcon()
                                               .isBlank()) {
            view.put("icon", action.getIcon());
        }
        if (action.getOrder() != null) {
            view.put("order", action.getOrder());
        }
        // A CommonJS module exporting getView() - the shape the extension-services endpoint loads.
        return "const viewData = " + JsonHelper.toJson(view) + ";\n" + "if (typeof exports !== 'undefined') {\n"
                + "    exports.getView = () => viewData;\n" + "}\n";
    }
}
