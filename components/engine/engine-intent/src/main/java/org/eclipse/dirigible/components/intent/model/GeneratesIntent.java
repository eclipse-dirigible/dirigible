/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A "create-from" (document generation) declaration: an on-demand action that clones a source
 * record ({@link #from}) into a new target record ({@link #to}), possibly in another model
 * ({@link #uses}) - e.g. generate a {@code SalesInvoice} from a {@code ProjectTimesheet}.
 *
 * <p>
 * It generates two halves:
 * <ul>
 * <li>a client button contributed onto the {@link #forEntity} view via the app's
 * {@code <project>-custom-action} extension point (the {@code GeneratesIntentGenerator}), carrying
 * an {@code endpoint} rather than a page; and</li>
 * <li>a server-side Java {@code @Controller} (emitted through the {@code .glue} file + the
 * {@code template-application-events-java} template) that loads the source through its generated
 * repository, maps it onto a fresh target entity, and saves through the <b>target's</b> generated
 * repository - so the target's create-time logic (document numbering, status init, calculated
 * fields) fires naturally.</li>
 * </ul>
 *
 * <p>
 * Mapping is split into two disjoint maps so the source-copy vs constant intent is unambiguous:
 * {@link #map} copies a source property onto a target property; {@link #defaults} sets a target
 * property to {@code now} (the current date) or a literal (string / integer / decimal / boolean).
 * {@link #items} optionally mirrors the composition child rows the same way.
 */
public class GeneratesIntent {

    /**
     * Unique name within the model; drives the action id, the contribution files and the controller.
     */
    private String name;

    /** The source entity in THIS model, loaded by the selected record's id. */
    private String from;

    /** The target entity to create. May live in another model (see {@link #uses}). */
    private String to;

    /**
     * Optional model alias (from the model's {@code uses:} list) the {@link #to} entity lives in. Blank
     * means the target is in this same model.
     */
    private String uses;

    /**
     * The entity whose generated view shows the button. Defaults to {@link #from} when blank (the
     * source record is the natural place to trigger a create-from).
     */
    private String forEntity;

    /** Button label; defaults to a humanized {@link #name} when blank. */
    private String label;

    /** Optional Lucide icon name carried onto the contribution descriptor. */
    private String icon;

    /** {@code entity} (per-record, default - it needs a source id) or {@code page}. */
    private String scope = "entity";

    /** Optional ordering hint among the contributed actions of a view. */
    private Integer order;
    /**
     * Optional completion hook: the EntityStatus seed id the SOURCE record is set to after the target
     * is created (e.g. a proforma flips to INVOICED once the invoice exists). A workflow-style system
     * write - no {@code -updated} re-fire, but the source's {@code -transitioned} topic IS published.
     * Requires the {@code from} entity to declare a {@code function: EntityStatus} relation.
     */
    private Integer sourceStatus;

    /** Target property -> source property (a field or to-one relation name of {@link #from}). */
    private Map<String, String> map = new LinkedHashMap<>();

    /** Target property -> {@code now} or a literal value (string / integer / decimal / boolean). */
    private Map<String, String> defaults = new LinkedHashMap<>();

    /**
     * Optional composition child mapping (the source document's items -> the target document's items).
     */
    private GeneratesItemsIntent items;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getUses() {
        return uses;
    }

    public void setUses(String uses) {
        this.uses = uses;
    }

    public String getForEntity() {
        return forEntity == null || forEntity.isBlank() ? from : forEntity;
    }

    public void setForEntity(String forEntity) {
        this.forEntity = forEntity;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope == null || scope.isBlank() ? "entity" : scope;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Integer getSourceStatus() {
        return sourceStatus;
    }

    public void setSourceStatus(Integer sourceStatus) {
        this.sourceStatus = sourceStatus;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map == null ? new LinkedHashMap<>() : map;
    }

    public Map<String, String> getDefaults() {
        return defaults;
    }

    public void setDefaults(Map<String, String> defaults) {
        this.defaults = defaults == null ? new LinkedHashMap<>() : defaults;
    }

    public GeneratesItemsIntent getItems() {
        return items;
    }

    public void setItems(GeneratesItemsIntent items) {
        this.items = items;
    }
}
