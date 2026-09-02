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

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the derivation pass that turns a model into generation parameters.
 */
class ModelParameterProcessorTest {

    @Test
    void coercesTheModelsStringFlagsToBooleans() {
        Map<String, Object> property = property("Id", "INTEGER");
        property.put("dataPrimaryKey", "true");
        property.put("dataAutoIncrement", "true");
        property.put("dataNullable", "false");
        property.put("isRequiredProperty", "false");
        Map<String, Object> model = model(entity("Book", "Books", property));

        ModelParameterProcessor.process(model, parameters());

        assertEquals(Boolean.TRUE, property.get("dataPrimaryKey"));
        assertEquals(Boolean.TRUE, property.get("dataAutoIncrement"));
        assertEquals(Boolean.FALSE, property.get("dataNullable"));
        assertEquals(Boolean.FALSE, property.get("isRequiredProperty"));
        // Derived from the original string, before it was replaced by its boolean.
        assertEquals(Boolean.TRUE, property.get("dataNotNull"));
    }

    @Test
    void aFlagTheModelOmitsBecomesFalse() {
        Map<String, Object> property = property("Name", "VARCHAR");
        ModelParameterProcessor.process(model(entity("Book", "Books", property)), parameters());

        assertEquals(Boolean.FALSE, property.get("dataPrimaryKey"));
        assertEquals(Boolean.FALSE, property.get("widgetIsMajor"));
    }

    /**
     * The one flag the generated forms, lists and details blocks consult to leave bookkeeping out: the
     * model may set it (a roll-up's displaced status), and the per-process stamps carry it by name, so
     * a model written before the flag existed still hides them.
     */
    @Test
    void hidesFlaggedBookkeepingAndTheProcessStamps() {
        Map<String, Object> flagged = property("DisplacedStatus", "INTEGER");
        flagged.put("isHiddenProperty", "true");
        Map<String, Object> stamps = property("ProcessIds", "VARCHAR");
        Map<String, Object> plain = property("Name", "VARCHAR");
        ModelParameterProcessor.process(model(entity("Invoice", "Invoices", flagged, stamps, plain)), parameters());

        assertEquals(Boolean.TRUE, flagged.get("isHiddenProperty"));
        assertEquals(Boolean.TRUE, stamps.get("isHiddenProperty"));
        assertEquals(Boolean.FALSE, plain.get("isHiddenProperty"));
    }

    @Test
    void defaultsTheWidgetLabelFromThePropertyName() {
        Map<String, Object> property = property("TaxEventDate", "DATE");
        ModelParameterProcessor.process(model(entity("Book", "Books", property)), parameters());

        assertEquals("Tax Event Date", property.get("widgetLabel"));
    }

    @Test
    void keepsAnAuthoredWidgetLabel() {
        Map<String, Object> property = property("TaxEventDate", "DATE");
        property.put("widgetLabel", "Tax point");
        ModelParameterProcessor.process(model(entity("Book", "Books", property)), parameters());

        assertEquals("Tax point", property.get("widgetLabel"));
    }

    @Test
    void marksTemporalAndNumericPropertiesForTheirWidgets() {
        Map<String, Object> date = property("Issued", "DATE");
        Map<String, Object> amount = property("Amount", "DECIMAL");
        Map<String, Object> entity = entity("Invoice", "Invoices", date, amount);
        ModelParameterProcessor.process(model(entity), parameters());

        assertEquals(Boolean.TRUE, date.get("isDateType"));
        assertEquals(Boolean.TRUE, entity.get("hasDates"));
        assertEquals(Boolean.TRUE, amount.get("isNumberType"));
        assertEquals(Boolean.TRUE, amount.get("isFloatType"));
        assertEquals(Boolean.TRUE, entity.get("hasFloats"));
        assertEquals("### ### ### ##0.00", amount.get("formatPattern"));
    }

    @Test
    void everyDerivedNumberIsADoubleSoTheTemplatesSeeWhatTheyAlwaysSaw() {
        Map<String, Object> property = property("Name", "VARCHAR");
        property.put("dataLength", "40");
        property.put("widgetLength", "20");
        ModelParameterProcessor.process(model(entity("Book", "Books", property)), parameters());

        assertEquals(Double.valueOf(0), property.get("minLength"));
        assertEquals(Double.valueOf(20), property.get("maxLength"));
    }

