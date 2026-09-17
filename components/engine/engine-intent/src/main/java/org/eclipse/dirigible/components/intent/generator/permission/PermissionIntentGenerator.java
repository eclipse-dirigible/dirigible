/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.permission;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.intent.generator.IntentEntities;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.generator.PermissionSupport;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.PermissionIntent;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Emits the intent's access model from its {@code permissions} block:
 * <ul>
 * <li>{@code <intent>.roles} - the role NAMES the application uses, deduped by name, which the
 * {@code RolesSynchronizer} picks up.</li>
 * <li>{@code <intent>.access} - the URL-shaped constraints over the paths the generated stack
 * publishes (each entity's controller subtree and generated pages, each report's controller and
 * page), derived from the {@code can: [Resource:action, ...]} tokens. <b>Opt-in</b> through the
 * project's {@code .settings} ({@code "access": {"generate": true}}) rather than a DSL key: the
 * paths belong to the templates the settings' recipes name, so whether to gate them is a decision
 * about this deployment, not a property of the domain.</li>
 * </ul>
 *
 * <p>
 * The {@code can:} tokens are ALSO what the entity and report gates are bound from - see
 * {@link PermissionSupport}, which the EDM and report generators consult so the role the author
 * declared is the role the generated controller checks. Anything those tokens could not bind is
 * reported here (an undeclared resource as an issue, an action outside the read/write vocabulary as
 * an advisory), reported once for the whole pass rather than once per generator that consults them.
 *
 * <p>
 * A hand-authored {@code .access} at the PROJECT ROOT is scrub-owned - {@code .access} is an
 * intent-owned extension, so the next Generate deletes it. Hand-written constraints belong under
 * {@code custom/}, which the scrub never touches.
 *
 * <p>
 * Idempotent: identical input always produces byte-identical output.
 */
@Component
@Order(600)
public class PermissionIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionIntentGenerator.class);

    /** The perspective a generated report's controller lives under. */
    private static final String REPORTS_PERSPECTIVE = "Reports";

    @Override
    public String name() {
        return "permission";
    }

    @Override
    public void generate(IntentGenerationContext context) {
        IntentModel model = context.getModel();
        if (model.getPermissions()
                 .isEmpty()) {
            return;
        }
        PermissionSupport.Gates gates = PermissionSupport.gates(model);
        gates.issues()
             .forEach(context::addIssue);
        gates.advisories()
             .forEach(context::addAdvisory);
        context.writeModelFile(IntentNaming.baseName(context) + ".roles", buildRolesJson(model));
        if (context.getSettings()
                   .isGenerateAccess()) {
            List<Map<String, Object>> constraints = buildConstraints(context, gates);
            if (!constraints.isEmpty()) {
                Map<String, Object> document = new LinkedHashMap<>();
                document.put("constraints", constraints);
                context.writeModelFile(IntentNaming.baseName(context) + ".access", JsonHelper.toJson(document));
            }
        }
    }

    private static String buildRolesJson(IntentModel model) {
        Set<String> seenNames = new LinkedHashSet<>();
        List<Map<String, String>> roles = new ArrayList<>();
        for (PermissionIntent permission : model.getPermissions()) {
            String name = permission.getRole();
            if (name == null || name.isBlank()) {
                LOGGER.warn("Skipping permission entry with no role name");
                continue;
            }
            if (!seenNames.add(name)) {
                continue;
            }
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("name", name);
            if (permission.getDescription() != null && !permission.getDescription()
                                                                  .isBlank()) {
                entry.put("description", permission.getDescription());
            }
            roles.add(entry);
        }
        return JsonHelper.toJson(roles);
    }

    /**
     * The constraints for every gate-bearing resource a {@code can:} token names.
     *
     * <p>
     * Each constraint carries the resource's READ roles and method {@code *}: the artefact gates
     * whether the caller may reach the endpoint or the page at all, while the read-versus-write split
     * stays in the generated controller's own {@code checkPermissions}. Splitting it here by HTTP
     * method would be wrong rather than merely redundant - the generated controllers read through
     * {@code POST .../search} and {@code POST .../count}, so "POST means write" would lock a read-only
     * role out of every list and register.
     *
     * @param context the generation context
     * @param gates the authored read / write role sets
     * @return the constraints, in resource-declaration order
     */
    private static List<Map<String, Object>> buildConstraints(IntentGenerationContext context, PermissionSupport.Gates gates) {
        IntentModel model = context.getModel();
        String project = context.getProjectName() == null || context.getProjectName()
                                                                    .isEmpty() ? IntentNaming.baseName(context) : context.getProjectName();
        String base = IntentNaming.baseName(context);
        String module = IntentNaming.javaModule(context);
        Map<String, String> compositionParents = IntentEntities.compositionParents(model);
        List<Map<String, Object>> constraints = new ArrayList<>();
        for (EntityIntent entity : model.getEntities()) {
            String name = entity.getName();
            if (name == null || name.isBlank()) {
                continue;
            }
            if (entity.getExtend() != null) {
                // An EXTENSION contributes its fields to an entity owned elsewhere; it publishes no
                // controller and no page of its own, so there is no path here to constrain.
                continue;
            }
            // Same rule the EDM gates follow: a composition child with no grants of its own is gated
            // by the master it is managed under, so a document's items are reachable to exactly the
            // roles the document is.
            String gateName = gates.covers(name) ? name : IntentEntities.compositionPerspective(name, compositionParents);
            Set<String> readRoles = gates.read()
                                         .get(gateName);
            if (readRoles == null) {
                continue;
            }
            List<String> roles = List.copyOf(readRoles);
            String perspective = IntentEntities.resolvePerspective(name, compositionParents, model);
            constraints.add(constraint("/services/java/" + project + "/gen/" + module + "/api/" + IntentNaming.javaIdentifier(perspective)
                    + "/" + name + "Controller/**", roles));
            constraints.add(
                    constraint("/services/web/" + project + "/gen/" + base + "/views/" + perspective + "/" + name + "-*.html", roles));
            constraints.add(constraint(
                    "/services/web/" + project + "/gen/" + base + "/js/components/pages/" + perspective + "/" + name + "*.js", roles));
        }
        for (ReportIntent report : model.getReports()) {
            String name = report.getName();
            if (name == null || !gates.read()
                                      .containsKey(name)) {
                continue;
            }
            List<String> roles = List.copyOf(gates.read()
                                                  .get(name));
            // A report is generated from its own <Report>.report, so its generation folder is the
            // report's name - not the intent's base name - and its controller sits under Reports.
            constraints.add(constraint("/services/java/" + project + "/gen/" + IntentNaming.javaIdentifier(name) + "/api/"
                    + IntentNaming.javaIdentifier(REPORTS_PERSPECTIVE) + "/" + name + "Controller/**", roles));
            constraints.add(constraint("/services/web/" + project + "/gen/" + name + "/reports/" + name + "/**", roles));
        }
        return constraints;
    }

    private static Map<String, Object> constraint(String path, List<String> roles) {
        Map<String, Object> constraint = new LinkedHashMap<>();
        constraint.put("path", path);
        constraint.put("method", "*");
        constraint.put("scope", "HTTP");
        constraint.put("roles", roles);
        return constraint;
    }
}
