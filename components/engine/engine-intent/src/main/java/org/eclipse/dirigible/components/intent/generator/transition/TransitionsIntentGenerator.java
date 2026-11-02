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
 * {@code <name>-transition-action.json} (the action descriptor). Like a
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
 * A {@code personal: true} transition (#8406) contributes a second pair,
 * {@code <name>-transition-my-action}, whose descriptor carries {@code surface: "my"} and the
 * endpoint of the personal run ({@code <ClassName>TransitionMy/run}): the personal pages offer it
 * on the owner's own records, the power pages never list it.
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
            String translationKey = IntentNaming.customActionTranslationKey(project, context, name);
            String statusProperty = statusProperty(model, t.getForEntity());
            writeAction(context, project, name + "-transition-action",
                    buildDescriptorModule(project, IntentNaming.javaModule(context), t, translationKey, statusProperty, false),
                    "Transition [" + name + "] on [" + t.getForEntity() + "]");
            if (t.isPersonal()) {
                // The owner's run from the personal surface (#8406): a descriptor of its own, because
                // its endpoint is another controller - one scoped to the caller's own records - and a
                // page offers only the actions whose endpoint answers it.
                writeAction(context, project, name + "-transition-my-action",
                        buildDescriptorModule(project, IntentNaming.javaModule(context), t, translationKey, statusProperty, true),
                        "Transition [" + name + "] on [" + t.getForEntity() + "], personal surface");
            }
        }
    }

    private static void writeAction(IntentGenerationContext context, String project, String fileBase, String descriptor,
            String description) {
        Map<String, Object> extension = new LinkedHashMap<>();
        extension.put("module", project + "/" + fileBase + ".json");
        extension.put("extensionPoint", project + "-custom-action");
        extension.put("description", description);
        context.writeModelFile(fileBase + ".extension", JsonHelper.toJson(extension));
        context.writeModelFile(fileBase + ".json", descriptor);
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
            String statusProperty, boolean personal) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", project + "-" + t.getForEntity() + "-" + t.getName() + (personal ? "-my" : ""));
        String label = IntentNaming.customActionLabel(t.getName(), t.getLabel());
        view.put("label", label);
        // The renderer shows T(translation.key, label) - untranslated apps keep the raw label.
        view.put("translation", Map.of("key", translationKey));
        // The server controller (server half) is served under gen/events; NOT the entity api base.
        view.put("endpoint", "/services/java/" + project + "/gen/events/" + javaModule + "/" + IntentNaming.pascalIdentifier(t.getName())
                + (personal ? "TransitionMy/run" : "Transition/run"));
        view.put("view", t.getForEntity());
        if (personal) {
            // The surface the action is offered on (#8406): the personal pages ask for it, and the power
            // pages - which ask for none - never list it.
            view.put("surface", "my");
        }
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
        if (t.isPrimary()) {
            // The record's filled button when it applies (#8119) - emitted only when declared, so a
            // descriptor without it is byte-identical to before.
            view.put("primary", true);
        }
        if (t.isDestructive()) {
            // A red run button and a place in the destructive group of the record's menus (#8364) -
            // emitted only when declared, so a descriptor without it is byte-identical to before.
            view.put("destructive", true);
        }
        // A JSON extension module: the view object itself, read by the views aggregation without a script
        // engine.
        return JsonHelper.toJson(view);
    }
}