    @Test
    void buildsTheJavaLookupUrlForARelationRenderedAsADropdown() {
        Map<String, Object> property = property("Author", "INTEGER");
        property.put("widgetType", "DROPDOWN");
        property.put("relationshipEntityName", "Author");
        property.put("relationshipEntityPerspectiveName", "Authors");
        Map<String, Object> entity = entity("Book", "Books", property);
        Map<String, Object> parameters = parameters();
        parameters.put("javaRuntime", Boolean.TRUE);

        ModelParameterProcessor.process(model(entity), parameters);

        assertEquals("/services/java/bookstore/gen/sales_order/api/authors/AuthorController", property.get("widgetDropdownUrl"));
        assertEquals("/services/java/bookstore/gen/sales_order/api/authors/AuthorController", property.get("widgetDropdownControllerUrl"));
        // The application's own assets live under the raw folder name, not the sanitized one.
        assertEquals("/services/web/bookstore/gen/sales-order/index.html", property.get("widgetDropdownAppUrl"));
        assertEquals(Boolean.TRUE, entity.get("hasDropdowns"));
    }

    @Test
    void leavesTheLookupUrlEmptyForAPlainProperty() {
        Map<String, Object> property = property("Name", "VARCHAR");
        ModelParameterProcessor.process(model(entity("Book", "Books", property)), parameters());

        assertEquals("", property.get("widgetDropdownUrl"));
        assertEquals("", property.get("widgetDropdownControllerUrl"));
    }

    /**
     * A perspective key the entity does not carry is left out rather than set to null - the original
     * assigns an absent value, which drops the key from the serialized parameters entirely.
     */
    @Test
    void aPerspectiveOmitsTheKeysTheEntityDoesNotCarry() {
        Map<String, Object> entity = entity("Book", "Books", property("Name", "VARCHAR"));
        entity.put("perspectiveOrder", "100");
        Map<String, Object> parameters = parameters();

        ModelParameterProcessor.process(model(entity), parameters);

        Map<String, Object> perspective = ModelValues.asMap(ModelValues.asMap(parameters.get("perspectives"))
                                                                       .get("Books"));
        assertEquals("100", perspective.get("order"));
        assertFalse(perspective.containsKey("header"), "an absent perspective header must not become a null one");
        assertFalse(perspective.containsKey("navId"), "an absent navigation id must not become a null one");
        assertEquals(List.of("Book"), perspective.get("views"));
    }

    /**
     * The flag the generated pages gate on: only an entity with a read-scoped property asks its
     * controller which fields the caller may not see, so an application using none of this issues no
     * such request at all.
     */
    @Test
    void flagsOnlyTheEntitiesCarryingAReadScopedProperty() {
        Map<String, Object> restricted = property("DailyRate", "DECIMAL");
        restricted.put("roleRead", "Payroll,Administrator");
        Map<String, Object> scoped = entity("Employee", "People", restricted);
        Map<String, Object> plain = entity("Author", "Authors", property("Name", "VARCHAR"));

        ModelParameterProcessor.process(model(scoped, plain), parameters());

        assertEquals(Boolean.TRUE, scoped.get("hasRestrictedFields"));
        assertFalse(plain.containsKey("hasRestrictedFields"), "an entity with no read-scoped property must not carry the flag");
    }

    @Test
    void collectsTheDefaultRolesOnlyWhereTheModelAsksForThem() {
        Map<String, Object> asking = entity("Book", "Books", property("Name", "VARCHAR"));
        asking.put("generateDefaultRoles", "true");
        asking.put("roleRead", "book-read");
        asking.put("roleWrite", "book-write");
        Map<String, Object> silent = entity("Author", "Authors", property("Name", "VARCHAR"));
        Map<String, Object> parameters = parameters();

        ModelParameterProcessor.process(model(asking, silent), parameters);

        List<Object> roles = ModelValues.asList(parameters.get("roles"));
        assertEquals(1, roles.size());
        Map<String, Object> role = ModelValues.asMap(roles.get(0));
        assertEquals("Book", role.get("entityName"));
        assertEquals("book-read", role.get("roleRead"));
        assertEquals("book-write", role.get("roleWrite"));
    }

