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

import com.google.gson.annotations.SerializedName;

/**
 * Domain entity declaration in an {@code .intent}. Generates one EDM entity, one DSM table, one TS
 * or Java entity decorator, and a default repository / controller pair downstream.
 */
public class EntityIntent {

    private String name;
    private String description;
    /**
     * Optional {@code extends:} declaration. When present, this entity is an EXTENSION: it owns no
     * table of its own; its fields are contributed to the referenced base entity (folded into the base
     * table at generation). Authored as {@code extends: { model: <base-model>, entity: <base> }}
     * ({@code extends} is a Java keyword, so the field is {@code extend} bound to the YAML key).
     */
    @SerializedName("extends")
    private ExtendsIntent extend;
    /**
     * Optional entity classification. {@code setting} marks the entity as nomenclature / configuration
     * data: the EDM generator emits it with {@code type="SETTING"} so the template engine routes it
     * under the dashboard's Settings menu instead of generating a top-level perspective. Absent (the
     * default) means a regular managed entity.
     */
    private String kind;

    /**
     * Explicit presentation role that selects the entity's UI template - {@code Document},
     * {@code DocumentItem}, {@code Master}, {@code Detail}, {@code List}, {@code Setting}, or
     * {@code Calendar} (the role alias for {@code view: calendar} - the entity renders on the Harmonia
     * calendar, configured by the {@code calendar:} block; reserved values such as
     * {@code Board}/{@code Gantt} stay gated). Authoritative when set; otherwise the role is inferred
     * from structure and the legacy flags ({@code kind: setting}, an {@code *Item}-named child).
     */
    private String function;
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
     * Marks a document entity as <b>duplicable</b>: its generated document view shows a built-in
     * Duplicate action that clones the current record (header + composition line items) into a new
     * draft and opens it. The clone creates through the normal REST create path, so the number
     * ({@code calculatedActionOnCreate}), the initial status ({@code init}) and calculated fields are
     * reassigned by the server. Absent (the default) → no Duplicate action.
     */
    private Boolean duplicable;
    /**
     * Optional explicit ordering of the generated UI controls (form inputs, list columns, detail rows)
     * by property name - fields and to-one relations interleaved, in the given order. Names match the
     * authored field / relation names (case-insensitive). Any property not listed keeps its default
     * position, appended after the listed ones. Absent → the default order (fields in declaration
     * order, then to-one relations), which pushes all relations to the end.
     */
    private List<String> order = new ArrayList<>();
    /**
     * Optional UI view type for this entity. {@code calendar} renders its records as events on a
     * Harmonia calendar (see {@link #calendar}); absent → the default list / manage / document layout
     * inferred from structure.
     */
    private String view;
    /**
     * Optional rendering of a document master's line-items pane. Default (unset) renders the items as
     * an editable table; {@code documentItemsLayout: chat} renders them as a conversation thread
     * (bubbles + a composer) - the document header/status/process stay intact. Only meaningful on a
     * document master.
     */
    private String documentItemsLayout;
    /** Calendar-view configuration; used when {@code view: calendar} or {@code view: range}. */
    private CalendarIntent calendar;
    /** Slots-view configuration; used when {@code view: slots}. */
    private SlotsIntent slots;
    private List<FieldIntent> fields = new ArrayList<>();
    private List<RelationIntent> relations = new ArrayList<>();
    /**
     * RETIRED spelling, bound only so the parser can reject it with a migration message: status-scoped
     * immutability is authored as {@code immutableWhen} since the rename.
     */
    private List<Integer> immutableIn;
    /**
     * Optional status-scoped immutability: a boolean expression over the entity's
     * {@code function: EntityStatus} relation - terms {@code <Status> == <seed id>} joined with
     * {@code ||} - while true the record is READ-ONLY for user writes (e.g.
     * {@code immutableWhen: "Status == 2"} = a POSTED journal entry can no longer be edited or deleted
     * through the REST surface). Workflow/system writes through the repository (storno generation,
     * roll-ups) are deliberately unaffected - this guards the USER surface. Requires the entity to
     * declare a {@code function: EntityStatus} relation.
     */
    private String immutableWhen;
    /**
     * Optional append-only marker: {@code immutable: true} makes every record READ-ONLY for user writes
     * from the moment it is created (audit trails, ledger lines, event logs) - REST update and delete
     * always return 409. System writes through the repository stay possible. Mutually exclusive with
     * {@code immutableWhen} - always-immutable subsumes any status scope.
     */
    private Boolean immutable;
    /**
     * Optional hierarchy declaration: names this entity's to-one SELF-relation that forms the tree edge
     * (e.g. {@code hierarchy: Parent} on a chart-of-accounts Account). The generated list renders as a
     * tree, relations targeting this entity get a hierarchy-aware picker, and {@code leafOnly}
     * references become valid. Explicit by design - a self-FK alone does not imply a hierarchy.
     */
    private String hierarchy;

    /**
     * Names the field of THIS entity matched against the logged-in username to resolve the current
     * user's record (e.g. {@code identity: email}) - the mapping that personal surfaces are scoped by.
     * Declared once, on the entity that represents the person; consumers referencing it cross-model
     * inherit the mapping through the resolved model.
     */
    private String identity;

