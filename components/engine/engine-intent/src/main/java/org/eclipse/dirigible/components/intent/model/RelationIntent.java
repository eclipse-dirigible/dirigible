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

import java.util.List;
import java.util.Map;

/**
 * Relationship between two {@link EntityIntent}s. {@link #kind} is one of {@code oneToMany},
 * {@code manyToOne}, {@code oneToOne}, {@code manyToMany}.
 */
public class RelationIntent {

    private String name;
    private String kind;
    private String to;
    private boolean required;
    private boolean composition;
    private String description;
    /**
     * Optional cross-model reference: the {@code uses[].model} alias of the intent model that owns the
     * {@link #to} target entity. Absent (the default) means the target is an entity in this same model.
     * Present means the relation is a cross-model association - the generator emits a
     * {@code PROJECTION} to the owner model plus an integer FK + dropdown, and never a local table for
     * the target.
     */
    private String model;
    /**
     * Pre-rename boolean form of the status role - REJECTED by the parser with a clear migration
     * message; kept as a field only so the validator can detect it. Author {@code function:
     * EntityStatus} instead.
     */
    private boolean documentStatus;

    /**
     * Explicit relation role - currently {@code EntityStatus}: marks this to-one relation as the
     * entity's system-managed <b>status</b> (widget {@code DOCUMENT_STATUS}). Valid on ANY entity, not
     * only documents. It renders as a read-only coloured badge (title-bar pill in the document and
     * shared manage forms, badge pills in the list tables) instead of an editable dropdown - the value
     * is managed by the platform (an {@code init:} seed, a workflow step, a roll-up status), never
     * typed by the user. An entity whose status must be hand-set simply does not mark the relation.
     */
    private String function;
    /**
     * Optional initial value for this to-one relation's FK: the integer id of a seed row of the target
     * (e.g. a status nomenclature). Emitted as the FK column's {@code dataDefaultValue}, so a new row
     * gets this FK at the <b>database level</b> on insert when the column is left unset. Use this for
     * an initial document status (e.g. {@code Status} defaults to the DRAFT seed) instead of a process
     * step - a default is race-free, whereas a process step that sets it on start races the trigger's
     * {@code ProcessId} write-back. Named {@code init} for readability (it is the relation's analogue
     * of a field's {@code defaultValue}).
     */
    private String init;
    /**
     * Optional form-control width as a 12-column grid span (3/4/6/12: 3 = quarter, 4 = third, 6 = half,
     * 12 = full). Emitted as the FK property's {@code widgetSize}; the Harmonia form maps it to
     * {@code grid-column: span N}. Absent (the default) leaves it unset (the form falls back to half
     * width). Use a small span (e.g. 4) to pack several short dropdowns onto one row.
     */
    private Integer size;
    /**
     * Optional list of the target entity's field names to surface as extra <b>read-only</b> columns
     * wherever this to-one relation shows as a lookup column (the master-detail / document allocation
     * tables). The FK lookup already fetches the referenced rows to resolve the display label, so these
     * columns are pulled from that same fetched row - no extra request, no URL construction (works for
     * a cross-model target too). Names are the target's logical field names (e.g.
     * {@code [date, number]}); the generator PascalCases them to match the REST JSON.
     */
    private List<String> show;

    /**
     * Optional Depends-On declaration: this dropdown reacts to a sibling to-one relation - its option
     * list is re-filtered (and a single match auto-selected) when the trigger's selection changes.
     * Emitted as the {@code widgetDependsOn*} attributes on the FK property. See
     * {@link DependsOnIntent}.
     */
    private DependsOnIntent dependsOn;

    /**
     * Optional static option filter: a single {@code <target property>: <literal>} pair that narrows
     * this dropdown's option list to the target rows matching it (e.g. a stock line's Product picker
     * showing only {@code Type: 1} = real products, never services). Unlike {@code dependsOn} (which
     * reacts to a sibling selection) the condition is constant. Emitted as the
     * {@code widgetOptionsFilterBy} / {@code widgetOptionsFilterValue} attributes on the FK property;
     * label-resolution lookups deliberately keep the full set so historical rows still resolve.
     */
    private Map<String, Object> where;

