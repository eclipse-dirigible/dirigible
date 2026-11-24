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
 * list, or for a composition child a per-record button on its master, whose id the page passes to
 * the upload as the line's key to it ({@code under}, #8626). It opens the shared capture page in
 * the app-wide dialog, where the user picks or photographs the document, uploads it and is shown
 * what the capture flagged for review.</li>
 * <li><b>the personal capture action</b> ({@code <name>-capture-my-action}) - the same button on
 * the personal pages of the master, when it has a {@code personal:} owner: the page then talks to
 * the capture's owner-scoped {@code /my} endpoints.</li>
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
            String page = page(project, capture, "", byName, compositionParents, model, context);
            // A line of an existing record (#8626) is captured from that record: the button is the
            // master's per-record action, and the page is told the line's key to it for the upload.
            String master = compositionParents.get(into.getName());
            String under = master == null ? null : IntentEntities.itemsBackReference(into, master);
            String captureView = under == null ? into.getName() : master;
            write(context, project, name + "-capture-action",
                    descriptor(project, capture, name, CaptureSupport.label(capture),
                            under == null ? page : page + "&under=" + encode(under), captureView, under == null ? "page" : "entity",
                            "sap-icon--camera", null, context),
                    "Capture action [" + name + "] on [" + captureView + "]");
            if (under != null && CaptureSupport.fromPersonalSurface(into, byName, compositionParents)) {
                // ... and by its owner on the personal pages of their own record, through the capture's
                // owner-scoped endpoints (.../my/upload, .../my/review/{id}).
                write(context, project, name + "-capture-my-action",
                        descriptor(project, capture, name, CaptureSupport.label(capture),
                                page(project, capture, "/my", byName, compositionParents, model, context) + "&under=" + encode(under),
                                master, "entity", "sap-icon--camera", "my", context),
                        "Personal capture action [" + name + "] on [" + master + "]");
            }
            String review = name + REVIEW_SUFFIX;
            write(context, project, name + "-capture-review-action",
                    descriptor(project, capture, review, CaptureSupport.reviewLabel(capture), page + "&mode=review", into.getName(),
                            "entity", "sap-icon--inspection", null, context),
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

    private static String descriptor(String project, CaptureIntent capture, String actionName, String label, String path, String viewName,
            String type, String icon, String surface, IntentGenerationContext context) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", project + "-" + capture.getInto() + "-" + actionName + (surface == null ? "" : "-" + surface));
        view.put("label", label);
        // The renderer shows T(translation.key, label) - untranslated apps keep the raw label.
        view.put("translation", Map.of("key", IntentNaming.customActionTranslationKey(project, context, actionName)));
        view.put("path", path);
        view.put("view", viewName);
        view.put("type", type);
        view.put("icon", icon);
        if (surface != null) {
            view.put("surface", surface);
        }
        return JsonHelper.toJson(view);
    }

    /**
     * The capture page's URL for one capture, without the mode and the record id; {@code endpoints} is
     * appended to the capture controller's path ({@code /my} for the owner-scoped ones).
     */
    private static String page(String project, CaptureIntent capture, String endpoints, Map<String, EntityIntent> byName,
            Map<String, String> compositionParents, IntentModel model, IntentGenerationContext context) {
        String module = IntentNaming.javaModule(context);
        StringBuilder url = new StringBuilder(PAGE);
        url.append("?capture=")
           .append(encode("/services/java/" + project + "/gen/events/" + module + "/" + IntentNaming.pascalCase(capture.getName())
                   + "Capture" + endpoints));
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
