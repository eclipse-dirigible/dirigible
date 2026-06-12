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
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.IntentNaming;
import org.eclipse.dirigible.components.intent.generator.IntentTargetGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.PermissionIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Emits {@code <intent>.roles} from the intent's {@code permissions}. The roles are deduped by
 * name; the {@code can: [Resource:action, ...]} tokens are NOT translated into {@code .access}
 * constraints here. URL-shaped access constraints belong to the downstream EDM / form / report
 * template generators, which know the paths they will publish; emitting them from the intent layer
 * would couple the engine to a specific template's output paths.
 *
 * <p>
 * The {@code permissions} block on the intent therefore plays two roles today:
 * <ul>
 * <li>It is the canonical place to declare the role <em>names</em> the app uses - this generator
 * materializes those into {@code .roles}, which the {@code RolesSynchronizer} picks up.</li>
 * <li>The {@code can} tokens are a hint to downstream UI generators about which actions each role
 * may invoke. The mapping from token to URL is the downstream template's contract, not intent's.
 * Until that contract is wired, the tokens are informational; document them but do not generate
 * {@code .access} from them.</li>
 * </ul>
 *
 * <p>
 * Idempotent: identical input always produces byte-identical output.
 */
@Component
@Order(600)
public class PermissionIntentGenerator implements IntentTargetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionIntentGenerator.class);

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
        context.writeModelFile(IntentNaming.baseName(context) + ".roles", buildRolesJson(model));
    }

    private static String buildRolesJson(IntentModel model) {
        Set<String> seenNames = new LinkedHashSet<>();
        java.util.List<Map<String, String>> roles = new ArrayList<>();
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
}
