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
import java.util.List;
import java.util.Map;

/**
 * A "create-from" (document generation) declaration: an on-demand action that clones a source
 * record ({@link #from}) into a new target record ({@link #to}), possibly in another model
 * ({@link #uses}) - e.g. generate a {@code SalesInvoice} from a {@code ProjectTimesheet}.
 *
 * <p>
 * The trigger is a click, an {@link #event} of the source, or both - see {@link #event} for the
 * at-most-once semantics an event-driven create-from carries.
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
 *
 * <p>
 * The composition line-items of the target are filled by exactly one of two mutually-exclusive
 * shapes:
 * <ul>
 * <li>{@link #items} (an OBJECT) - the <b>mirror</b> form: for each source item row a target item
 * row is created and its cells copied ({@code map}) / defaulted ({@code defaults}); and</li>
 * <li>{@link #itemLines} (a LIST) - the <b>computed</b> form (issue #6555): a fixed set of
 * synthetic target lines whose cells are EXPRESSIONS over the SOURCE record - numeric arithmetic
 * evaluated through {@code Calc} (as calculated fields / posting item amounts are), {@code {field}}
 * string interpolation, a source foreign-key copy, or a {@code now}/literal. This expresses the
 * "one line for the period's rolled-up total" invoice a create-from could not build before.</li>
 * </ul>
 */
public class GeneratesIntent {

    /**
     * The default event cardinality: at most one target row per source, guarded by the back-reference.
     */
    public static final String MODE_ONCE = "once";

    /** The opt-in event cardinality: one target row per delivered event, with no guard at all. */
    public static final String MODE_APPEND = "append";

    /**
     * Unique name within the model; drives the action id, the contribution files and the controller.
     */
    private String name;

    /**
     * The source entity, loaded by the selected record's id. Lives in THIS model unless
     * {@link #fromUses} names the model that owns it.
     */
    private String from;

    /**
     * Optional model alias (from the model's {@code uses:} list) the {@link #from} entity lives in.
     * Blank means the source is a local entity of this model (the default, fully backward compatible).
     *
     * <p>
     * A cross-model source lets the create-from be authored on the module that owns the TARGET, which
     * is what breaks a mutual compile dependency between two modules: without it, "A generates into B"
     * must be authored in A - so A's generated controller references B's entities while B already
     * references A's, and neither module can be compiled (or packaged as a jar) before the other. The
     * mirror of the cross-model {@code entity}/{@code model} source a {@code schedules} block accepts
     * (issue #6532).
     */
    private String fromUses;

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

    /**
     * Optional event trigger (issue #6711): the create-from runs by itself when the SOURCE reaches a
     * moment, instead of waiting for a click. Exactly one of two axes, the same pair the rest of the
     * declarative glue binds to:
     * <ul>
     * <li>the source's <b>lifecycle</b> - {@code onTransition} (a status write; a {@code when} status
     * guard is mandatory) or {@code onCreate} (the source's insert; the guard is optional), naming the
     * source entity - the same one {@link #from} declares, repeated for symmetry with {@code postings}'
     * event axis and validated against it; and</li>
     * <li>a <b>process step</b> (issue #6800) - {@code onStepReached} / {@code onStepCompleted}:
     * <code>{ process, step }</code>, the axis {@code notifications} / {@code integrations} /
     * {@code outbound} already use, whose record is the process's trigger entity (which must be
     * {@link #from}). The {@code when} guard stays optional: the step IS the moment.</li>
     * </ul>
     * The owning model is NOT repeated here: {@link #fromUses} already declares it.
     *
     * <p>
     * {@code mode} declares the <b>cardinality</b>. The default {@code once} is at-most-once: the
     * target's back-reference to the source (the {@link #map} entry copying the source's primary key)
     * is checked before anything is created, so an event redelivery - and a click on a button that is
     * still declared - is a no-op that returns the document that already exists. {@code append} drops
     * that lookup, so every delivery of the event creates a row: the "one log/protocol row per step,
     * per transition" shape. It is the ABSENCE of a guard, not a state-aware one - a redelivery appends
     * a duplicate, and a replacement for a voided target is not what it expresses. The back-reference
     * is required in BOTH modes: the dedup key in {@code once}, the row's provenance in {@code append}.
     */
    private Map<String, Object> event;

    /**
     * Whether the client button is contributed. Defaults to {@code true} without an {@link #event} (a
     * create-from with no trigger at all would generate nothing) and to {@code false} with one: the
     * point of declaring an event is that no one has to click. Set it explicitly to {@code true} to
     * keep both affordances - the button then shares the event's at-most-once guard.
     */
    private Boolean button;

    /** Optional ordering hint among the contributed actions of a view. */
    private Integer order;
    /**
     * Optional completion hook: the EntityStatus seed id the SOURCE record is set to after the target
     * is created (e.g. a proforma flips to INVOICED once the invoice exists). A workflow-style system
     * write - no {@code -updated} re-fire, but the source's {@code -transitioned} topic IS published.
     * Requires the {@code from} entity to declare a {@code function: EntityStatus} relation.
     */
    private Integer sourceStatus;

