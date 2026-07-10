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
 * Root of the {@code .intent} YAML document. Every collection defaults to an empty list so partial
 * intents (e.g. entities-only) parse cleanly. The whole tree is intentionally shallow and uniform
 * so generators can dispatch by collection rather than by tree-walk.
 */
public class IntentModel {

    /** Optional intent identifier; defaults to the file's base name. */
    private String name;

    /** Optional one-line description shown in the IDE preview pane. */
    private String description;

    /**
     * Optional brand icon (a Lucide icon name, e.g. {@code book}) for the generated app's shell header.
     */
    private String icon;

    /** Schema version of the intent format. {@code 1} for the current draft. */
    private int version = 1;

    /** Other intent models this one references cross-model (see {@link UsesIntent}). */
    private List<UsesIntent> uses = new ArrayList<>();
    /**
     * Optional data-language codes the app offers (e.g. {@code [en, bg]}, short lowercase codes;
     * {@code en} - the base data - when omitted). Emitted onto the {@code .model} root; the Harmonia
     * shell's Region &amp; Language setting lists them and sends the choice as {@code
     * Accept-Language} on every call, which the generated multilingual repositories translate by.
     */
    private List<String> languages = new ArrayList<>();

    private List<EntityIntent> entities = new ArrayList<>();
    private List<ProcessIntent> processes = new ArrayList<>();
    private List<FormIntent> forms = new ArrayList<>();
    private List<ReportIntent> reports = new ArrayList<>();
    /** Custom dashboard widgets — developer-supplied REST KPIs and embedded pages. */
    private List<CustomWidgetIntent> widgets = new ArrayList<>();
    private List<PermissionIntent> permissions = new ArrayList<>();
    private List<SeedIntent> seeds = new ArrayList<>();
    private List<NotificationIntent> notifications = new ArrayList<>();
    private List<ScheduleIntent> schedules = new ArrayList<>();
    private List<IntegrationIntent> integrations = new ArrayList<>();
    private List<InboundIntent> inbound = new ArrayList<>();
    private List<RollupIntent> rollups = new ArrayList<>();
    /** Period expansions: a master's date span generated into child rows (days / weeks / months). */
    private List<ExpansionIntent> expansions = new ArrayList<>();
    private List<SettlementIntent> settlements = new ArrayList<>();
    /** Developer-declared on-demand action buttons contributed onto entity views. */
    private List<ActionIntent> actions = new ArrayList<>();
    /** Create-from (document generation) actions - clone a source record into a new target record. */
    private List<GeneratesIntent> generates = new ArrayList<>();
    /** Declarative postings: source-document status → generated local document + items. */
    private List<PostingIntent> postings = new ArrayList<>();

    public List<ActionIntent> getActions() {
        return actions;
    }

    public void setActions(List<ActionIntent> actions) {
        this.actions = actions == null ? new ArrayList<>() : actions;
    }

    public List<GeneratesIntent> getGenerates() {
        return generates;
    }

    public List<PostingIntent> getPostings() {
        return postings;
    }

    public void setPostings(List<PostingIntent> postings) {
        this.postings = postings == null ? new ArrayList<>() : postings;
    }

    public void setGenerates(List<GeneratesIntent> generates) {
        this.generates = generates == null ? new ArrayList<>() : generates;
    }

    public List<SettlementIntent> getSettlements() {
        return settlements;
    }

    public void setSettlements(List<SettlementIntent> settlements) {
        this.settlements = settlements;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<UsesIntent> getUses() {
        return uses;
    }

    public void setUses(List<UsesIntent> uses) {
        this.uses = uses == null ? new ArrayList<>() : uses;
    }

    public List<EntityIntent> getEntities() {
        return entities;
    }

    public void setEntities(List<EntityIntent> entities) {
        this.entities = entities == null ? new ArrayList<>() : entities;
    }

    public List<ProcessIntent> getProcesses() {
        return processes;
    }

    public void setProcesses(List<ProcessIntent> processes) {
        this.processes = processes == null ? new ArrayList<>() : processes;
    }

    public List<FormIntent> getForms() {
        return forms;
    }

    public void setForms(List<FormIntent> forms) {
        this.forms = forms == null ? new ArrayList<>() : forms;
    }

    public List<ReportIntent> getReports() {
        return reports;
    }

    public void setReports(List<ReportIntent> reports) {
        this.reports = reports == null ? new ArrayList<>() : reports;
    }

    public List<CustomWidgetIntent> getWidgets() {
        return widgets;
    }

    public void setWidgets(List<CustomWidgetIntent> widgets) {
        this.widgets = widgets == null ? new ArrayList<>() : widgets;
    }

    public List<PermissionIntent> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionIntent> permissions) {
        this.permissions = permissions == null ? new ArrayList<>() : permissions;
    }

    public List<SeedIntent> getSeeds() {
        return seeds;
    }

    public void setSeeds(List<SeedIntent> seeds) {
        this.seeds = seeds == null ? new ArrayList<>() : seeds;
    }

    public List<NotificationIntent> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<NotificationIntent> notifications) {
        this.notifications = notifications == null ? new ArrayList<>() : notifications;
    }

    public List<ScheduleIntent> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<ScheduleIntent> schedules) {
        this.schedules = schedules == null ? new ArrayList<>() : schedules;
    }

    public List<IntegrationIntent> getIntegrations() {
        return integrations;
    }

    public void setIntegrations(List<IntegrationIntent> integrations) {
        this.integrations = integrations == null ? new ArrayList<>() : integrations;
    }

    public List<InboundIntent> getInbound() {
        return inbound;
    }

    public void setInbound(List<InboundIntent> inbound) {
        this.inbound = inbound == null ? new ArrayList<>() : inbound;
    }

    public List<RollupIntent> getRollups() {
        return rollups;
    }

    public void setRollups(List<RollupIntent> rollups) {
        this.rollups = rollups == null ? new ArrayList<>() : rollups;
    }

    public List<ExpansionIntent> getExpansions() {
        return expansions;
    }

    public void setExpansions(List<ExpansionIntent> expansions) {
        this.expansions = expansions == null ? new ArrayList<>() : expansions;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }
}
