/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import org.eclipse.dirigible.commons.api.helpers.NamingHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.asMaps;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.str;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.strOr;
import static org.eclipse.dirigible.components.ide.template.service.model.ModelValues.truthy;

/**
 * The reverse index of the composition edges: which children each MASTER owns, so the master's
 * generated repository can delete them with it - or refuse its own delete while they exist
 * (dirigible #7100).
 *
 * <p>
 * A composition is authored on the CHILD (its {@code COMPOSITION} FK property, from which
 * {@link ModelParameterProcessor} derived {@code masterEntity} / {@code masterEntityId}), and that
 * is the only direction the model records. Without this index a deleted master leaves its children
 * behind pointing at an id that no longer exists - invisible in the UI, since no parent page
 * renders them, and still counted by every report and roll-up over the child.
 */
final class CompositionChildren {

    /** Not instantiable. */
    private CompositionChildren() {}

    /**
     * Attaches a {@code compositionChildren} entry to every master that owns one, in model order.
     *
     * @param entities the entities, already carrying the labels the refusal message names
     */
    static void annotate(List<Map<String, Object>> entities) {
        Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
        for (Map<String, Object> entity : entities) {
            byName.put(str(entity, "name"), entity);
        }
        for (Map<String, Object> child : entities) {
            String fkProperty = str(child, "masterEntityId");
            Map<String, Object> master = byName.get(str(child, "masterEntity"));
            // A projection has no local table and no repository on either side of the edge: the child of
            // one is owned by another model, and a projection master IS that other model's row.
            if (fkProperty == null || master == null || "PROJECTION".equals(str(child, "type"))
                    || "PROJECTION".equals(str(master, "type"))) {
                continue;
            }
            Map<String, Object> owned = new LinkedHashMap<>();
            owned.put("childEntity", child.get("name"));
            owned.put("javaChildPerspective", NamingHelper.sanitizeJavaIdentifier(str(child, "perspectiveName")));
            owned.put("fkProperty", fkProperty);
            owned.put("childLabel", strOr(child, "entityLabel", str(child, "name")));
            owned.put("refuse", refusesMasterDelete(child) ? "true" : "false");
            if (!(master.get("compositionChildren") instanceof List)) {
                master.put("compositionChildren", new ArrayList<>());
            }
            ModelValues.asList(master.get("compositionChildren"))
                       .add(owned);
        }
    }

    /**
     * Whether the child's composition property carries {@code relationshipMasterDeleteRefused} - the
     * author's decision ({@code whenMasterDeleted: refuse}) that the master's delete is rejected while
     * children exist, rather than cascading into them.
     *
     * @param child the composition child
     * @return true when the master's delete must be refused
     */
    private static boolean refusesMasterDelete(Map<String, Object> child) {
        for (Map<String, Object> property : asMaps(child.get("properties"))) {
            if ("COMPOSITION".equals(str(property, "relationshipType")) && truthy(property, "relationshipMasterDeleteRefused")) {
                return true;
            }
        }
        return false;
    }
}
