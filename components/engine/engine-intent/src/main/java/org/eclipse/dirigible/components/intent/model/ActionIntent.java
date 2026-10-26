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

import java.util.ArrayList;
import java.util.List;

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
 *
 * <p>
 * Instead of a page, an action may {@link #call} the application's own endpoint (issue #8090): the
 * shell sends the declared request after a confirmation ({@link #confirm}) or a dialog of
 * {@link #prompt} inputs, and toasts the outcome. {@link #fromStatus} hides the button outside the
 * listed statuses (advisory only - the endpoint decides), and {@link #can} offers it only to the
 * roles whose permissions list the token.
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

    /**
     * When {@code true} and the action applies to a record (its status guards and roles pass), it is
     * the record's filled button - in the list's details sheet and on the record page - instead of Edit
     * (#8119). A pending process task still comes first. Entity scope only.
     */
    private Boolean primary;

    /** Same-origin path opened in the app-wide dialog when the action is triggered. */
    private String page;

    /** The request sent to the application's own endpoint; excludes {@link #page}. */
    private ActionCallIntent call;

    /** The inputs a {@link #call} collects in a dialog before it is sent. */
    private List<ActionPromptIntent> prompt = new ArrayList<>();

    /** The confirmation text of a {@link #call} without a {@link #prompt}. */
    private String confirm;

    /** The {@code forEntity} statuses (seed ids) the button is offered in. */
    private List<Integer> fromStatus = new ArrayList<>();

    /** The {@code Resource:action} permission token whose roles are offered the button. */
    private String can;

    /** The toast text of a successful {@link #call}. */
    private String success;

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

    public Boolean getPrimary() {
        return primary;
    }

    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }

    /** Whether this action is the record's filled button when it applies (see {@link #primary}). */
    public boolean isPrimary() {
        return Boolean.TRUE.equals(primary);
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public ActionCallIntent getCall() {
        return call;
    }

    public void setCall(ActionCallIntent call) {
        this.call = call;
    }

    public List<ActionPromptIntent> getPrompt() {
        return prompt;
    }

    public void setPrompt(List<ActionPromptIntent> prompt) {
        this.prompt = prompt == null ? new ArrayList<>() : prompt;
    }

    public String getConfirm() {
        return confirm;
    }

    public void setConfirm(String confirm) {
        this.confirm = confirm;
    }

    public List<Integer> getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(List<Integer> fromStatus) {
        this.fromStatus = fromStatus == null ? new ArrayList<>() : fromStatus;
    }

    public String getCan() {
        return can;
    }

    public void setCan(String can) {
        this.can = can;
    }

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }
}