    /**
     * A display-label expression - literals plus {@code &#123;field&#125;} /
     * {@code &#123;Relation.field&#125;} tokens (one hop; {@code |format} applies a date pattern to
     * temporal values). Generates a stored, read-only {@code Name} property recomputed by the
     * repository on every write, so lookups and dropdowns show it everywhere. Compose across hops by
     * referencing the related entity's own generated label: {@code &#123;Parent.Name&#125;}.
     */
    private String label;
    /**
     * Optional declarative validations - the cross-field / cross-line rules plain field attributes
     * cannot express: {@code exactlyOne} (row-level one-of), {@code itemsSumEqual} (the document's
     * items balance - the double-entry invariant) and {@code itemsMin} (minimum line count), the latter
     * two gated on an EntityStatus seed id so drafting stays unconstrained. See {@link CheckIntent}.
     */
    private List<CheckIntent> checks;


    public String getName() {
        return name;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    private boolean viewIs(String role) {
        return view != null && role.equalsIgnoreCase(view.trim());
    }

    /**
     * Whether this entity uses the calendar renderer - {@code view: calendar}, {@code view: range}, or
     * the presentation role {@code function: Calendar} (the role alias for {@code view: calendar}; the
     * {@code calendar:} block configures it either way).
     */
    public boolean isCalendar() {
        return viewIs("calendar") || viewIs("range") || functionIs("Calendar");
    }

    /**
     * Whether this entity is a date-range/span calendar ({@code view: range}) - events span start..end,
     * all-day.
     */
    public boolean isRange() {
        return viewIs("range");
    }

    /** Whether this entity is rendered as a slot picker ({@code view: slots}). */
    public boolean isSlots() {
        return viewIs("slots");
    }

    public String getDocumentItemsLayout() {
        return documentItemsLayout;
    }

    public void setDocumentItemsLayout(String documentItemsLayout) {
        this.documentItemsLayout = documentItemsLayout;
    }

    /**
     * Whether this document master renders its line-items as a chat thread ({@code documentItemsLayout:
     * chat}) instead of the default editable table.
     */
    public boolean isChatItems() {
        return documentItemsLayout != null && "chat".equalsIgnoreCase(documentItemsLayout.trim());
    }

    public CalendarIntent getCalendar() {
        return calendar;
    }

    public void setCalendar(CalendarIntent calendar) {
        this.calendar = calendar;
    }

    public SlotsIntent getSlots() {
        return slots;
    }

    public void setSlots(SlotsIntent slots) {
        this.slots = slots;
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

    public ExtendsIntent getExtend() {
        return extend;
    }

    public void setExtend(ExtendsIntent extend) {
        this.extend = extend;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    /** Case-insensitive test that the entity's {@code function} equals the given role. */
    private boolean functionIs(String role) {
        return function != null && role.equalsIgnoreCase(function.trim());
    }

    /**
     * Whether this entity is declared as a setting / nomenclature - the explicit
     * {@code function: Setting}, or the legacy {@code kind: setting}.
     */
    public boolean isSetting() {
        return functionIs("Setting") || "setting".equalsIgnoreCase(kind);
    }

    /** Whether this entity is explicitly declared a document master ({@code function: Document}). */
    public boolean isDocument() {
        return functionIs("Document");
    }

    /**
     * Whether this entity is explicitly declared the line-items of its composition parent document
     * ({@code function: DocumentItem}) - the explicit form of the legacy {@code *Item} naming.
     */
    public boolean isDocumentItem() {
        return functionIs("DocumentItem");
    }

    /**
     * Whether this entity is a file-attachment child ({@code function: Attachment}) - a composition
     * detail of its master holding one uploaded file's metadata (the bytes live in the CMS). The EDM
     * generator injects the standard attachment columns; the generated controller and Harmonia view
     * render it as an "Attachments" section.
     */
    public boolean isAttachment() {
        return functionIs("Attachment");
    }

    /**
     * Whether this entity is a generated-copy child ({@code function: Snapshot}) - a composition detail
     * holding one system-generated, immutable, versioned file (e.g. the printed invoice stored on
     * issue). Like an attachment it carries the standard file-metadata columns and renders in the Files
     * panel, but read-only (download + list only; created server-side, never uploaded or deleted by the
     * user) and with an added {@code Version} column.
     */
    public boolean isSnapshot() {
        return functionIs("Snapshot");
    }

    /**
     * Whether this entity is any file-child kind (attachment or snapshot) - shares metadata injection.
     */
    public boolean isFileChild() {
        return isAttachment() || isSnapshot();
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

    public List<Integer> getImmutableIn() {
        return immutableIn;
    }

    public void setImmutableIn(List<Integer> immutableIn) {
        this.immutableIn = immutableIn;
    }

    public String getImmutableWhen() {
        return immutableWhen;
    }

    public void setImmutableWhen(String immutableWhen) {
        this.immutableWhen = immutableWhen;
    }

    public Boolean getImmutable() {
        return immutable;
    }

    public void setImmutable(Boolean immutable) {
        this.immutable = immutable;
    }

    public String getHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(String hierarchy) {
        this.hierarchy = hierarchy;
    }

    public List<CheckIntent> getChecks() {
        return checks;
    }

    public void setChecks(List<CheckIntent> checks) {
        this.checks = checks;
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

    /**
     * Whether this entity's document view offers the built-in Duplicate action
     * ({@code duplicable: true}).
     */
    public boolean isDuplicable() {
        return Boolean.TRUE.equals(duplicable);
    }

    public Boolean getDuplicable() {
        return duplicable;
    }

    public void setDuplicable(Boolean duplicable) {
        this.duplicable = duplicable;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
