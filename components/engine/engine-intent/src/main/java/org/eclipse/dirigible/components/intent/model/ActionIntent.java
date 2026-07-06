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

/**
 * On-demand action declaration. Contributes a developer-defined action button onto an entity's
 * generated Harmonia view via the app's {@code <project>-custom-action} extension point (the same
 * mechanism external projects use), so the app's own actions and third-party contributions render
 * through one path (the {@code customActions} store).
 *
 * {@link #forEntity} ties the action to an {@link EntityIntent}; {@link #scope} decides where the
 * button renders: {@code page} (a toolbar action on the whole view) or {@code entity} (a per-record
 * action that carries the selected record's id to the opened page). {@link #page} is the
 * same-origin path opened in the app-wide dialog when the button is clicked.
 */
public class ActionIntent {

    /** Unique action id within the model; also drives the generated contribution file names. */
    private String name;

    /** The entity whose generated view shows this action's button. Must be a declared entity. */
    private String forEntity;

    /** {@code entity} (per-record, default) or {@code page} (whole-view toolbar action). */
    private String scope = "entity";

    /** Button label; defaults to a humanized {@link #name} when blank. */
    private String label;

    /** Optional Lucide icon name (informational; carried onto the contribution descriptor). */
    private String icon;

    /** Optional ordering hint among the contributed actions of a view. */
    private Integer order;

    /** Same-origin path opened in the app-wide dialog when the action is triggered. */
    private String page;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getForEntity() {
        return forEntity;
    }

    public void setForEntity(String forEntity) {
        this.forEntity = forEntity;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope == null || scope.isBlank() ? "entity" : scope;
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

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }
}
