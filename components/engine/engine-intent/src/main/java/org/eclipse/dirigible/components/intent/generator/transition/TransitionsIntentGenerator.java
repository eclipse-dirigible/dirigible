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
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
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
 * record's id to it and toasts the result. A transition declaring a {@code notify:} block
 * additionally carries {@code notifies}, which tells that store the response is {@code { record,
 * notify }} and its delivery outcome is worth a warning.
 *
 * <p>
 * The descriptor also mirrors the {@code from:} guard ({@code statusProperty} + {@code from}) so
 * the button can be hidden on a record the transition cannot apply to (dirigible #7073) - Void
 * offered on a paid invoice and answered with a 409 is the failure that asked for it. The server's
 * check stays authoritative; this only keeps an impossible button off the screen.
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
            context.writeModelFile(fileBase + ".js", buildDescriptorModule(project, IntentNaming.javaModule(context), t,
                    IntentNaming.customActionTranslationKey(project, context, name), statusProperty(model, t.getForEntity())));
        }
    }

    private static String buildExtensionJson(String project, String modulePath, TransitionIntent t) {
        Map<String, Object> extension = new LinkedHashMap<>();
        extension.put("module", modulePath);
        extension.put("extensionPoint", project + "-custom-action");
        extension.put("description", "Transition [" + t.getName() + "] on [" + t.getForEntity() + "]");
        return JsonHelper.toJson(extension);
    }

    /**
     * The PascalCase name of the entity's {@code function: EntityStatus} relation - the property the
     * record carries its current status in, and the one the {@code from:} check reads on the client.
     * Empty when the entity declares none (the parser already reported that; the button then simply
     * stays unguarded and the server's 409 remains the only gate).
     */
    private static String statusProperty(IntentModel model, String entityName) {
        for (EntityIntent entity : model.getEntities()) {
            if (!entity.getName()
                       .equals(entityName)) {
                continue;
            }
            for (RelationIntent relation : entity.getRelations()) {
                if (relation.isEntityStatus()) {
                    return IntentNaming.pascalCase(relation.getName());
                }
            }
        }
        return "";
    }

    private static String buildDescriptorModule(String project, String javaModule, TransitionIntent t, String translationKey,
            String statusProperty) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", project + "-" + t.getForEntity() + "-" + t.getName());
        String label = IntentNaming.customActionLabel(t.getName(), t.getLabel());
        view.put("label", label);
        // The renderer shows T(translation.key, label) - untranslated apps keep the raw label.
        view.put("translation", Map.of("key", translationKey));
        // The server controller (server half) is served under gen/events; NOT the entity api base.
        view.put("endpoint", "/services/java/" + project + "/gen/events/" + javaModule + "/" + IntentNaming.pascalIdentifier(t.getName())
                + "Transition/run");
        view.put("view", t.getForEntity());
        view.put("type", "entity");
        // A status flip is not a create: the shell words its toast from `kind` rather than reporting
        // "Created <ref>" for a record that already existed (dirigible #7073).
        view.put("kind", "transition");
        // The `from:` guard, mirrored onto the client (dirigible #7073). The server's 409 stays
        // authoritative - this is what lets the button be HIDDEN on a record the transition can
        // never apply to, instead of offering Void on a paid invoice and answering the click with a
        // refusal. A `when:` guard is deliberately NOT mirrored: it is a Calc expression over the
        // stored record, and a client re-implementation of it would drift from the server's.
        List<Integer> from = t.getFrom();
        if (statusProperty != null && !statusProperty.isBlank() && from != null && !from.isEmpty()) {
            view.put("statusProperty", statusProperty);
            view.put("from", from);
        }
        if (t.getNotify() != null) {
            // A transition that mails answers { record, notify } instead of the bare record (dirigible
            // #7023), so the shell reports a failed delivery as a warning instead of a green toast.
            // Declared here rather than sniffed from the response: which shape an endpoint returns is
            // decided when it is generated, and a client guessing from the payload is how a body change
            // turns into a silently wrong toast.
            view.put("notifies", true);
        }
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