    @Test
    void aReportContributesNoWriteRole() {
        Map<String, Object> report = entity("Revenue", "Reports", property("Total", "DECIMAL"));
        report.put("type", "REPORT");
        report.put("generateDefaultRoles", "true");
        report.put("roleRead", "revenue-read");
        report.put("roleWrite", "revenue-write");
        Map<String, Object> parameters = parameters();

        ModelParameterProcessor.process(model(report), parameters);

        Map<String, Object> role = ModelValues.asMap(ModelValues.asList(parameters.get("roles"))
                                                                .get(0));
        assertEquals("revenue-read", role.get("roleRead"));
        assertNull(role.get("roleWrite"));
    }

    @Test
    void marksTheEntityCarryingAProcessIdentifier() {
        Map<String, Object> entity = entity("Loan", "Loans", property("ProcessId", "VARCHAR"));
        ModelParameterProcessor.process(model(entity), parameters());

        assertEquals(Boolean.TRUE, entity.get("hasProcess"));
    }

    @Test
    void splitsTheDeclarativeChecksByTheScopeThatEnforcesThem() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("kind", "exactlyOne");
        Map<String, Object> guard = new LinkedHashMap<>();
        guard.put("kind", "guard");
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("kind", "requiredWhen");
        Map<String, Object> entity = entity("Invoice", "Invoices", property("Total", "DECIMAL"));
        entity.put("checks", List.of(row, guard, document));

        ModelParameterProcessor.process(model(entity), parameters());

