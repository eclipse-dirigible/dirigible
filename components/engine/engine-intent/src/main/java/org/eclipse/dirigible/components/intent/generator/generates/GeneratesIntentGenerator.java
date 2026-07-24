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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.model.GeneratesIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Materializes the <b>client half</b> of each {@code generates} (create-from) action: a
 * contribution to the app's {@code <project>-custom-action} extension point - one
 * {@code <name>-generate-action.extension} plus one {@code <name>-generate-action.js} (the action
 * descriptor). Unlike a plain {@link org.eclipse.dirigible.components.intent.model.ActionIntent}
 * the descriptor carries an {@code endpoint} (not a {@code page}): the shared {@code customActions}
 * store POSTs the selected record's id to that endpoint and toasts the result rather than opening a
 * dialog.
 *
 * <p>
 * The endpoint is the REST {@code @Controller} generated (server half) from the {@code .glue}
 * file's {@code generates} collection by the {@code template-application-events-java} template,
 * served under {@code /services/java/<project>/gen/events/<module>/<ClassName>Generate/run}
 * (mirroring the inbound webhook URL layout).
 *
 * <p>
 * Idempotent: identical input always produces byte-identical output. The {@code .extension} files
 * are intent-owned (see
 * {@link org.eclipse.dirigible.components.intent.generator.IntentGenerationService}), so a
 * generates action removed from the intent is scrubbed on the next Generate.
 */
@Component
@Order(460)
public class GeneratesIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneratesIntentGenerator.class);

    @Override
    public String name() {
        return "generates";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        if (model.getGenerates()
                 .isEmpty()) {
            return;
        }
        String project = context.getProjectName();
        for (GeneratesIntent g : model.getGenerates()) {
            String name = g.getName();
            if (name == null || name.isBlank()) {
                LOGGER.warn("Skipping generates action with no name");
                continue;
            }
            String fileBase = name + "-generate-action";
            String modulePath = project + "/" + fileBase + ".js";
            context.writeModelFile(fileBase + ".extension", buildExtensionJson(project, modulePath, g));
            context.writeModelFile(fileBase + ".js", buildDescriptorModule(project, IntentNaming.javaModule(context), g));
        }
    }

    private static String buildExtensionJson(String project, String modulePath, GeneratesIntent g) {
        Map<String, Object> extension = new LinkedHashMap<>();
        extension.put("module", modulePath);
        extension.put("extensionPoint", project + "-custom-action");
        extension.put("description", "Generate action [" + g.getName() + "] on [" + g.getForEntity() + "]");
        return JsonHelper.toJson(extension);
    }

    private static String buildDescriptorModule(String project, String javaModule, GeneratesIntent g) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", project + "-" + g.getForEntity() + "-" + g.getName());
        String label = g.getLabel() == null || g.getLabel()
                                                .isBlank() ? IntentNaming.humanize(g.getName()) : g.getLabel();
        view.put("label", label);
        // The server controller (server half) is served under gen/events; NOT the entity api base.
        view.put("endpoint", "/services/java/" + project + "/gen/events/" + javaModule + "/" + IntentNaming.pascalIdentifier(g.getName())
                + "Generate/run");
        view.put("view", g.getForEntity());
        view.put("type", g.getScope());
        if (g.getIcon() != null && !g.getIcon()
                                     .isBlank()) {
            view.put("icon", g.getIcon());
        }
        if (g.getOrder() != null) {
            view.put("order", g.getOrder());
        }
        // A CommonJS module exporting getView() - the shape the extension-services endpoint loads.
        return "const viewData = " + JsonHelper.toJson(view) + ";\n" + "if (typeof exports !== 'undefined') {\n"
                + "    exports.getView = () => viewData;\n" + "}\n";
    }
}
