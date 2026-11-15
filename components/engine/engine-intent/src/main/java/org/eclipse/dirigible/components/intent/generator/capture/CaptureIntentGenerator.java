/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.capture;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.intent.generator.CaptureSupport;
import org.eclipse.dirigible.components.intent.generator.IntentEntities;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.model.CaptureIntent;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Emits the client half of a {@code captures:} entry: the two buttons that reach the generated
 * {@code <Name>Capture} controller from the captured entity's views, contributed through the
 * {@code <project>-custom-action} extension point the Harmonia {@code customActions} store renders.
 *
 * <ul>
 * <li><b>the capture action</b> ({@code <name>-capture-action}) - a toolbar button on the entity's
 * list. It opens the shared capture page in the app-wide dialog, where the user picks or
 * photographs the document, uploads it and is shown what the capture flagged for review.</li>
 * <li><b>the review action</b> ({@code <name>-capture-review-action}) - a per-record button that
 * opens the same page on an existing record: the kept file beside the flags of its capture.</li>
 * </ul>
 *
 * Both are {@code path} actions on one platform page ({@value #PAGE}), parameterized by the query
 * string, so a capture ships no page of its own. The page is told the capture controller, the
 * attachment controller the kept file is read from (when the capture declares {@code attachment:})
 * and the attachment's foreign key to the record.
 *
 * <p>
 * Idempotent: identical input always produces byte-identical output. The {@code .extension} files
 * are intent-owned, so a capture removed from the intent is scrubbed on the next Generate.
 */
@Component
@Order(470)
public class CaptureIntentGenerator implements IntentTargetGenerator {

    /** The platform page both actions open. */
    static final String PAGE = "/services/web/application-core/shell/views/capture.html";

    /** The suffix that names a capture's review action, in its file and its catalog key. */
    public static final String REVIEW_SUFFIX = "Review";

    @Override
    public String name() {
        return "capture";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        if (model.getCaptures()
                 .isEmpty()) {
            return;
        }
        String project = context.getProjectName();
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        Map<String, String> compositionParents = IntentEntities.compositionParents(model);
        for (CaptureIntent capture : model.getCaptures()) {
            String name = capture.getName();
            EntityIntent into = capture.getInto() == null ? null : byName.get(capture.getInto());
            if (name == null || name.isBlank() || into == null) {
                continue;
            }
            String page = page(project, capture, byName, compositionParents, model, context);
            write(context, project, name + "-capture-action",
                    descriptor(project, capture, name, CaptureSupport.label(capture), page, "page", "sap-icon--camera", context),
                    "Capture action [" + name + "] on [" + into.getName() + "]");
            String review = name + REVIEW_SUFFIX;
            write(context, project, name + "-capture-review-action", descriptor(project, capture, review,
                    CaptureSupport.reviewLabel(capture), page + "&mode=review", "entity", "sap-icon--inspection", context),
                    "Capture review action [" + name + "] on [" + into.getName() + "]");
        }
    }

    private static void write(IntentGenerationContext context, String project, String fileBase, String descriptor, String description) {
        Map<String, Object> extension = new LinkedHashMap<>();
        extension.put("module", project + "/" + fileBase + ".json");
        extension.put("extensionPoint", project + "-custom-action");
        extension.put("description", description);
        context.writeModelFile(fileBase + ".extension", JsonHelper.toJson(extension));
        context.writeModelFile(fileBase + ".json", descriptor);
    }

    private static String descriptor(String project, CaptureIntent capture, String actionName, String label, String path, String type,
            String icon, IntentGenerationContext context) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", project + "-" + capture.getInto() + "-" + actionName);
        view.put("label", label);
        // The renderer shows T(translation.key, label) - untranslated apps keep the raw label.
        view.put("translation", Map.of("key", IntentNaming.customActionTranslationKey(project, context, actionName)));
        view.put("path", path);
        view.put("view", capture.getInto());
        view.put("type", type);
        view.put("icon", icon);
        return JsonHelper.toJson(view);
    }

    /** The capture page's URL for one capture, without the mode and the record id. */
    private static String page(String project, CaptureIntent capture, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentModel model, IntentGenerationContext context) {
        String module = IntentNaming.javaModule(context);
        StringBuilder url = new StringBuilder(PAGE);
        url.append("?capture=")
           .append(encode(
                   "/services/java/" + project + "/gen/events/" + module + "/" + IntentNaming.pascalCase(capture.getName()) + "Capture"));
        url.append("&entity=")
           .append(encode(capture.getInto()));
        EntityIntent attachment = capture.getAttachment() == null ? null : byName.get(capture.getAttachment());
        String backReference = attachment == null ? null : IntentEntities.itemsBackReference(attachment, capture.getInto());
        if (attachment != null && backReference != null) {
            String perspective = IntentEntities.resolvePerspective(attachment.getName(), compositionParents, model);
            url.append("&files=")
               .append(encode("/services/java/" + project + "/gen/" + module + "/api/" + IntentNaming.javaIdentifier(perspective) + "/"
                       + attachment.getName() + "Controller"));
            url.append("&fk=")
               .append(encode(backReference));
        }
        return url.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