        assertEquals(1, ModelValues.asList(entity.get("rowChecks"))
                                   .size());
        assertEquals(1, ModelValues.asList(entity.get("guardChecks"))
                                   .size());
        assertEquals(1, ModelValues.asList(entity.get("documentChecks"))
                                   .size());
    }

    @Test
    void resolvesAProjectionOwnerFromEitherReferenceForm() {
        Map<String, Object> foreignKey = property("Currency", "INTEGER");
        foreignKey.put("relationshipEntityName", "Currency");
        Map<String, Object> child = entity("InvoiceItem", "Invoices", foreignKey);
        Map<String, Object> projection = entity("Currency", "Currencies", property("Code", "VARCHAR"));
        projection.put("type", "PROJECTION");
        projection.put("projectionReferencedModel", "/codbex-currencies/currencies.model");
        ModelParameterProcessor.process(model(child, projection), parameters());

        Map<String, Object> referenced = ModelValues.asMap(ModelValues.asList(child.get("referencedProjections"))
                                                                      .get(0));
        assertEquals("codbex-currencies", referenced.get("project"));
        assertEquals("currencies", referenced.get("genFolderName"));
    }

    @Test
    void resolvesAProjectionOwnerFromTheOlderThreeSegmentReferenceToo() {
        Map<String, Object> foreignKey = property("Currency", "INTEGER");
        foreignKey.put("relationshipEntityName", "Currency");
        Map<String, Object> child = entity("InvoiceItem", "Invoices", foreignKey);
        Map<String, Object> projection = entity("Currency", "Currencies", property("Code", "VARCHAR"));
        projection.put("type", "PROJECTION");
        projection.put("projectionReferencedModel", "/workspace/codbex-currencies/currencies.model");
        ModelParameterProcessor.process(model(child, projection), parameters());

        Map<String, Object> referenced = ModelValues.asMap(ModelValues.asList(child.get("referencedProjections"))
                                                                      .get(0));
        assertEquals("codbex-currencies", referenced.get("project"));
        assertEquals("currencies", referenced.get("genFolderName"));
    }

    @Test
    void sanitizesTheGenerationFolderAndThePerspectiveIntoJavaIdentifiers() {
        Map<String, Object> entity = entity("Book", "Sales Orders", property("Name", "VARCHAR"));
        Map<String, Object> parameters = parameters();

        ModelParameterProcessor.process(model(entity), parameters);

        assertEquals("sales_order", parameters.get("javaGenFolderName"));
        assertEquals("sales_orders", entity.get("javaPerspectiveName"));
    }

    @Test
    void normalizesTheTablePrefixWithItsSeparator() {
        Map<String, Object> parameters = parameters();
        parameters.put("tablePrefix", "CODBEX");

        ModelParameterProcessor.process(model(entity("Book", "Books", property("Name", "VARCHAR"))), parameters);

        assertEquals("CODBEX_", parameters.get("tablePrefix"));
    }

    @Test
    void recordsThePrimaryKeys() {
        Map<String, Object> first = property("Id", "INTEGER");
        first.put("dataPrimaryKey", "true");
        Map<String, Object> second = property("Version", "INTEGER");
        second.put("dataPrimaryKey", "true");
        Map<String, Object> entity = entity("Book", "Books", first, second);

        ModelParameterProcessor.process(model(entity), parameters());

        assertEquals(List.of("Id", "Version"), entity.get("primaryKeys"));
        assertEquals("Id, Version", entity.get("primaryKeysString"));
    }

    @Test
    void preRendersAnInputPatternForBothATemplateLanguageAndJava() {
        Map<String, Object> property = property("Code", "VARCHAR");
        property.put("widgetPattern", "^\\d{3}\\.\\d{2}$");
        ModelParameterProcessor.process(model(entity("Book", "Books", property)), parameters());

        assertEquals("'^\\\\d{3}\\\\.\\\\d{2}$'", property.get("widgetPatternJs"));
        assertEquals("^\\\\d{3}\\\\.\\\\d{2}$", property.get("widgetPatternJava"));
    }

    @Test
    void marksAHierarchicalEntityAsNeedingReferenceValidation() {
        Map<String, Object> entity = entity("Category", "Categories", property("Name", "VARCHAR"));
        entity.put("hierarchyProperty", "Parent");
        ModelParameterProcessor.process(model(entity), parameters());

        assertTrue(Boolean.TRUE.equals(entity.get("hasReferenceValidations")));
    }

    /**
     * The line of a locked document must inherit the lock: its writes recompute the master's totals, so
     * leaving the child unguarded leaves the master's own guard with an open back door.
     */
    @Test
    void aCompositionChildInheritsItsMastersStatusLock() {
        Map<String, Object> master = entity("Invoice", "Invoices", property("Id", "INTEGER"));
        master.put("immutableStatusProperty", "Status");
        master.put("immutableStatusValues", "2,3");
        Map<String, Object> child = entity("InvoiceItem", "Invoices", compositionTo("Invoice", "Invoices"));

        ModelParameterProcessor.process(model(master, child), javaParameters());

        Map<String, Object> lock = masterLock(child);
        assertEquals("Invoice", lock.get("fkProperty"));
        assertEquals("Invoice", lock.get("entity"));
        assertEquals("gen.sales_order.data.invoices.InvoiceEntity", lock.get("entityClass"));
        assertEquals("gen.sales_order.data.invoices.InvoiceRepository", lock.get("repositoryClass"));
        assertEquals("Status", lock.get("statusProperty"));
        assertEquals("2,3", lock.get("statusValues"));
        assertEquals(Boolean.FALSE, lock.get("always"));
    }

    @Test
    void aCompositionChildInheritsAnAppendOnlyMaster() {
        Map<String, Object> master = entity("Invoice", "Invoices", property("Id", "INTEGER"));
        master.put("immutableAlways", "true");
        Map<String, Object> child = entity("InvoiceItem", "Invoices", compositionTo("Invoice", "Invoices"));

        ModelParameterProcessor.process(model(master, child), javaParameters());

        assertEquals(Boolean.TRUE, masterLock(child).get("always"));
    }

    /**
     * The deliberate post-lock collection (intent {@code locksWithMaster: false}) - money keeps
     * arriving against an issued invoice long after its content is frozen.
     */
    @Test
    void aChildOptedOutOfTheLockCarriesNoGuard() {
        Map<String, Object> master = entity("Invoice", "Invoices", property("Id", "INTEGER"));
        master.put("immutableStatusProperty", "Status");
        master.put("immutableStatusValues", "2");
        Map<String, Object> child = entity("InvoicePayment", "Invoices", compositionTo("Invoice", "Invoices"));
        child.put("locksWithMaster", "false");

        ModelParameterProcessor.process(model(master, child), javaParameters());

        assertNull(child.get("masterLock"));
    }

    /**
     * Date-based immutability (intent {@code immutableInPeriod:}): the guarded entity names the
     * register and its own date, the register carries its bounds and closed statuses, and only this
     * pass knows both plus the package each one is generated into.
     */
    @Test
    void aGuardedEntityJoinsItsPeriodRegister() {
        Map<String, Object> register = entity("AccountingPeriod", "Periods", property("Id", "INTEGER"));
        register.put("periodStartProperty", "StartDate");
        register.put("periodEndProperty", "EndDate");
        register.put("periodStatusProperty", "Status");
        register.put("periodClosedValues", "2");
        Map<String, Object> entry = entity("JournalEntry", "Ledger", property("EntryDate", "DATE"));
        entry.put("periodLockEntity", "AccountingPeriod");
        entry.put("periodLockDateProperty", "EntryDate");

        ModelParameterProcessor.process(model(register, entry), javaParameters());

        Map<String, Object> lock = (Map<String, Object>) entry.get("periodLock");
        assertEquals("EntryDate", lock.get("dateProperty"));
        assertEquals("java.time.LocalDate", lock.get("dateJavaClass"));
        assertEquals("AccountingPeriod", lock.get("entity"));
        assertEquals("gen.sales_order.data.periods.AccountingPeriodEntity", lock.get("entityClass"));
        assertEquals("gen.sales_order.data.periods.AccountingPeriodRepository", lock.get("repositoryClass"));
        assertEquals("StartDate", lock.get("startProperty"));
        assertEquals("EndDate", lock.get("endProperty"));
        assertEquals("Status", lock.get("statusProperty"));
        assertEquals("2", lock.get("closedValues"));
        // The register itself is not guarded by anything - closing a period is a write to the register.
        assertNull(register.get("periodLock"));
    }

    /**
     * A line's writes recompute its master's totals, so a document dated in a closed period freezes its
     * lines with it - the same argument that made the status lock reach the children.
     */
    @Test
    void aCompositionChildInheritsItsMastersPeriodLock() {
        Map<String, Object> register = entity("AccountingPeriod", "Periods", property("Id", "INTEGER"));
        register.put("periodStartProperty", "StartDate");
        register.put("periodEndProperty", "EndDate");
        register.put("periodStatusProperty", "Status");
        register.put("periodClosedValues", "2");
        Map<String, Object> master = entity("Invoice", "Invoices", property("IssueDate", "DATE"));
        master.put("periodLockEntity", "AccountingPeriod");
        master.put("periodLockDateProperty", "IssueDate");
        Map<String, Object> child = entity("InvoiceItem", "Invoices", compositionTo("Invoice", "Invoices"));

        ModelParameterProcessor.process(model(register, master, child), javaParameters());

        Map<String, Object> lock = masterLock(child);
        // A master locked only by its period carries no status half at all - the guard's status branch
        // keys on exactly that, rather than on a flag that would have to be kept in step with it.
        assertEquals(Boolean.FALSE, lock.get("always"));
        assertNull(lock.get("statusProperty"));
        Map<String, Object> period = (Map<String, Object>) lock.get("period");
        assertEquals("IssueDate", period.get("dateProperty"));
        assertEquals("gen.sales_order.data.periods.AccountingPeriodRepository", period.get("repositoryClass"));
    }

    @Test
    void aChildOfAnUnlockedMasterCarriesNoGuard() {
        Map<String, Object> master = entity("Invoice", "Invoices", property("Id", "INTEGER"));
        Map<String, Object> child = entity("InvoiceItem", "Invoices", compositionTo("Invoice", "Invoices"));

        ModelParameterProcessor.process(model(master, child), javaParameters());

        assertNull(child.get("masterLock"));
    }

    /**
     * A multi-select (intent {@code kind: subset}) carries its option source as the dedicated
     * {@code widgetOptionsEntityName}, never as relationship metadata - so the lookup URLs are built
     * from that entity's own perspective.
     */
    @Test
    void aMultiselectResolvesItsOptionSourceFromTheOptionsEntity() {
        Map<String, Object> channels = property("Channels", "VARCHAR");
        channels.put("widgetType", "MULTISELECT");
        channels.put("widgetOptionsEntityName", "Channel");
        channels.put("widgetDropDownKey", "Id");
        channels.put("widgetDropDownValue", "Name");
        Map<String, Object> lookupEntity = entity("Channel", "Channels", property("Id", "INTEGER"));
        lookupEntity.put("type", "SETTING");
        Map<String, Object> campaign = entity("Campaign", "Campaigns", channels);

        ModelParameterProcessor.process(model(campaign, lookupEntity), javaParameters());

        // A SETTING target publishes under the shared Settings perspective, not its own name (as the
        // sanitized Java package segment it becomes).
        assertEquals("/services/java/bookstore/gen/sales_order/api/settings/ChannelController",
                channels.get("widgetDropdownControllerUrl"));
        assertEquals(Boolean.TRUE, campaign.get("hasDropdowns"));
    }

    /**
     * The dead-widget failure the guard exists for: the view block is gated on the widget TYPE while
     * its option loading is gated on the entity carrying any option source at all, so an unresolvable
     * target used to generate a Refresh button calling a method that was never emitted.
     */
    @Test
    void aMultiselectWithoutAnOptionsEntityFailsTheGeneration() {
        Map<String, Object> channels = property("Channels", "VARCHAR");
        channels.put("widgetType", "MULTISELECT");
        Map<String, Object> model = model(entity("Campaign", "Campaigns", channels));

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> ModelParameterProcessor.process(model, javaParameters()));
        assertTrue(ex.getMessage()
                     .contains("[Campaign.Channels] is a multi-select but names no options entity"),
                ex.getMessage());
    }

    @Test
    void aMultiselectOverAnUnknownOptionsEntityFailsTheGeneration() {
        Map<String, Object> channels = property("Channels", "VARCHAR");
        channels.put("widgetType", "MULTISELECT");
        channels.put("widgetOptionsEntityName", "Chanel");
        Map<String, Object> model = model(entity("Campaign", "Campaigns", channels));

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> ModelParameterProcessor.process(model, javaParameters()));
        assertTrue(ex.getMessage()
                     .contains("[Campaign.Channels] is a multi-select over [Chanel]"),
                ex.getMessage());
    }

    /**
     * A register column over a subset resolves EACH key through the options entity's rows, so it
     * carries the {@code multi} flag beside the same lookup a foreign key gets - without it the
     * register renders the raw key list.
     */
    @Test
    void aRegisterColumnOverASubsetCarriesTheMultiLookup() {
        Map<String, Object> column = property("Channels", "VARCHAR");
        column.put("widgetType", "MULTISELECT");
        column.put("widgetOptionsEntityName", "Channel");
        column.put("widgetOptionsEntityPerspectiveName", "Settings");
        column.put("widgetDropDownKey", "Id");
        column.put("widgetDropDownValue", "Name");
        Map<String, Object> campaign = entity("Campaign", "Campaigns", property("Id", "INTEGER"));
        campaign.put("relatedEntities", List.of(register("CampaignCost", "Costs", column)));

        ModelParameterProcessor.process(model(campaign), javaParameters());

        Map<String, Object> resolved = registerColumn(campaign);
        assertEquals(Boolean.TRUE, resolved.get("multi"));
        assertEquals("/services/java/bookstore/gen/sales_order/api/settings/ChannelController", lookup(resolved).get("url"));
        assertEquals("Id", lookup(resolved).get("key"));
        assertEquals("Name", lookup(resolved).get("text"));
    }

    /**
     * A register source generated before the perspective travelled on the property: the options entity
     * is resolved in this model instead, which is where a same-model register's target always lives.
     */
    @Test
    void aRegisterColumnOverASubsetFallsBackToThisModelsPerspective() {
        Map<String, Object> column = property("Channels", "VARCHAR");
        column.put("widgetType", "MULTISELECT");
        column.put("widgetOptionsEntityName", "Channel");
        Map<String, Object> lookupEntity = entity("Channel", "Channels", property("Id", "INTEGER"));
        lookupEntity.put("type", "SETTING");
        Map<String, Object> campaign = entity("Campaign", "Campaigns", property("Id", "INTEGER"));
        campaign.put("relatedEntities", List.of(register("CampaignCost", "Costs", column)));

        ModelParameterProcessor.process(model(campaign, lookupEntity), javaParameters());

        Map<String, Object> resolved = registerColumn(campaign);
        assertEquals(Boolean.TRUE, resolved.get("multi"));
        assertEquals("/services/java/bookstore/gen/sales_order/api/settings/ChannelController", lookup(resolved).get("url"));
    }

    /**
     * An options entity this model cannot resolve at all (a register source owned by another model,
     * generated before the perspective travelled): the column renders the raw keys rather than a URL
     * built on a guess.
     */
    @Test
    void aRegisterColumnWhoseOptionsEntityIsUnresolvableCarriesNoLookup() {
        Map<String, Object> column = property("Channels", "VARCHAR");
        column.put("widgetType", "MULTISELECT");
        column.put("widgetOptionsEntityName", "Channel");
        Map<String, Object> campaign = entity("Campaign", "Campaigns", property("Id", "INTEGER"));
        campaign.put("relatedEntities", List.of(register("CampaignCost", "Costs", column)));

        ModelParameterProcessor.process(model(campaign), javaParameters());

        Map<String, Object> resolved = registerColumn(campaign);
        assertNull(resolved.get("multi"));
        assertNull(resolved.get("lookup"));
    }

    /**
     * Builds a related-records register declaration, as the model carries it.
     *
     * @param sourceEntity the referencing entity
     * @param perspectiveName its perspective
     * @param columns the property metadata of the columns it shows
     * @return the register
     */
    @SafeVarargs
    private static Map<String, Object> register(String sourceEntity, String perspectiveName, Map<String, Object>... columns) {
        Map<String, Object> register = new LinkedHashMap<>();
        register.put("entity", sourceEntity);
        register.put("label", sourceEntity);
        register.put("perspectiveName", perspectiveName);
        register.put("primaryKey", "Id");
        register.put("fkProperty", "Campaign");
        register.put("properties", List.of(columns));
        return register;
    }

    /**
     * The single column of the single register of an entity.
     *
     * @param entity the entity carrying the register
     * @return the resolved column descriptor
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> registerColumn(Map<String, Object> entity) {
        Map<String, Object> register = ((List<Map<String, Object>>) entity.get("relatedEntities")).get(0);
        return ((List<Map<String, Object>>) register.get("columns")).get(0);
    }

    /**
     * A resolved column's lookup descriptor.
     *
     * @param column the column
     * @return the lookup
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> lookup(Map<String, Object> column) {
        return (Map<String, Object>) column.get("lookup");
    }

    /**
     * Builds a model around the given entities.
     *
     * @param entities the entities
     * @return the model
     */
    private static Map<String, Object> model(Map<String, Object>... entities) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("entities", List.of(entities));
        return model;
    }

    /**
     * Builds an entity with the given properties.
     *
     * @param name the entity name
     * @param perspectiveName the perspective it belongs to
     * @param properties the properties
     * @return the entity
     */
    private static Map<String, Object> entity(String name, String perspectiveName, Map<String, Object>... properties) {
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("name", name);
        entity.put("type", "PRIMARY");
        entity.put("perspectiveName", perspectiveName);
        entity.put("properties", List.of(properties));
        return entity;
    }

    /**
     * Builds a property.
     *
     * @param name the property name
     * @param dataType the SQL type
     * @return the property
     */
    private static Map<String, Object> property(String name, String dataType) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", name);
        property.put("dataType", dataType);
        return property;
    }

    /**
     * The inherited-lock metadata of a child entity.
     *
     * @param entity the child entity
     * @return the metadata
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> masterLock(Map<String, Object> entity) {
        return (Map<String, Object>) entity.get("masterLock");
    }

    /**
     * Builds the composition FK a child carries to its master - the property the whole master-detail
     * derivation keys on.
     *
     * @param master the master entity name
     * @param masterPerspective the master's perspective
     * @return the property
     */
    private static Map<String, Object> compositionTo(String master, String masterPerspective) {
        Map<String, Object> property = property(master, "INTEGER");
        property.put("relationshipType", "COMPOSITION");
        property.put("relationshipCardinality", "1_n");
        property.put("relationshipEntityName", master);
        property.put("relationshipEntityPerspectiveName", masterPerspective);
        return property;
    }

    /**
     * The parameters of a generation targeting the Java runtime - the only one the cross-entity
     * derivations run for.
     *
     * @return the parameters
     */
    private static Map<String, Object> javaParameters() {
        Map<String, Object> parameters = parameters();
        parameters.put("javaRuntime", Boolean.TRUE);
        return parameters;
    }

    /**
     * Builds the parameters a request would carry.
     *
     * @return the parameters
     */
    private static Map<String, Object> parameters() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("projectName", "bookstore");
        parameters.put("genFolderName", "sales-order");
        return parameters;
    }

}
