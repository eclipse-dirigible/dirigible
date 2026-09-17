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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single attribute on an {@link EntityIntent}. {@link #type} carries a logical type string
 * ({@code string}, {@code integer}, {@code decimal}, {@code boolean}, {@code date},
 * {@code timestamp}, {@code uuid}, {@code text}, {@code month}, {@code week}) that the entity
 * generator maps to JDBC and EDM types. {@code month} ({@code YYYY-MM}) and {@code week}
 * ({@code YYYY-Www}) are stored as strings and render as the Harmonia month / week pickers.
 */
public class FieldIntent {

    private String name;
    private String type;
    /**
     * Optional display label, replacing the humanized field name in every generated surface: the form
     * input's caption, the list column header, the read-only details block. It seeds the generated
     * en-US catalog entry for the property, so a translation of it is authored the same way as any
     * other label. Use it for what humanizing a name cannot produce - an acronym ({@code nationalId} as
     * "National ID", not "National Id"), a unit, a term of art.
     */
    private String label;
    /**
     * Optional label variants keyed by ISO 3166-1 alpha-2 <b>country</b> code, resolved from the
     * tenant's country ({@code DIRIGIBLE_APPLICATION_COUNTRY}) rather than from the language the user
     * reads the UI in.
     *
     * <p>
     * A national identification number is called ЕГН in Bulgaria and Steuer-ID in Germany: which term
     * applies is a property of the company, not of the reader. Keying such a label off the language
     * catalogs gets it wrong in both directions - an English-speaking accountant at a Bulgarian company
     * sees the generic term, their Bulgarian colleague the local one, on the same record (dirigible
     * #6424). The variant therefore overrides the label for EVERY language, and {@link #label} (or the
     * humanized name) remains the fallback for a country that declares none.
     */
    private Map<String, String> countryLabels = new LinkedHashMap<>();
    private boolean required;
    private boolean primaryKey;
    private boolean generated;
    private boolean unique;
    private Integer length;
    /** Override for the default DECIMAL precision (16) - the total number of digits. */
    private Integer precision;
    /** Override for the default DECIMAL scale (2) - the digits after the decimal point. */
    private Integer scale;
    private String defaultValue;
    private String description;
    /**
     * Optional expression evaluated to populate this property on create. When set, the property is
     * marked calculated and the generated repository assigns it on insert. The expression is emitted
     * verbatim into the chosen runtime (it must be valid in that language - e.g. a Java expression for
     * the Java DAO).
     */
    private String calculatedOnCreate;
    /**
     * Optional expression evaluated to populate this property on update (see
     * {@link #calculatedOnCreate}).
     */
    private String calculatedOnUpdate;
    /**
     * Optional fully-qualified Java class that computes this property on create, as the server-side
     * call-out alternative to {@link #calculatedOnCreate}. The class is a {@code @Component}
     * implementing {@code org.eclipse.dirigible.sdk.db.CalculatedField}; the generated repository
     * invokes it via {@code Beans.get(<class>.class).calculate(entity)} and it takes precedence over
     * the expression. Reference the class from the owning entity's {@link EntityIntent#getImports()}.
     */
    private String calculatedActionOnCreate;
    /**
     * Optional fully-qualified Java class that computes this property on update (see
     * {@link #calculatedActionOnCreate}).
     */
    private String calculatedActionOnUpdate;
    /**
     * Render hint: a document (header-items) layout shows this property in the right-aligned totals
     * footer under the items table, not in the header form. Typically a calculated numeric total
     * ({@code net} / {@code vat} / {@code total}). Purely presentational - the value is produced by the
     * calculated-field expressions ({@link #calculatedOnCreate} / {@link #calculatedOnUpdate}).
     */
    private boolean aggregate;
    /**
     * Whether the field is read-only in generated forms: shown in the read-only details block (Label:
     * Value) above the action buttons rather than as an editable input. System fields
     * ({@code ProcessId}, the audit columns, {@code uuid}) are flagged automatically by the EDM
     * generator; set this on any other field (e.g. a workflow-managed {@code status}) to do the same.
     */
    private boolean readOnly;

    /**
     * Hidden from the personal (my) surface entirely: absent from its pages and stripped from the
     * personal REST controller's responses; write attempts are ignored. The power surface is
     * unaffected. Not valid on the primary key, the identity field, or the personal FK.
     */
    private boolean sensitive;

    /**
     * Role allow-list scoping this field on EVERY generated surface: the value is stripped from the
     * REST responses and ignored on writes unless the caller holds one of the listed roles, and the
     * generated UI leaves out the column / input for everyone else. An allow-list on purpose - a role
     * added to the application later sees nothing until it is listed here, and a misspelled role hides
     * the value rather than exposing it. Every listed role must be declared in the intent's
     * {@code permissions}. Empty (the default) = visible to every caller who may read the entity.
     */
    private List<String> visibleTo = new ArrayList<>();
    /**
     * Document role: marks this field as the document's <b>number/title</b> (widget {@code
     * DOCUMENT_NUMBER}). In the document (header-items) layout the value is shown in the form's title
     * (e.g. {@code SALES INVOICE 00001231}) instead of as an editable field.
     */
    private boolean documentTitle;

    /** Explicit field role selecting a document slot - currently {@code DocumentTitle}. */
    private String function;
    /**
     * Chat role: marks this field (on a document's line-items child) as the <b>message body</b> when
     * the master declares {@code documentItemsLayout: chat}. The document view then renders each item
     * as a chat bubble whose text is this field; the author and timestamp come from the child's audit
     * columns.
     */
    private boolean messageBody;
    /**
     * Chat role: an optional boolean field (on the chat items child) flagging an <b>internal</b> memo -
     * the bubble is tinted distinctly (and hidden from the external partner surface). External by
     * default.
     */
    private boolean messageInternal;
    /**
     * Whether the field appears as a column in the entity <b>list</b> table (the model's
     * {@code widgetIsMajor}). Defaults to {@code true}; set {@code major: false} to keep the field off
     * the list/table view (it is still shown in forms and the record details pane). {@code Boolean}
     * (nullable) so an unset value keeps the default-true behaviour.
     */
    private Boolean major;
    /**
     * Whether this string field participates in the sibling <code>&lt;TABLE&gt;_LANG</code> translation
     * table of a {@code multilingual} entity. Defaults to {@code true} - on a multilingual entity every
     * character-typed property is translatable, which is right for a label and wrong for a <b>key</b>:
     * a code a determination rule matches on, a business key an arrival resolves a relation by. Such a
     * value is not text to be translated but the identity of the row, and translating it breaks the
     * match silently - the read overlay hands the UI the translated value, saving the form writes it
     * back into the BASE column, and from then on nothing matches the literal the model was authored
     * with (dirigible #6545). {@code translatable: false} keeps the field out of the language table
     * altogether, so it has no translation to overlay anywhere: not in a read, not in a report column,
     * not in a translation seed. {@code Boolean} (nullable) so an unset value keeps the default-true
     * behaviour.
     */
    private Boolean translatable;
    /**
     * Optional form-control width as a 12-column grid span (3/4/6/12: 3 = quarter, 4 = third, 6 = half,
     * 12 = full). Emitted as the property's {@code widgetSize}; the Harmonia form maps it to
     * {@code grid-column: span N}. Absent (the default) leaves it unset (the form falls back to half
     * width). Textarea/checkbox widgets always span the full row regardless.
     */
    private Integer size;

    /**
     * Optional named input format for a {@code string} field - a preset over {@link #pattern}. Today
     * the only value is {@code email}, which selects the EMAIL widget in the generated UI (so the input
     * is a {@code type="email"} control) and supplies the canonical address regex, giving the same
     * server-side 400 and client-side feedback an authored {@code pattern} would. Mutually exclusive
     * with {@code pattern}: declaring both would leave which one wins ambiguous.
     */
    private String format;

    /**
     * Optional input-format regex for a {@code string} / {@code text} field (IBAN, VAT number,
     * postcode, e-mail, ...). Emitted as the property's {@code widgetPattern}: the generated REST
     * controller rejects a non-matching value with 400, and the form input carries it as an HTML
     * {@code pattern} so the user is told before submitting.
     *
     * <p>
     * Restricted to string-typed fields on purpose - on a numeric property {@code widgetPattern} is the
     * DISPLAY format ({@code ModelParameterProcessor} reads it as {@code formatPattern}), so a regex
     * there would silently corrupt how the number renders.
     */
    private String pattern;

    /**
     * Optional Depends-On declaration: this scalar field is <b>auto-populated</b> from a property of a
     * sibling to-one relation's target when that relation's selection changes (e.g. {@code price}
     * copied from the chosen {@code Product}). Requires {@code valueFrom}; {@code filterBy} is not
     * applicable to a scalar. Emitted as the {@code widgetDependsOn*} attributes. See
     * {@link DependsOnIntent}.
     */
    private DependsOnIntent dependsOn;

    /**
     * Optional first-class document numbering: the platform maintains a per-series counter and stamps
     * the formatted number onto this string field (at create or at a modeled issue step). Replaces the
     * hand-written placeholder action + {@code generateNumber} service-task delegate. See
     * {@link NumberIntent}.
     */
    private NumberIntent number;

    public String getFormat() {
        return format;
    }

    public String getPattern() {
        return pattern;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Map<String, String> getCountryLabels() {
        return countryLabels;
    }

    public void setCountryLabels(Map<String, String> countryLabels) {
        this.countryLabels = countryLabels == null ? new LinkedHashMap<>() : countryLabels;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean isGenerated() {
        return generated;
    }

    public void setGenerated(boolean generated) {
        this.generated = generated;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(Integer precision) {
        this.precision = precision;
    }

    public Integer getScale() {
        return scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    public String getCalculatedOnCreate() {
        return calculatedOnCreate;
    }

    public void setCalculatedOnCreate(String calculatedOnCreate) {
        this.calculatedOnCreate = calculatedOnCreate;
    }

    public String getCalculatedOnUpdate() {
        return calculatedOnUpdate;
    }

    public void setCalculatedOnUpdate(String calculatedOnUpdate) {
        this.calculatedOnUpdate = calculatedOnUpdate;
    }

    public String getCalculatedActionOnCreate() {
        return calculatedActionOnCreate;
    }

    public void setCalculatedActionOnCreate(String calculatedActionOnCreate) {
        this.calculatedActionOnCreate = calculatedActionOnCreate;
    }

    public String getCalculatedActionOnUpdate() {
        return calculatedActionOnUpdate;
    }

    public void setCalculatedActionOnUpdate(String calculatedActionOnUpdate) {
        this.calculatedActionOnUpdate = calculatedActionOnUpdate;
    }

    /** Whether this property is computed (has a create/update expression or a create/update action). */
    public boolean isCalculated() {
        return (calculatedOnCreate != null && !calculatedOnCreate.isBlank())
                || (calculatedOnUpdate != null && !calculatedOnUpdate.isBlank())
                || (calculatedActionOnCreate != null && !calculatedActionOnCreate.isBlank())
                || (calculatedActionOnUpdate != null && !calculatedActionOnUpdate.isBlank());
    }

    public boolean isAggregate() {
        return aggregate;
    }

    public void setAggregate(boolean aggregate) {
        this.aggregate = aggregate;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    /**
     * Whether this field is the document's title/number - the explicit {@code function: DocumentTitle}
     * or the legacy {@code documentTitle: true}.
     */
    public boolean isDocumentTitle() {
        return documentTitle || (function != null && "DocumentTitle".equalsIgnoreCase(function.trim()));
    }

    /** Whether this field is the chat message body ({@code messageBody: true}). */
    public boolean isMessageBody() {
        return messageBody;
    }

    public void setMessageBody(boolean messageBody) {
        this.messageBody = messageBody;
    }

    /** Whether this field is the chat internal/external flag ({@code messageInternal: true}). */
    public boolean isMessageInternal() {
        return messageInternal;
    }

    public void setMessageInternal(boolean messageInternal) {
        this.messageInternal = messageInternal;
    }

    public void setDocumentTitle(boolean documentTitle) {
        this.documentTitle = documentTitle;
    }

    public Boolean getMajor() {
        return major;
    }

    public void setMajor(Boolean major) {
        this.major = major;
    }

    /** Whether this field is a list-table column - defaults to true when {@code major} is unset. */
    public boolean isMajor() {
        return major == null || major;
    }

    public Boolean getTranslatable() {
        return translatable;
    }

    public void setTranslatable(Boolean translatable) {
        this.translatable = translatable;
    }

    /**
     * Whether the field is translatable at all - defaults to true when {@code translatable} is unset.
     *
     * @return false only when {@code translatable: false} is authored
     */
    public boolean isTranslatable() {
        return translatable == null || translatable;
    }

    /**
     * Whether a {@code multilingual} entity's language table carries a column for this field - the one
     * predicate every consumer of the translation overlay must agree on: the schema template that EMITS
     * the language column, the report generator that overlays it, the CSVIM generator that seeds it,
     * and the parser that refuses a translation seeding what has no column. A character-typed field
     * that is neither the primary key (the language row's own reference to the base row) nor calculated
     * (assigned by the repository on every write) nor marked {@code translatable: false}.
     *
     * <p>
     * Says nothing about the owning entity: a field of a non-multilingual entity has no language table
     * to have a column in, so the caller pairs this with {@code entity.isMultilingual()}.
     *
     * @return true when the field's value can carry a per-language translation
     */
    public boolean hasLanguageColumn() {
        if (primaryKey || isCalculated() || !isTranslatable()) {
            return false;
        }
        String logical = type == null ? "string" : type.toLowerCase(java.util.Locale.ROOT);
        return "string".equals(logical) || "text".equals(logical);
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public DependsOnIntent getDependsOn() {
        return dependsOn;
    }

    public NumberIntent getNumber() {
        return number;
    }

    public void setNumber(NumberIntent number) {
        this.number = number;
    }

    public void setDependsOn(DependsOnIntent dependsOn) {
        this.dependsOn = dependsOn;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public List<String> getVisibleTo() {
        return visibleTo;
    }

    public void setVisibleTo(List<String> visibleTo) {
        this.visibleTo = visibleTo == null ? new ArrayList<>() : visibleTo;
    }
}