    /**
     * The INVERSE of {@link #sourceStatus} (issue #6868): the seed id the SOURCE returns to when the
     * target generated from it is RETIRED - reaches a status its nomenclature classifies {@code
     * cancelled} or {@code void}. Void and reissue, declared.
     *
     * <p>
     * Why it is needed at all: {@link #sourceStatus} moves the source off the status its own
     * {@code event} guard qualifies on, deliberately, so the guard-claimed source stops matching. The
     * at-most-once guard learned to step over a retired target (issue #6814), which frees the source's
     * one-shot slot - but nothing could refill it: the source stands at its post-generation status and
     * the ordinary lifecycle graph declares no edge back, so no qualifying {@code -transitioned} is
     * ever published again and an event-only create-from had no reissue path at all.
     *
     * <p>
     * This declares the move back. The retirement of the target flips the source to this status through
     * the same targeted primitive the completion hook uses, publishing the source's
     * {@code -transitioned} with the write - so the ordinary trigger re-fires, the guard steps over the
     * retired document, and the replacement is minted. Nothing about the reissue is a special path: it
     * is the source's own lifecycle move plus the machinery that was already there. The retired
     * document is kept, never edited or re-pointed.
     *
     * <p>
     * It is opt-in and refused where it could never fire (see
     * {@code IntentParser.validateGeneratesReopen}): it requires an {@link #event} to re-fire and
     * {@link #sourceStatus} to invert, must name a status other than that one, needs a LOCAL target
     * whose nomenclature classifies a retiring {@code stage:}, and - when the source declares a
     * {@code lifecycle:} - needs that graph to declare the edge back.
     */
    private Integer sourceStatusOnRetire;

    /** Target property -> source property (a field or to-one relation name of {@link #from}). */
    private Map<String, String> map = new LinkedHashMap<>();

    /** Target property -> {@code now} or a literal value (string / integer / decimal / boolean). */
    private Map<String, String> defaults = new LinkedHashMap<>();

    /**
     * Optional composition child mapping (the source document's items -> the target document's items) -
     * the MIRROR form. Mutually exclusive with {@link #itemLines}.
     */
    private GeneratesItemsIntent items;

    /**
     * Optional computed line-items (issue #6555) - the COMPUTED form. Each element is one synthetic
     * target line: a cell key names a field or to-one relation of the target document's line-items
     * child, and its value is an expression over the SOURCE record (a numeric {@code Calc} arithmetic
     * expression, a {@code {field}}-interpolated string, a source foreign-key copy, or a
     * {@code now}/literal); an optional {@code when} cell guards the whole line. Mutually exclusive
     * with {@link #items}. The parser moves a list-valued {@code items:} here so the two shapes stay
     * typed.
     */
    private List<Map<String, String>> itemLines;

    /**
     * Scheduled generation only: child blocks generated under the created target - one child per
     * element of a source collection (a matching LOCAL entity's rows, or the working days of the
     * month). See {@code GenerateChildIntent}.
     */
    private java.util.List<GenerateChildIntent> children;

    /**
     * Optional declared input form (issue #6685): a small set of the TARGET's properties the user
     * supplies before the target is created - the values that cannot be derived from the source (which
     * payment, how much). Entries name fields / to-one relations of {@link #to}; the values are posted
     * with the source id and set on the target after {@link #map} / {@link #defaults}. See
     * {@link PromptFieldIntent}.
     */
    private List<PromptFieldIntent> prompt;

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

    public String getFromUses() {
        return fromUses;
    }

    public void setFromUses(String fromUses) {
        this.fromUses = fromUses;
    }

    /** Whether the {@link #from} entity is owned by another model (see {@link #getFromUses()}). */
    public boolean isCrossModelSource() {
        return fromUses != null && !fromUses.isBlank();
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

    public Map<String, Object> getEvent() {
        return event;
    }

    public void setEvent(Map<String, Object> event) {
        this.event = event;
    }

    /** Whether this create-from is triggered by a source event rather than only by a click. */
    public boolean isEventDriven() {
        return event != null && !event.isEmpty();
    }

    /**
     * The declared cardinality of the event trigger (see {@link #event}).
     *
     * @return {@link #MODE_APPEND} when the author asked for a row per event, {@link #MODE_ONCE}
     *         otherwise (the default, and the value for a create-from with no event at all)
     */
    public String getEventMode() {
        Object mode = event == null ? null : event.get("mode");
        String declared = mode == null ? null
                : mode.toString()
                      .trim();
        return declared == null || declared.isEmpty() ? MODE_ONCE : declared;
    }

    /**
     * @return whether every delivery of the event appends a target row (no at-most-once guard)
     */
    public boolean isAppendMode() {
        return MODE_APPEND.equals(getEventMode());
    }

    public Boolean getButton() {
        return button;
    }

    public void setButton(Boolean button) {
        this.button = button;
    }

    /** Whether the client button is contributed (see {@link #button}). */
    public boolean hasButton() {
        return button == null ? !isEventDriven() : button;
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

    public Integer getSourceStatusOnRetire() {
        return sourceStatusOnRetire;
    }

    public void setSourceStatusOnRetire(Integer sourceStatusOnRetire) {
        this.sourceStatusOnRetire = sourceStatusOnRetire;
    }

    /**
     * Whether a retired target returns the source to a status of its own (see
     * {@link #sourceStatusOnRetire}).
     */
    public boolean hasReopen() {
        return sourceStatusOnRetire != null;
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

    public List<Map<String, String>> getItemLines() {
        return itemLines;
    }

    public void setItemLines(List<Map<String, String>> itemLines) {
        this.itemLines = itemLines;
    }

    public java.util.List<GenerateChildIntent> getChildren() {
        return children;
    }

    public void setChildren(java.util.List<GenerateChildIntent> children) {
        this.children = children;
    }

    public List<PromptFieldIntent> getPrompt() {
        return prompt;
    }

    public void setPrompt(List<PromptFieldIntent> prompt) {
        this.prompt = prompt;
    }

    /** Whether this action declares a {@code prompt:} input form. */
    public boolean hasPrompt() {
        return prompt != null && !prompt.isEmpty();
    }
}
