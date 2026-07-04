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
 * Domain entity declaration in an {@code .intent}. Generates one EDM entity, one DSM table, one TS
 * or Java entity decorator, and a default repository / controller pair downstream.
 */
public class EntityIntent {

    private String name;
    private String description;
    /**
     * Optional entity classification. {@code setting} marks the entity as nomenclature / configuration
     * data: the EDM generator emits it with {@code type="SETTING"} so the template engine routes it
     * under the dashboard's Settings menu instead of generating a top-level perspective. Absent (the
     * default) means a regular managed entity.
     */
    private String kind;
    /**
     * Optional icon for the entity's navigation entry. A short icon name (e.g. {@code book},
     * {@code user}); the Harmonia UI renders it as a Lucide icon and the EDM generator also maps it to
     * a unicons SVG URL for the AngularJS perspective. Absent → a default list icon.
     */
    private String icon;
    /**
     * Whether the generator adds the four standard audit columns ({@code CreatedAt}, {@code CreatedBy},
     * {@code UpdatedAt}, {@code UpdatedBy}) the platform's {@code org.eclipse.dirigible.sdk.db} audit
     * annotations populate. Absent (the default) → no audit columns.
     */
    private Boolean audit;
    /**
     * Optional navigation-group id. When set, the generated perspective for this entity carries this as
     * its {@code groupId}, so the shared application shell nests it under the matching navigation group
     * (defined once, e.g. in a dedicated navigation-groups project). Absent → the perspective is
     * top-level (or under the platform's default group). Does not affect the project's own standalone
     * shell.
     */
    private String group;
    /**
     * Optional Java import lines injected verbatim into the generated entity Repository so calculated
     * fields can reference custom classes - chiefly a {@code calculatedActionOnCreate} action's
     * {@code org.eclipse.dirigible.sdk.db.CalculatedField} implementation. A multi-line string of
     * {@code import ...;} statements; the EDM generator Base64-encodes it into the model's
     * {@code importsCode} the DAO template emits. Absent → no custom imports.
     */
    private String imports;
    /**
     * Marks the entity as <b>multilingual</b>: its translatable (string-typed) properties may carry
     * per-language values in a sibling {@code
     *
    <TABLE>
     * _LANG} table (the codbex convention). Emitted as the EDM entity attribute
     * {@code multilingual="true"}; the schema template then generates the language table and the Java
     * DAO template overlays translated values on every read for the caller's {@code Accept-Language}.
     * Translation rows are authored as {@code seeds} with a {@code language:} code.
     */
    private Boolean multilingual;
    /**
     * Optional explicit ordering of the generated UI controls (form inputs, list columns, detail rows)
     * by property name - fields and to-one relations interleaved, in the given order. Names match the
     * authored field / relation names (case-insensitive). Any property not listed keeps its default
     * position, appended after the listed ones. Absent → the default order (fields in declaration
     * order, then to-one relations), which pushes all relations to the end.
     */
    private List<String> order = new ArrayList<>();
    private List<FieldIntent> fields = new ArrayList<>();
    private List<RelationIntent> relations = new ArrayList<>();

    public String getName() {
        return name;
    }

    public List<String> getOrder() {
        return order;
    }

    public void setOrder(List<String> order) {
        this.order = order == null ? new ArrayList<>() : order;
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

    /**
     * Whether this entity is declared as a setting / nomenclature (case-insensitive
     * {@code kind: setting}).
     */
    public boolean isSetting() {
        return "setting".equalsIgnoreCase(kind);
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

    /** Whether this entity gets the four standard audit columns ({@code audit: true}). */
    public boolean isAudited() {
        return Boolean.TRUE.equals(audit);
    }

    public Boolean getAudit() {
        return audit;
    }

    public void setAudit(Boolean audit) {
        this.audit = audit;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getImports() {
        return imports;
    }

    public void setImports(String imports) {
        this.imports = imports;
    }

    public List<FieldIntent> getFields() {
        return fields;
    }

    public void setFields(List<FieldIntent> fields) {
        this.fields = fields == null ? new ArrayList<>() : fields;
    }

    public List<RelationIntent> getRelations() {
        return relations;
    }

    public void setRelations(List<RelationIntent> relations) {
        this.relations = relations == null ? new ArrayList<>() : relations;
    }

    /** Whether this entity keeps per-language values in a sibling language table. */
    public boolean isMultilingual() {
        return multilingual != null && multilingual;
    }

    public Boolean getMultilingual() {
        return multilingual;
    }

    public void setMultilingual(Boolean multilingual) {
        this.multilingual = multilingual;
    }
}
