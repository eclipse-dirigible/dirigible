/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.transition;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.TransitionIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Materializes the <b>client half</b> of each {@code transitions} declaration (the guarded
 * on-demand status flip): a contribution to the app's {@code <project>-custom-action} extension
 * point - one {@code <name>-transition-action.extension} plus one
 * {@code <name>-transition-action.js} (the action descriptor). Like a
 * {@link org.eclipse.dirigible.components.intent.model.GeneratesIntent generates} action the
 * descriptor carries an {@code endpoint}: the shared {@code customActions} store POSTs the selected
 * record's id to it and toasts the result.
 *
 * <p>
 * The endpoint is the REST {@code @Controller} generated (server half) from the {@code .glue}
 * file's {@code transitions} collection by the {@code template-application-events-java} template,
 * served under {@code /services/java/<project>/gen/events/<module>/<ClassName>Transition/run}. A
 * transition is always per-record ({@code type: entity}) - a whole-view status flip has no meaning.
 *
 * <p>
 * Idempotent: identical input always produces byte-identical output. The {@code .extension} files
 * are intent-owned, so a transition removed from the intent is scrubbed on the next Generate.
 */
@Component
@Order(470)
public class TransitionsIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransitionsIntentGenerator.class);

    @Override
    public String name() {
        return "transitions";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        if (model.getTransitions()
                 .isEmpty()) {
            return;
        }
        String project = context.getProjectName();
        for (TransitionIntent t : model.getTransitions()) {
            String name = t.getName();
            if (name == null || name.isBlank()) {
                LOGGER.warn("Skipping transition with no name");
                continue;
            }
            String fileBase = name + "-transition-action";
            String modulePath = project + "/" + fileBase + ".js";
            context.writeModelFile(fileBase + ".extension", buildExtensionJson(project, modulePath, t));
            context.writeModelFile(fileBase + ".js", buildDescriptorModule(project, IntentNaming.javaModule(context), t));
        }
    }

    private static String buildExtensionJson(String project, String modulePath, TransitionIntent t) {
        Map<String, Object> extension = new LinkedHashMap<>();
        extension.put("module", modulePath);
        extension.put("extensionPoint", project + "-custom-action");
        extension.put("description", "Transition [" + t.getName() + "] on [" + t.getForEntity() + "]");
        return JsonHelper.toJson(extension);
    }

    private static String buildDescriptorModule(String project, String javaModule, TransitionIntent t) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", project + "-" + t.getForEntity() + "-" + t.getName());
        String label = t.getLabel() == null || t.getLabel()
                                                .isBlank() ? IntentNaming.humanize(t.getName()) : t.getLabel();
        view.put("label", label);
        // The server controller (server half) is served under gen/events; NOT the entity api base.
        view.put("endpoint", "/services/java/" + project + "/gen/events/" + javaModule + "/" + IntentNaming.pascalIdentifier(t.getName())
                + "Transition/run");
        view.put("view", t.getForEntity());
        view.put("type", "entity");
        if (t.getIcon() != null && !t.getIcon()
                                     .isBlank()) {
            view.put("icon", t.getIcon());
        }
        if (t.getOrder() != null) {
            view.put("order", t.getOrder());
        }
        // A CommonJS module exporting getView() - the shape the extension-services endpoint loads.
        return "const viewData = " + JsonHelper.toJson(view) + ";\n" + "if (typeof exports !== 'undefined') {\n"
                + "    exports.getView = () => viewData;\n" + "}\n";
    }
}
