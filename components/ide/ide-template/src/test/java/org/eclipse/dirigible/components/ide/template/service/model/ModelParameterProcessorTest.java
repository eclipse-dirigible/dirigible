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
