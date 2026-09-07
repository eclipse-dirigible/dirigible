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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Covers the reverse index of the composition edges (dirigible #7100): a composition is authored on
 * the CHILD, and this is what lets the MASTER's repository deal with the rows it owns before it is
 * deleted.
 */
class CompositionChildrenTest {

    @Test
    void aMasterCarriesEveryChildThatComposesIntoIt() {
        Map<String, Object> master = entity("VacationRequest", "PRIMARY", "vacations");
        Map<String, Object> days = child("VacationDay", "vacations", "VacationRequest", "Request", false);
        Map<String, Object> attachments = child("VacationAttachment", "vacations", "VacationRequest", "Request", true);
        List<Map<String, Object>> entities = new ArrayList<>(List.of(master, days, attachments));

        CompositionChildren.annotate(entities);

        List<Object> owned = ModelValues.asList(master.get("compositionChildren"));
        assertThat(owned).as("both children of the master, in model order")
                         .hasSize(2);
        assertThat(asMap(owned.get(0))).containsEntry("childEntity", "VacationDay")
                                       .containsEntry("fkProperty", "Request")
                                       .containsEntry("javaChildPerspective", "vacations")
                                       .containsEntry("childLabel", "Vacation Day")
                                       .containsEntry("refuse", "false");
        assertThat(asMap(owned.get(1))).as("whenMasterDeleted: refuse rides the child's composition property")
                                       .containsEntry("childEntity", "VacationAttachment")
                                       .containsEntry("refuse", "true");
        assertThat(days.get("compositionChildren")).as("a childless child owns nothing")
                                                   .isNull();
    }

    /** A hyphenated perspective is a package segment in the generated import. */
    @Test
    void theChildPerspectiveIsSanitizedIntoAJavaIdentifier() {
        Map<String, Object> master = entity("SalesOrder", "PRIMARY", "sales-order");
        Map<String, Object> item = child("SalesOrderItem", "sales-order", "SalesOrder", "SalesOrder", false);
        List<Map<String, Object>> entities = new ArrayList<>(List.of(master, item));

        CompositionChildren.annotate(entities);

        assertThat(asMap(ModelValues.asList(master.get("compositionChildren"))
                                    .get(0))).containsEntry("javaChildPerspective", "sales_order");
    }

    /**
     * A projection has no local table and no repository: the master of one is another model's row, so
     * nothing here could delete it.
     */
    @Test
    void aProjectionMasterOwnsNothing() {
        Map<String, Object> projection = entity("Customer", "PROJECTION", "");
        Map<String, Object> note = child("CustomerNote", "notes", "Customer", "Customer", false);
        List<Map<String, Object>> entities = new ArrayList<>(List.of(projection, note));

        CompositionChildren.annotate(entities);

        assertThat(projection.get("compositionChildren")).isNull();
    }

    /** An entity with no composition parent is not anybody's child. */
    @Test
    void anEntityWithoutAMasterIsNotIndexed() {
        Map<String, Object> standalone = entity("Country", "PRIMARY", "master-data");
        List<Map<String, Object>> entities = new ArrayList<>(List.of(standalone));

        CompositionChildren.annotate(entities);

        assertThat(standalone.get("compositionChildren")).isNull();
    }

    private static Map<String, Object> entity(String name, String type, String perspectiveName) {
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("name", name);
        entity.put("type", type);
        entity.put("perspectiveName", perspectiveName);
        entity.put("properties", new ArrayList<>());
        return entity;
    }

    private static Map<String, Object> child(String name, String perspectiveName, String masterEntity, String fkProperty,
            boolean refuseMasterDelete) {
        Map<String, Object> child = entity(name, "DEPENDENT", perspectiveName);
        child.put("entityLabel", name.replaceAll("(?<=[a-z])(?=[A-Z])", " "));
        child.put("masterEntity", masterEntity);
        child.put("masterEntityId", fkProperty);
        Map<String, Object> fk = new LinkedHashMap<>();
        fk.put("name", fkProperty);
        fk.put("relationshipType", "COMPOSITION");
        if (refuseMasterDelete) {
            fk.put("relationshipMasterDeleteRefused", "true");
        }
        ModelValues.asList(child.get("properties"))
                   .add(fk);
        return child;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }
}
