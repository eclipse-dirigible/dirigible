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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the derivation pass that turns a model into generation parameters.
 */
class ModelParameterProcessorTest {

    /**
     * #7611: a check's message reaches the Java templates as the arguments of the per-language
     * resolution - the fully qualified catalog key and the default text - and the en-US catalog
     * collects every message under the same key, a model written before the key was recorded deriving
     * it the same way.
     */
    @Test
    @SuppressWarnings("unchecked")
    void checkMessagesBecomeCatalogKeysAndResolutionArguments() {
        Map<String, Object> translated = new LinkedHashMap<>();
        translated.put("kind", "compare");
        translated.put("message", "The \"discount\" is at most 50%");
        translated.put("messageKey", "Customer_discountCap");
        Map<String, Object> legacy = new LinkedHashMap<>();
        legacy.put("kind", "exactlyOne");
        legacy.put("message", "Exactly one");
        legacy.put("fields", List.of("A", "B"));
        Map<String, Object> customer = entity("Customer", "Customers", property("Id", "INTEGER"));
        customer.put("checks", List.of(translated, legacy));
        Map<String, Object> model = model(customer);
        Map<String, Object> parameters = parameters();
        parameters.put("filePath", "billing.model");

        ModelParameterProcessor.process(model, parameters);

        assertEquals("bookstore:billing-model.checks.Customer_discountCap", translated.get("messageCatalogKey"));
        assertEquals("\"bookstore:billing-model.checks.Customer_discountCap\", \"The \\\"discount\\\" is at most 50%\"",
                translated.get("messageArgsJava"));
        assertEquals("bookstore:billing-model.checks.Customer_exactlyOne_1", legacy.get("messageCatalogKey"));
        assertEquals("\"bookstore:billing-model.checks.Customer_exactlyOne_1\", \"Exactly one\"", legacy.get("messageArgsJava"));

        Map<String, Object> catalog = ModelTranslations.checkMessages(model);
        assertEquals("The \"discount\" is at most 50%", catalog.get("Customer_discountCap"));
        assertEquals("Exactly one", catalog.get("Customer_exactlyOne_1"));
    }

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
     * The list templates iterate {@code listProperties}: the {@code listOrder} names first, in that
     * order (dirigible #7614), the rest after; the property maps are the same ones the form iterates.
     */
    @Test
    @SuppressWarnings("unchecked")
    void listPropertiesFollowTheListOrder() {
        Map<String, Object> number = property("Number", "VARCHAR");
        Map<String, Object> due = property("Due", "DATE");
        Map<String, Object> customer = property("Customer", "INTEGER");
        Map<String, Object> total = property("Total", "DECIMAL");
        Map<String, Object> entity = entity("Invoice", "Invoices", number, due, customer, total);
        entity.put("listOrder", "Number,customer,Total");

        ModelParameterProcessor.process(model(entity), parameters());

        List<Map<String, Object>> listed = (List<Map<String, Object>>) entity.get("listProperties");
        assertEquals(4, listed.size());
        assertSame(number, listed.get(0));
        assertSame(customer, listed.get(1));
        assertSame(total, listed.get(2));
        assertSame(due, listed.get(3));
    }

    /** Without a {@code listOrder} the list follows the control order, as before #7614. */
    @Test
    @SuppressWarnings("unchecked")
    void listPropertiesDefaultToTheControlOrder() {
        Map<String, Object> number = property("Number", "VARCHAR");
        Map<String, Object> due = property("Due", "DATE");
        Map<String, Object> entity = entity("Invoice", "Invoices", number, due);

        ModelParameterProcessor.process(model(entity), parameters());

        List<Map<String, Object>> listed = (List<Map<String, Object>>) entity.get("listProperties");
        assertEquals(2, listed.size());
        assertSame(number, listed.get(0));
        assertSame(due, listed.get(1));
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

    /**
     * The generated repository applies the authored default itself, before the create-time calculations
     * read the column (#7104), so the parameter graph carries it as a Java expression - resolved here
     * rather than assembled in the template, which used to interpolate the value unescaped and fail the
     * compile of the whole generated module on an authored quote (#7154).
     */
    @Test
    void carriesTheAuthoredDefaultAsAnEscapedJavaExpression() {
        Map<String, Object> quoted = property("Size", "VARCHAR");
        quoted.put("dataDefaultValue", "6\"");
        Map<String, Object> status = property("Status", "VARCHAR");
        status.put("dataDefaultValue", "'DRAFT'");
        Map<String, Object> rate = property("VatRate", "DECIMAL");
        rate.put("dataDefaultValue", "20");
        ModelParameterProcessor.process(model(entity("Line", "Lines", quoted, status, rate)), parameters());

        assertEquals("\"6\\\"\"", quoted.get("dataDefaultValueJavaLiteral"));
        assertEquals("\"DRAFT\"", status.get("dataDefaultValueJavaLiteral"));
        assertEquals("new java.math.BigDecimal(\"20\")", rate.get("dataDefaultValueJavaLiteral"));
    }

    /**
     * The key's presence is what the template reads as "this property has a default to apply", so a
     * property with none must leave it absent rather than null.
     */
    @Test
    void leavesTheDefaultExpressionAbsentWhereThereIsNothingToApply() {
        Map<String, Object> none = property("Name", "VARCHAR");
        Map<String, Object> sqlExpression = property("Issued", "DATE");
        sqlExpression.put("dataDefaultValue", "CURRENT_DATE");
        Map<String, Object> generatedKey = property("Id", "INTEGER");
        generatedKey.put("dataPrimaryKey", "true");
        generatedKey.put("dataAutoIncrement", "true");
        generatedKey.put("dataDefaultValue", "1");
        ModelParameterProcessor.process(model(entity("Invoice", "Invoices", none, sqlExpression, generatedKey)), parameters());

        assertFalse(none.containsKey("dataDefaultValueJavaLiteral"));
        assertFalse(sqlExpression.containsKey("dataDefaultValueJavaLiteral"));
        assertFalse(generatedKey.containsKey("dataDefaultValueJavaLiteral"));
    }

    /**
     * The .schema declares the same default as the column's DB DEFAULT, in a JSON string - so the
     * parameter graph carries it escaped for one too, as authored (the value reaches the DDL verbatim
     * by design) and quotes included. It used to be interpolated unescaped, and an authored quote left
     * the whole schema artefact unparseable, so the synchronizer created no table for ANY entity of the
     * project (#7206).
     */
    @Test
    void carriesTheAuthoredDefaultAsAnEscapedJsonLiteralToo() {
        Map<String, Object> quoted = property("Size", "VARCHAR");
        quoted.put("dataDefaultValue", "6\" \\ wide");
        Map<String, Object> status = property("Status", "VARCHAR");
        status.put("dataDefaultValue", "'DRAFT'");
        Map<String, Object> issued = property("Issued", "DATE");
        issued.put("dataDefaultValue", "CURRENT_DATE");
        // The database assigns the generated key, so it carries no Java literal - but the schema still
        // declares whatever was authored on it.
        Map<String, Object> generatedKey = property("Id", "INTEGER");
        generatedKey.put("dataPrimaryKey", "true");
        generatedKey.put("dataAutoIncrement", "true");
        generatedKey.put("dataDefaultValue", "1");
        Map<String, Object> none = property("Name", "VARCHAR");
        ModelParameterProcessor.process(model(entity("Line", "Lines", quoted, status, issued, generatedKey, none)), parameters());

        assertEquals("\"6\\\" \\\\ wide\"", quoted.get("dataDefaultValueJsonLiteral"));
        assertEquals("\"'DRAFT'\"", status.get("dataDefaultValueJsonLiteral"),
                "the SQL quotes are the DDL's, so the schema keeps the value as authored");
        assertEquals("\"CURRENT_DATE\"", issued.get("dataDefaultValueJsonLiteral"));
        assertEquals("\"1\"", generatedKey.get("dataDefaultValueJsonLiteral"));
        assertFalse(none.containsKey("dataDefaultValueJsonLiteral"), "a property with no default must leave the key absent");
    }

    /**
     * The item dialog seeds a new line with the authored default, in the shape the draft holds - and
     * the seed is resolved here rather than assembled in the template, which used to interpolate the
     * value unescaped and make the whole generated register a syntax error on an authored apostrophe
     * (#7207).
     */
    @Test
    void carriesTheAuthoredDefaultAsAnEscapedJavaScriptSeed() {
        Map<String, Object> possessive = property("Copy", "VARCHAR");
        possessive.put("dataDefaultValue", "Owner's copy");
        Map<String, Object> billable = property("Billable", "BOOLEAN");
        billable.put("widgetType", "CHECKBOX");
        billable.put("dataDefaultValue", "true");
        Map<String, Object> rate = property("VatRate", "DECIMAL");
        rate.put("dataDefaultValue", "20");
        ModelParameterProcessor.process(model(entity("Line", "Lines", possessive, billable, rate)), parameters());

        assertEquals("'Owner\\'s copy'", possessive.get("dataDefaultValueJsLiteral"));
        assertEquals("true", billable.get("dataDefaultValueJsLiteral"));
        assertEquals("20", rate.get("dataDefaultValueJsLiteral"));
    }

    /**
     * The defect: one authored default reaches several generated languages, and the SQL-quoted shape
     * was read in the Java String arm only - so an integer column's {@code '20'} seeded 20 in the item
     * dialog and emitted {@code Integer.valueOf("'20'")} in the repository, a NumberFormatException on
     * every create that relied on it (#7293). The two literals are asserted together because agreeing
     * is the whole invariant.
     */
    @Test
    void readsAQuotedNumericDefaultTheSameWayInBothLanguages() {
        Map<String, Object> quantity = property("Quantity", "INTEGER");
        quantity.put("dataDefaultValue", "'20'");
        Map<String, Object> rate = property("VatRate", "DECIMAL");
        rate.put("dataDefaultValue", "'20.00'");
        Map<String, Object> billable = property("Billable", "BOOLEAN");
        billable.put("widgetType", "CHECKBOX");
        billable.put("dataDefaultValue", "'true'");
        ModelParameterProcessor.process(model(entity("Line", "Lines", quantity, rate, billable)), parameters());

        assertEquals("Integer.valueOf(\"20\")", quantity.get("dataDefaultValueJavaLiteral"));
        assertEquals("20", quantity.get("dataDefaultValueJsLiteral"));
        assertEquals("new java.math.BigDecimal(\"20.00\")", rate.get("dataDefaultValueJavaLiteral"));
        assertEquals("20.00", rate.get("dataDefaultValueJsLiteral"));
        assertEquals("Boolean.TRUE", billable.get("dataDefaultValueJavaLiteral"));
        assertEquals("true", billable.get("dataDefaultValueJsLiteral"));
    }

    /**
     * A numeric default the property's own type cannot read is refused while the author is generating,
     * naming the property - it used to compile into an expression that threw on every create.
     */
    @Test
    void refusesANumericDefaultThatIsNotAValueOfItsType() {
        Map<String, Object> quantity = property("Quantity", "INTEGER");
        quantity.put("dataDefaultValue", "8.0");
        Map<String, Object> model = model(entity("Line", "Lines", quantity));

        IllegalArgumentException refusal =
                assertThrows(IllegalArgumentException.class, () -> ModelParameterProcessor.process(model, parameters()));

        assertTrue(refusal.getMessage()
                          .contains("Line.Quantity"),
                "the refusal must name the property, got: " + refusal.getMessage());
    }

    /**
     * The key's presence is what the template reads as "this property has a default to seed", so a
     * property with none must leave it absent rather than null.
     */
    @Test
    void leavesTheJavaScriptSeedAbsentWhereThereIsNothingToSeed() {
        Map<String, Object> none = property("Name", "VARCHAR");
        ModelParameterProcessor.process(model(entity("Invoice", "Invoices", none)), parameters());

        assertFalse(none.containsKey("dataDefaultValueJsLiteral"));
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

    /**
     * A gate admitting further roles after its own (an intent's {@code .settings}
     * {@code access.extraRoles}) still declares only its own role as the default one - the whole list
     * as one name would publish a role no caller can hold (dirigible #7652).
     */
    @Test
    void aGateAdmittingFurtherRolesDeclaresOnlyItsOwn() {
        Map<String, Object> entity = entity("Book", "Books", property("Name", "VARCHAR"));
        entity.put("generateDefaultRoles", "true");
        entity.put("roleRead", "library.Book.BookReadOnly,Owner,User");
        entity.put("roleWrite", "library.Book.BookFullAccess,Owner,User");
        Map<String, Object> parameters = parameters();

        ModelParameterProcessor.process(model(entity), parameters);

        Map<String, Object> role = ModelValues.asMap(ModelValues.asList(parameters.get("roles"))
                                                                .get(0));
        assertEquals("library.Book.BookReadOnly", role.get("roleRead"));
        assertEquals("library.Book.BookFullAccess", role.get("roleWrite"));
        assertEquals("library.Book.BookReadOnly,Owner,User", entity.get("roleRead"), "the gate itself keeps every role it admits");
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
        document.put("kind", "itemsMin");
        Map<String, Object> conditional = new LinkedHashMap<>();
        conditional.put("kind", "requiredWhen");
        Map<String, Object> gated = new LinkedHashMap<>();
        gated.put("kind", "requiredWhen");
        gated.put("status", "4");
        Map<String, Object> entity = entity("Invoice", "Invoices", property("Total", "DECIMAL"));
        entity.put("checks", List.of(row, guard, document, conditional, gated));

        ModelParameterProcessor.process(model(entity), parameters());

        // An ungated requiredWhen holds on every user write, so it joins the row checks the REST
        // surfaces enforce; one naming a status is the repository's, like every other gated check.
        assertEquals(2, ModelValues.asList(entity.get("rowChecks"))
                                   .size());
        assertEquals(1, ModelValues.asList(entity.get("guardChecks"))
                                   .size());
        assertEquals(2, ModelValues.asList(entity.get("documentChecks"))
                                   .size());
    }

    @Test
    void aCompareSplitsByItsGateToo() {
        Map<String, Object> ungated = new LinkedHashMap<>();
        ungated.put("kind", "compare");
        Map<String, Object> gated = new LinkedHashMap<>();
        gated.put("kind", "compare");
        gated.put("status", "2");
        Map<String, Object> entity = entity("VacationRequest", "Requests", property("Days", "DECIMAL"));
        entity.put("checks", List.of(ungated, gated));

        ModelParameterProcessor.process(model(entity), parameters());

        // A comparison holds on every user write unless it names the status it is enforced at (#7338):
        // "days > 0 before SUBMITTED" is the repository's, so the draft being filled in is not refused.
        assertEquals(1, ModelValues.asList(entity.get("rowChecks"))
                                   .size());
        assertEquals(1, ModelValues.asList(entity.get("documentChecks"))
                                   .size());
    }

    @Test
    void carriesEveryAuthoredMessageAsAnEscapedJavaLiteralToo() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("kind", "compare");
        row.put("message", "A \"due\" date is never before the invoice date");
        Map<String, Object> guard = new LinkedHashMap<>();
        guard.put("kind", "guard");
        guard.put("message", "Insufficient balance in C:\\ledger");
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("kind", "itemsMin");
        document.put("message", "An invoice needs at least one \"line\"");
        Map<String, Object> unique = new LinkedHashMap<>();
        unique.put("name", "INVOICE_\"NUMBER\"");
        unique.put("message", "This \"number\" is already registered");
        Map<String, Object> entity = entity("Invoice", "Invoices", property("Total", "DECIMAL"));
        entity.put("checks", List.of(row, guard, document));
        entity.put("uniqueConstraints", List.of(unique));

        ModelParameterProcessor.process(model(entity), parameters());

        // The raw value stays for the surfaces that render it as text; only the Java sites read the
        // twin, whose quote is escaped rather than ending the literal it is written into (#7241).
        assertEquals("A \"due\" date is never before the invoice date", row.get("message"));
        assertEquals("A \\\"due\\\" date is never before the invoice date", row.get("messageJavaLiteral"));
        assertEquals("Insufficient balance in C:\\\\ledger", guard.get("messageJavaLiteral"));
        assertEquals("An invoice needs at least one \\\"line\\\"", document.get("messageJavaLiteral"));
        assertEquals("This \\\"number\\\" is already registered", unique.get("messageJavaLiteral"));
        assertEquals("INVOICE_\\\"NUMBER\\\"", unique.get("nameJavaLiteral"));
    }

    /**
     * The model carries a check's literal and its condition as DATA (#7405); the Java twin is derived
     * HERE, next to the message twin and the default-value twin, so an artefact an author opens in the
     * modeler never holds a java.math.BigDecimal.
     */
    @Test
    void derivesTheJavaTwinsOfACheckSNeutralLiteralAndCondition() {
        Map<String, Object> compare = new LinkedHashMap<>();
        compare.put("kind", "compare");
        compare.put("field", "Days");
        compare.put("op", ">");
        compare.put("value", Map.of("kind", "number", "text", "0"));
        Map<String, Object> forbid = new LinkedHashMap<>();
        forbid.put("kind", "forbidWhen");
        forbid.put("when", List.of(
                Map.of("owner", "hop0", "property", "Status", "equal", true, "type", "integer", "value", "7", "numericKey", false)));
        Map<String, Object> entity = entity("VacationRequest", "VacationRequests", property("Days", "DECIMAL"));
        entity.put("checks", List.of(compare, forbid));

        ModelParameterProcessor.process(model(entity), parameters());

        // The neutral halves stay - they are what a non-Java template reads - and the Java rides
        // alongside them rather than replacing them.
        assertEquals(Map.of("kind", "number", "text", "0"), compare.get("value"));
        assertEquals("new java.math.BigDecimal(\"0\")", compare.get("literalJavaExpression"));
        assertEquals("java.util.Objects.equals((hop0 == null ? null : hop0.Status), 7)", forbid.get("guardJavaExpression"));
    }

    /**
     * A half that does not render is left ABSENT, as the default value literal is: the key's presence
     * is what a template reads.
     */
    @Test
    void leavesACheckSJavaTwinAbsentWhereThereIsNothingToRender() {
        Map<String, Object> compare = new LinkedHashMap<>();
        compare.put("kind", "compare");
        compare.put("field", "Days");
        compare.put("than", "Allowed");
        Map<String, Object> entity = entity("VacationRequest", "VacationRequests", property("Days", "DECIMAL"));
        entity.put("checks", List.of(compare));

        ModelParameterProcessor.process(model(entity), parameters());

        assertFalse(compare.containsKey("literalJavaExpression"));
        assertFalse(compare.containsKey("guardJavaExpression"));
    }

    @Test
    void leavesTheMessageLiteralAbsentWhereNoMessageIsAuthored() {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("kind", "exactlyOne");
        Map<String, Object> unique = new LinkedHashMap<>();
        unique.put("name", "INVOICE_NUMBER");
        Map<String, Object> entity = entity("Invoice", "Invoices", property("Total", "DECIMAL"));
        entity.put("checks", List.of(check));
        entity.put("uniqueConstraints", List.of(unique));

        ModelParameterProcessor.process(model(entity), parameters());

        assertFalse(check.containsKey("messageJavaLiteral"));
        assertFalse(unique.containsKey("messageJavaLiteral"));
        assertEquals("INVOICE_NUMBER", unique.get("nameJavaLiteral"));
    }

    @Test
    void aForbidWhenSplitsByItsGateAndCollectsItsMasterGuard() {
        Map<String, Object> term = new LinkedHashMap<>();
        term.put("property", "Status");
        term.put("equal", Boolean.TRUE);
        term.put("value", "7");
        Map<String, Object> ungated = new LinkedHashMap<>();
        ungated.put("kind", "forbidWhen");
        ungated.put("masterGuard", List.of(term));
        Map<String, Object> gated = new LinkedHashMap<>();
        gated.put("kind", "forbidWhen");
        gated.put("status", "7");
        Map<String, Object> entity = entity("SalesInvoiceCustomerPayment", "Payments", property("Amount", "DECIMAL"));
        entity.put("checks", List.of(ungated, gated));

        ModelParameterProcessor.process(model(entity), parameters());

        // Like requiredWhen, an ungated forbidWhen holds on every user write (row checks) and a gated one
        // is the repository's business.
        assertEquals(1, ModelValues.asList(entity.get("rowChecks"))
                                   .size());
        assertEquals(1, ModelValues.asList(entity.get("documentChecks"))
                                   .size());
        // Only the one whose condition reads the composition master carries a UI guard the detail-register
        // template emits so the master-detail panel hides the child's Add/edit/delete affordance.
        List<Object> guards = ModelValues.asList(entity.get("forbidWhenGuards"));
        assertEquals(1, guards.size());
        assertEquals("Status", ModelValues.asMaps(guards.get(0))
                                          .get(0)
                                          .get("property"));
        // ...and BOTH reach the delete list, whatever the gate says about the write half (#7372): a
        // delete is nobody's transition, so there is no repository-side write for a gated check to sit
        // on, and the removal of a guarded row is the largest of the three changes the panel hides.
        assertEquals(2, ModelValues.asList(entity.get("deleteChecks"))
                                   .size());
    }

    /**
     * A check kind that is about the VALUES a write carries has nothing to say about a delete, so only
     * forbidWhen lands in the delete list - an unguarded entity generates byte-identically (#7372).
     */
    @Test
    void noOtherCheckKindReachesTheDeleteList() {
        Map<String, Object> exactlyOne = new LinkedHashMap<>();
        exactlyOne.put("kind", "exactlyOne");
        Map<String, Object> required = new LinkedHashMap<>();
        required.put("kind", "requiredWhen");
        Map<String, Object> items = new LinkedHashMap<>();
        items.put("kind", "itemsMin");
        items.put("status", "2");
        Map<String, Object> entity = entity("Invoice", "Invoices", property("Total", "DECIMAL"));
        entity.put("checks", List.of(exactlyOne, required, items));

        ModelParameterProcessor.process(model(entity), parameters());

        assertEquals(0, ModelValues.asList(entity.get("deleteChecks"))
                                   .size());
    }

    /**
     * A warning (#7466) is asked of the person saving and refuses nothing, so it reaches the
     * repository's {@code warnings()} list alone - not {@code validate()}, not the delete verb, not the
     * master-detail guard - whatever its kind; its code gets the escaped twin the Java sites read.
     */
    @Test
    void aWarningReachesOnlyTheWarningList() {
        Map<String, Object> forbid = new LinkedHashMap<>();
        forbid.put("kind", "forbidWhen");
        forbid.put("severity", "warn");
        forbid.put("code", "Invoice.forbidWhen.0");
        forbid.put("message", "The \"invoice\" is issued");
        Map<String, Object> duplicate = new LinkedHashMap<>();
        duplicate.put("kind", "duplicate");
        duplicate.put("severity", "warn");
        duplicate.put("code", "Invoice.duplicate.1");
        Map<String, Object> refusing = new LinkedHashMap<>();
        refusing.put("kind", "exactlyOne");
        Map<String, Object> entity = entity("Invoice", "Invoices", property("Total", "DECIMAL"));
        entity.put("checks", List.of(forbid, duplicate, refusing));

        ModelParameterProcessor.process(model(entity), parameters());

        List<Map<String, Object>> warnings = ModelValues.asMaps(entity.get("warningChecks"));
        assertEquals(2, warnings.size());
        assertEquals("Invoice.forbidWhen.0", warnings.get(0)
                                                     .get("codeJavaLiteral"));
        assertEquals("The \\\"invoice\\\" is issued", warnings.get(0)
                                                              .get("messageJavaLiteral"));
        assertEquals(1, ModelValues.asList(entity.get("rowChecks"))
                                   .size(),
                "only the refusing check is validate()'s");
        assertEquals(0, ModelValues.asList(entity.get("deleteChecks"))
                                   .size());
        assertEquals(0, ModelValues.asList(entity.get("forbidWhenGuards"))
                                   .size());
    }

    @Test
    void resolvesTheHopsAConditionalRequirementReadsItsValueThrough() {
        Map<String, Object> hop = new LinkedHashMap<>();
        hop.put("local", "hop0");
        hop.put("sourceExpression", "entity.Customer");
        hop.put("entity", "Customer");
        hop.put("perspective", "Customers");
        hop.put("crossModel", Boolean.TRUE);
        hop.put("targetModel", "base-customers");
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("kind", "requiredWhen");
        check.put("pathLoads", List.of(hop));
        Map<String, Object> entity = entity("Invoice", "Invoices", property("Total", "DECIMAL"));
        entity.put("checks", List.of(check));

        ModelParameterProcessor.process(model(entity), parameters());

        Map<String, Object> resolved = ModelValues.asMaps(ModelValues.asMaps(entity.get("rowChecks"))
                                                                     .get(0)
                                                                     .get("pathLoads"))
                                                  .get(0);
        assertEquals("gen.base_customers.data.customers.CustomerEntity", resolved.get("entityClass"));
        assertEquals("gen.base_customers.data.customers.CustomerRepository", resolved.get("repositoryClass"));
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
     * whenTargetDeleted: restrict (intent #7547) - the target carries FACTS only (the referencing
     * entity's name + its FK property); this pass resolves the referencing entity's own generated
     * coordinates into the FQNs the DAO constructs directly, same as
     * {@link #aCompositionChildInheritsItsMastersStatusLock} resolves the PARENT's.
     */
    @Test
    void aDeleteRestrictorResolvesToTheReferencingEntitysFqn() {
        Map<String, Object> category = entity("ExpenseCategory", "expenses", property("Id", "INTEGER"));
        Map<String, Object> restrictor = new LinkedHashMap<>();
        restrictor.put("referencingEntity", "Expense");
        restrictor.put("fkProperty", "Category");
        category.put("deleteRestrictors", new java.util.ArrayList<>(List.of(restrictor)));
        Map<String, Object> expense = entity("Expense", "expenses", property("Id", "INTEGER"));

        ModelParameterProcessor.process(model(category, expense), javaParameters());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> restrictors = (List<Map<String, Object>>) category.get("deleteRestrictors");
        assertEquals(1, restrictors.size());
        assertEquals("gen.sales_order.data.expenses.ExpenseEntity", restrictors.get(0)
                                                                               .get("entityClass"));
        assertEquals("gen.sales_order.data.expenses.ExpenseRepository", restrictors.get(0)
                                                                                   .get("repositoryClass"));
        // The refusal names the referencing entity the way the page does, not by its raw identifier.
        assertEquals("Expense", restrictors.get(0)
                                           .get("referencingLabel"));
    }

    /** A restrictor naming an entity that was not generated is dropped, not emitted broken. */
    @Test
    void anUnresolvableDeleteRestrictorIsDropped() {
        Map<String, Object> category = entity("ExpenseCategory", "expenses", property("Id", "INTEGER"));
        Map<String, Object> restrictor = new LinkedHashMap<>();
        restrictor.put("referencingEntity", "Ghost");
        restrictor.put("fkProperty", "Category");
        category.put("deleteRestrictors", new java.util.ArrayList<>(List.of(restrictor)));

        ModelParameterProcessor.process(model(category), javaParameters());

        assertEquals(List.of(), category.get("deleteRestrictors"));
    }

    /**
     * The parent side of a junction's {@code checks: agree} (#7589): the guard carries facts only and
     * this pass resolves the junction's repository FQN and the message's Java-literal twin - a quoted
     * field name in the message must not end the literal it is written into. A guard naming a junction
     * that was not generated is dropped, as an unresolvable restrictor is.
     */
    @Test
    void anAgreeGuardResolvesToTheJunctionsRepositoryAndEscapesItsMessage() {
        Map<String, Object> payment = entity("CustomerPayment", "payments", property("Id", "INTEGER"));
        Map<String, Object> guard = new LinkedHashMap<>();
        guard.put("referencingEntity", "InvoicePayment");
        guard.put("fkProperty", "CustomerPayment");
        guard.put("property", "Customer");
        guard.put("message", "The \"customer\" must match the invoice's");
        Map<String, Object> ghost = new LinkedHashMap<>(guard);
        ghost.put("referencingEntity", "Ghost");
        payment.put("agreeGuards", new java.util.ArrayList<>(List.of(guard, ghost)));
        Map<String, Object> allocation = entity("InvoicePayment", "allocations", property("Id", "INTEGER"));

        ModelParameterProcessor.process(model(payment, allocation), javaParameters());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> guards = (List<Map<String, Object>>) payment.get("agreeGuards");
        assertEquals(1, guards.size(), "the guard naming an ungenerated junction must be dropped: " + guards);
        assertEquals("gen.sales_order.data.allocations.InvoicePaymentRepository", guards.get(0)
                                                                                        .get("repositoryClass"));
        assertEquals("The \\\"customer\\\" must match the invoice's", guards.get(0)
                                                                            .get("messageJavaLiteral"));
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
     * A lock reaches down the whole composition chain (#7550): a posted payroll run freezes the lines
     * of its payslips too, although the payslip between them declares no lock of its own - the
     * grandchild walks to the run through its payslip.
     */
    @Test
    void aGrandchildInheritsTheLockOfAnAncestorThroughAnUnlockedMaster() {
        Map<String, Object> run = entity("PayrollRun", "Payroll", property("Id", "INTEGER"));
        run.put("immutableStatusProperty", "Status");
        run.put("immutableStatusValues", "3");
        Map<String, Object> payslip = entity("Payslip", "Payroll", compositionTo("PayrollRun", "Payroll"));
        Map<String, Object> line = entity("PayslipLine", "Payroll", compositionTo("Payslip", "Payroll"));

        ModelParameterProcessor.process(model(run, payslip, line), javaParameters());

        Map<String, Object> lock = masterLock(line);
        assertEquals("Payslip", lock.get("fkProperty"));
        assertEquals("gen.sales_order.data.payroll.PayslipRepository", lock.get("repositoryClass"));
        assertEquals(Boolean.FALSE, lock.get("always"), "the payslip itself does not lock");
        assertNull(lock.get("statusProperty"), "the payslip itself does not lock");
        List<Map<String, Object>> ancestors = (List<Map<String, Object>>) lock.get("ancestors");
        assertEquals(1, ancestors.size());
        Map<String, Object> ancestor = ancestors.get(0);
        assertEquals("PayrollRun", ancestor.get("fkProperty"), "the FK on the payslip leading to the run");
        assertEquals("PayrollRun", ancestor.get("entity"));
        assertEquals("gen.sales_order.data.payroll.PayrollRunRepository", ancestor.get("repositoryClass"));
        assertEquals("Status", ancestor.get("statusProperty"));
        assertEquals("3", ancestor.get("statusValues"));
        // the direct child keeps its one-hop lock, with nothing above the run to walk to
        assertEquals("PayrollRun", masterLock(payslip).get("entity"));
        assertEquals(List.of(), masterLock(payslip).get("ancestors"));
    }

    /**
     * A master that outlives its own master's lock ({@code locksWithMaster: false}) stops the climb:
     * its children are as writable as it is.
     */
    @Test
    void theChainStopsAtAMasterOptedOutOfTheLockAboveIt() {
        Map<String, Object> run = entity("PayrollRun", "Payroll", property("Id", "INTEGER"));
        run.put("immutableStatusProperty", "Status");
        run.put("immutableStatusValues", "3");
        Map<String, Object> payslip = entity("Payslip", "Payroll", compositionTo("PayrollRun", "Payroll"));
        payslip.put("locksWithMaster", "false");
        Map<String, Object> line = entity("PayslipLine", "Payroll", compositionTo("Payslip", "Payroll"));

        ModelParameterProcessor.process(model(run, payslip, line), javaParameters());

        assertNull(payslip.get("masterLock"));
        assertNull(line.get("masterLock"));
    }

    /** Every locking level counts: the direct master's own lock and the one above it are both kept. */
    @Test
    void aGrandchildKeepsItsMastersOwnLockAndTheOneAboveIt() {
        Map<String, Object> run = entity("PayrollRun", "Payroll", property("Id", "INTEGER"));
        run.put("immutableAlways", "true");
        Map<String, Object> payslip = entity("Payslip", "Payroll", compositionTo("PayrollRun", "Payroll"));
        payslip.put("immutableStatusProperty", "Status");
        payslip.put("immutableStatusValues", "2");
        Map<String, Object> line = entity("PayslipLine", "Payroll", compositionTo("Payslip", "Payroll"));

        ModelParameterProcessor.process(model(run, payslip, line), javaParameters());

        Map<String, Object> lock = masterLock(line);
        assertEquals("Status", lock.get("statusProperty"));
        List<Map<String, Object>> ancestors = (List<Map<String, Object>>) lock.get("ancestors");
        assertEquals(1, ancestors.size());
        assertEquals(Boolean.TRUE, ancestors.get(0)
                                            .get("always"));
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
     * The authored prose a generated class writes into a Java string literal, escaped once here so no
     * template has to re-derive it - and so a quote in it mis-values one field instead of failing the
     * compile of the whole generated module (#7295).
     */
    @Test
    void escapesTheAuthoredProseTheGeneratedJavaWritesIntoALiteral() {
        Map<String, Object> property = property("Name", "VARCHAR");
        property.put("description", "Customer's \"trade\" name");
        property.put("numberSeries", "Sales \"Invoice\"");
        ModelParameterProcessor.process(model(entity("Book", "Books", property)), parameters());

        assertEquals("Customer's \\\"trade\\\" name", property.get("descriptionJavaLiteral"));
        assertEquals("Sales \\\"Invoice\\\"", property.get("numberSeriesJavaLiteral"));
        // The raw values stay for the surfaces that render them as text.
        assertEquals("Customer's \"trade\" name", property.get("description"));
    }

    /**
     * A property carrying neither gets no twin: a template reads the key's absence.
     */
    @Test
    void aPropertyWithNoProseCarriesNoLiteralTwin() {
        Map<String, Object> property = property("Name", "VARCHAR");
        ModelParameterProcessor.process(model(entity("Book", "Books", property)), parameters());

        assertNull(property.get("descriptionJavaLiteral"));
        assertNull(property.get("numberSeriesJavaLiteral"));
    }

    /**
     * A label pattern is authored around the fields it interpolates, and its literal segments and
     * formats are written into the generated name computation as Java literals.
     */
    @Test
    void escapesTheLiteralSegmentsAndFormatsOfALabelPattern() {
        Map<String, Object> property = property("Code", "VARCHAR");
        Map<String, Object> entity = entity("Book", "Books", property);
        Map<String, Object> literal = new LinkedHashMap<>();
        literal.put("kind", "literal");
        literal.put("text", "the \"good\" one - ");
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("kind", "field");
        field.put("property", "Code");
        field.put("format", "dd\\MM");
        entity.put("labelParts", List.of(literal, field));
        Map<String, Object> parameters = parameters();
        // The label computation is a client-Java surface, so the pass that resolves it runs there.
        parameters.put("javaRuntime", "true");
        ModelParameterProcessor.process(model(entity), parameters);

        assertEquals("the \\\"good\\\" one - ", literal.get("textJavaLiteral"));
        assertEquals("dd\\\\MM", field.get("formatJavaLiteral"));
    }

    /**
     * The seeded status names a lifecycle refusal quotes travel structurally, escaped per entry - a
     * comma in a name used to shift every entry after it, a quote broke the literal.
     */
    @Test
    void readsTheSeededStatusNamesStructurally() {
        Map<String, Object> entity = entity("Invoice", "Invoices", property("Id", "INTEGER"));
        entity.put("lifecycleStatusNameList",
                List.of(Map.of("id", "1", "name", "Sent, awaiting reply"), Map.of("id", "2", "name", "\"P\"")));
        ModelParameterProcessor.process(model(entity), parameters());

        List<Map<String, Object>> entries = (List<Map<String, Object>>) entity.get("lifecycleStatusNameEntries");
        assertEquals(2, entries.size());
        assertEquals("1", entries.get(0)
                                 .get("idJavaLiteral"));
        assertEquals("Sent, awaiting reply", entries.get(0)
                                                    .get("nameJavaLiteral"));
        assertEquals("\\\"P\\\"", entries.get(1)
                                         .get("nameJavaLiteral"));
    }

    /**
     * A model written before the structured list still carries the {@code id=name,} join, and the pass
     * reads it back so nothing regenerates differently for a name that never held a separator.
     */
    @Test
    void fallsBackToTheJoinedSeededStatusNames() {
        Map<String, Object> entity = entity("Invoice", "Invoices", property("Id", "INTEGER"));
        entity.put("lifecycleStatusNames", "1=DRAFT,2=POSTED");
        ModelParameterProcessor.process(model(entity), parameters());

        List<Map<String, Object>> entries = (List<Map<String, Object>>) entity.get("lifecycleStatusNameEntries");
        assertEquals(2, entries.size());
        assertEquals("DRAFT", entries.get(0)
                                     .get("nameJavaLiteral"));
        assertEquals("2", entries.get(1)
                                 .get("idJavaLiteral"));
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
     * A composition child inherits its master's personal scope, and with dirigible #7340 it may opt
     * that inherited surface out of WRITES on its own edge - the shape of a user-authored header whose
     * lines only an engine writes. The master's own surface stays writable.
     */
    @Test
    void aCompositionChildsOwnEdgeCanMakeTheInheritedPersonalSurfaceSeeOnly() {
        Map<String, Object> owner = property("Employee", "INTEGER");
        owner.put("widgetType", "DROPDOWN");
        owner.put("relationshipEntityName", "Employee");
        owner.put("relationshipEntityPerspectiveName", "hr");
        owner.put("relationshipPersonal", "true");
        owner.put("relationshipIdentityProperty", "Email");
        Map<String, Object> request = entity("VacationRequest", "hr", property("Id", "INTEGER"), owner);

        Map<String, Object> parentFk = compositionTo("VacationRequest", "hr");
        parentFk.put("relationshipPersonalReadOnly", "true");
        Map<String, Object> day = entity("VacationDay", "hr", property("Id", "INTEGER"), parentFk);

        ModelParameterProcessor.process(model(request, day), javaParameters());

        assertEquals(Boolean.TRUE, day.get("personalReadOnly"), "the child's own edge makes its personal surface see-only");
        assertEquals(Boolean.FALSE, request.get("personalReadOnly"), "the master it inherits the scope from stays writable");
    }

    /** Without the marker the child inherits the master's writable personal surface, as before. */
    @Test
    void withoutTheMarkerTheChildInheritsTheMastersWritableSurface() {
        Map<String, Object> owner = property("Employee", "INTEGER");
        owner.put("widgetType", "DROPDOWN");
        owner.put("relationshipEntityName", "Employee");
        owner.put("relationshipEntityPerspectiveName", "hr");
        owner.put("relationshipPersonal", "true");
        owner.put("relationshipIdentityProperty", "Email");
        Map<String, Object> request = entity("VacationRequest", "hr", property("Id", "INTEGER"), owner);
        Map<String, Object> day = entity("VacationDay", "hr", property("Id", "INTEGER"), compositionTo("VacationRequest", "hr"));

        ModelParameterProcessor.process(model(request, day), javaParameters());

        assertEquals(Boolean.FALSE, day.get("personalReadOnly"));
    }

    /**
     * The items panel lives on the MASTER's document page, so the flag the document template gates Add
     * / Fill Month / row delete on is the master's - derived from the items child, which is what
     * actually refuses the write.
     */
    @Test
    void aSeeOnlyItemsChildMakesTheDocumentsItemsPanelSeeOnly() {
        Map<String, Object> owner = property("Employee", "INTEGER");
        owner.put("widgetType", "DROPDOWN");
        owner.put("relationshipEntityName", "Employee");
        owner.put("relationshipEntityPerspectiveName", "hr");
        owner.put("relationshipPersonal", "true");
        owner.put("relationshipIdentityProperty", "Email");
        Map<String, Object> request = entity("VacationRequest", "hr", property("Id", "INTEGER"), owner);
        request.put("layoutType", "MANAGE_DOCUMENT");
        request.put("documentItemsEntity", "VacationDay");

        Map<String, Object> parentFk = compositionTo("VacationRequest", "hr");
        parentFk.put("relationshipPersonalReadOnly", "true");
        Map<String, Object> day = entity("VacationDay", "hr", property("Id", "INTEGER"), parentFk);

        ModelParameterProcessor.process(model(request, day), javaParameters());

        assertEquals(Boolean.TRUE, request.get("documentItemsReadOnly"));
        // ... and the panel the master renders for it carries the same refusal, so it offers no Add.
        assertEquals(Boolean.TRUE, ((Map<String, Object>) ((List<Object>) request.get("myChildren")).get(0)).get("readOnly"));
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

    /**
     * A roll-up's capacity guard carries WHICH rows count and WHEN it runs (#7542). Both halves are
     * rendered here, where the property's own Java shape is known: a boxed equality against a bare
     * literal is the #7237 class of guard that reads as authored and is never true.
     */
    @Test
    void aFilteredAndGatedCapacityGuardRendersItsQueryItsRowTestAndItsMessage() {
        Map<String, Object> status = property("Status", "INTEGER");
        Map<String, Object> total = property("Total", "DECIMAL");
        Map<String, Object> guard = new LinkedHashMap<>();
        guard.put("parentEntity", "Contract");
        guard.put("capacityField", "Ceiling");
        guard.put("filter", List.of(clause("ne", "Status", "number", "3")));
        guard.put("guardStatusProperty", "Status");
        guard.put("guardStatusValue", "2");
        guard.put("message", "Only {remaining} of {capacity} left; asked {requested}.");
        Map<String, Object> entity = entity("CallOff", "CallOffs", status, total);
        entity.put("rollupGuards", List.of(guard));
        Map<String, Object> parameters = parameters();
        parameters.put("javaRuntime", Boolean.TRUE);

        ModelParameterProcessor.process(model(entity), parameters);

        assertEquals(".ne(\"Status\", 3)", guard.get("filterChain"));
        assertEquals("!(entity.Status != null && entity.Status.longValue() == 3L)", guard.get("incomingMatch"),
                "the row in hand is tested by VALUE - a boxed equality across two widths is never true");
        assertEquals("\"Only \" + guardParent.Ceiling.subtract(guardConsumed) + \" of \" + guardParent.Ceiling + \" left; asked \""
                + " + guardIncoming + \".\"", guard.get("messageExpression"));
        assertEquals("true", entity.get("hasGatedRollupGuards"),
                "the targeted write path has to run the gated guards too - a workflow setter is how a document reaches the gate");
    }

    /** A guard declaring neither key renders exactly as it did before #7542. */
    @Test
    void anUnfilteredUngatedCapacityGuardRendersNothingExtra() {
        Map<String, Object> guard = new LinkedHashMap<>();
        guard.put("parentEntity", "Contract");
        guard.put("capacityField", "Ceiling");
        Map<String, Object> entity = entity("CallOff", "CallOffs", property("Total", "DECIMAL"));
        entity.put("rollupGuards", List.of(guard));
        Map<String, Object> parameters = parameters();
        parameters.put("javaRuntime", Boolean.TRUE);

        ModelParameterProcessor.process(model(entity), parameters);

        assertNull(guard.get("filterChain"));
        assertNull(guard.get("incomingMatch"));
        assertNull(guard.get("messageExpression"));
        assertNull(entity.get("hasGatedRollupGuards"));
    }

    private static Map<String, Object> clause(String op, String property, String kind, String text) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("kind", kind);
        value.put("text", text);
        Map<String, Object> clause = new LinkedHashMap<>();
        clause.put("op", op);
        clause.put("property", property);
        clause.put("value", value);
        return clause;
    }

    private static Map<String, Object> parameters() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("projectName", "bookstore");
        parameters.put("genFolderName", "sales-order");
        return parameters;
    }

}