    /**
     * Restricts this to-one relation to LEAF nodes of its (hierarchical) target: the picker offers only
     * childless nodes and the generated REST validation rejects an FK to a node with children (e.g. a
     * journal line may reference an analytical account, never a synthetic one). Valid only when the
     * target entity declares {@code hierarchy}.
     */
    private boolean leafOnly;

    /**
     * Marks this to-one relation as the OWNER of the record for the personal surface: on the generated
     * personal (my) REST controller, reads are filtered to the logged-in user's mapped identity record
     * and writes force this FK server-side. Valid only when the target entity declares
     * {@code identity}. Composition children inherit the scope through their parent chain.
     */
    private boolean personal;

    /**
     * When set together with {@link #personal}, the personal (my) surface is READ-ONLY for the owner:
     * the generated {@code <Entity>MyController} exposes only the scoped reads (getAll / get / count)
     * and its create/update/delete return 405, and the personal pages render without New / Edit /
     * Delete. Use for records the owner may SEE but never author (a leave-balance account, a payslip);
     * the regular (power) controller is unaffected. Ignored unless {@link #personal} is also true.
     */
    private boolean personalReadOnly;

    /**
     * Marks this to-one relation as the OWNER of the record for the PARTNER surface: the generated
     * partner REST controller scopes reads to the logged-in external partner's mapped identity record
     * and forces this FK server-side on writes, and the partner perspective registers on the Partner
     * shell's extension point (disjoint from the personal shell). The exact mirror of {@link #personal}
     * keyed to an external partner entity (Customer / Supplier, which carry {@code identity: email}).
     * Valid only when the target declares {@code identity}. Composition children inherit the scope.
     */
    private boolean partner;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isComposition() {
        return composition;
    }

    public void setComposition(boolean composition) {
        this.composition = composition;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    /** Whether this relation targets an entity owned by another intent model. */
    public boolean isCrossModel() {
        return model != null && !model.isBlank();
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    /**
     * Whether this to-one relation is the entity's status badge - {@code function: EntityStatus}. The
     * pre-rename {@code function: DocumentStatus} and the {@code documentStatus: true} boolean are
     * rejected at parse time with a migration message.
     */
    public boolean isEntityStatus() {
        return function != null && "EntityStatus".equalsIgnoreCase(function.trim());
    }

    /** Whether the pre-rename {@code documentStatus: true} boolean was authored (parse-rejected). */
    public boolean isLegacyDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(boolean documentStatus) {
        this.documentStatus = documentStatus;
    }

    public String getInit() {
        return init;
    }

    public void setInit(String init) {
        this.init = init;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public List<String> getShow() {
        return show;
    }

    public void setShow(List<String> show) {
        this.show = show;
    }

    public DependsOnIntent getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(DependsOnIntent dependsOn) {
        this.dependsOn = dependsOn;
    }

    public Map<String, Object> getWhere() {
        return where;
    }

    public void setWhere(Map<String, Object> where) {
        this.where = where;
    }

    public boolean isLeafOnly() {
        return leafOnly;
    }

    public void setLeafOnly(boolean leafOnly) {
        this.leafOnly = leafOnly;
    }

    public boolean isPersonal() {
        return personal;
    }

    public void setPersonal(boolean personal) {
        this.personal = personal;
    }

    public boolean isPersonalReadOnly() {
        return personalReadOnly;
    }

    public void setPersonalReadOnly(boolean personalReadOnly) {
        this.personalReadOnly = personalReadOnly;
    }

    public boolean isPartner() {
        return partner;
    }

    public void setPartner(boolean partner) {
        this.partner = partner;
    }
}
