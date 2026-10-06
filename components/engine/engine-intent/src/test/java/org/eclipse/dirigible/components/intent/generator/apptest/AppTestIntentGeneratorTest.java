/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.apptest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.TestContexts;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * Verifies the pure manifest-building core of {@link AppTestIntentGenerator}: module-level
 * coordinates (module id, standalone shell, sanitized REST base, id property, languages) and the
 * per-entity mapping (label/plural/layout/route/nav-group/api/table from the EDM metadata; fields
 * and dropdown relations from the intent; cross-model relations and composition details excluded;
 * the multilingual sample derived from inline seeds). Repository-free — it feeds a hand-built EDM
 * metadata map, exactly the shape the {@code .model} carries.
 */
class AppTestIntentGeneratorTest {

    /** The canonical address regex {@code format: email} resolves to - what the EDM emits. */
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$";

    private static final String INTENT = """
            name: countries
            languages: [en, bg]
            uses:
              - { model: currencies }
            entities:
              - name: Country
                kind: setting
                group: master-data
                multilingual: true
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, unique: true, length: 255 }
                  - { name: code2, type: string, required: true, unique: true, length: 2 }
              - name: City
                group: master-data
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 200 }
                  - { name: founded, type: date }
                  - { name: renamed, type: date }
                  - { name: uuid, type: uuid }
                  - { name: slug, type: string, calculatedOnCreate: "1" }
                  - { name: total, type: decimal, aggregate: true }
                relations:
                  - { name: Country, kind: manyToOne, to: Country, required: true }
                  - { name: Currency, kind: manyToOne, to: Currency, model: currencies, required: true }
                  - { name: Twin, kind: manyToOne, to: City, dependsOn: { relation: Country, filterBy: Country }, where: { name: Plovdiv } }
                checks:
                  - { kind: exactlyOne, fields: [uuid, slug], message: "one of uuid/slug" }
                  - { kind: compare, field: renamed, op: gt, than: founded, message: "renamed after founded" }
              - name: Account
                group: master-data
                hierarchy: Parent
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 200 }
                relations:
                  - { name: Parent, kind: manyToOne, to: Account }
            seeds:
              - name: countries
                entity: Country
                rows:
                  - { id: 1, name: Albania, code2: AL }
              - name: countries-bg
                entity: Country
                language: bg
                rows:
                  - { id: 1, name: "Албания", code2: AL }
            """;

    private final IntentModel model = IntentParser.parse(INTENT);

    @SuppressWarnings("unchecked")
    @Test
    void buildsTheModuleLevelCoordinates() {
        Map<String, Object> manifest = AppTestIntentGenerator.buildManifest("countries", "countries", model, edm());

        assertEquals("countries", manifest.get("module"));
        assertEquals("/services/web/countries/gen/countries/index.html", manifest.get("standaloneShell"));
        // the REST base uses the sanitized (java-identifier) gen folder, not the raw hyphenated name
        assertEquals("/services/java/countries/gen/countries/api", manifest.get("restBase"));
        assertEquals("Id", manifest.get("idProperty"));
        assertEquals(List.of("en", "bg"), manifest.get("languages"));
        assertEquals(3, ((List<Object>) manifest.get("entities")).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void marksAutoReadOnlyFieldsAndHierarchyEntities() {
        Map<String, Object> manifest = AppTestIntentGenerator.buildManifest("countries", "countries", model, edm());

        // a uuid and a calculated field render without an editable input - the runner must not fill them
        Map<String, Object> city = entity(manifest, "City");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) city.get("fields");
        Map<String, Object> uuid = fields.stream()
                                         .filter(f -> "Uuid".equals(f.get("name")))
                                         .findFirst()
                                         .orElseThrow();
        assertEquals(Boolean.TRUE, uuid.get("readOnly"));
        Map<String, Object> slug = fields.stream()
                                         .filter(f -> "Slug".equals(f.get("name")))
                                         .findFirst()
                                         .orElseThrow();
        assertEquals(Boolean.TRUE, slug.get("readOnly"));
        Map<String, Object> total = fields.stream()
                                          .filter(f -> "Total".equals(f.get("name")))
                                          .findFirst()
                                          .orElseThrow();
        assertEquals(Boolean.TRUE, total.get("readOnly"), "an aggregate renders in the totals footer, not as an input");

        // a hierarchy entity lists as a tree - the runner branches on the flag
        Map<String, Object> account = entity(manifest, "Account");
        assertEquals(Boolean.TRUE, account.get("hierarchy"));
        assertNull(city.get("hierarchy"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void mapsEntityMetadataAndFields() {
        Map<String, Object> country = entity(AppTestIntentGenerator.buildManifest("countries", "countries", model, edm()), "Country");

        assertEquals("Country", country.get("label"));
        assertEquals("Countries", country.get("labelPlural"));
        assertEquals("manage-list", country.get("layout"));
        assertEquals("#/Country", country.get("route"));
        assertEquals("master-data", country.get("navGroup"));
        // api uses the sanitized perspective (Settings -> settings)
        assertEquals("/settings/CountryController", country.get("api"));
        assertEquals("KF_MOD_COUNTRIES_COUNTRY", country.get("table"));
        assertEquals(Boolean.TRUE, country.get("multilingual"));
        assertEquals(Boolean.TRUE, country.get("expectSeedData"));

        // fields: PascalCased names, required/unique/length flags, no PK
        List<Map<String, Object>> fields = (List<Map<String, Object>>) country.get("fields");
        assertEquals(2, fields.size());
        Map<String, Object> name = fields.get(0);
        assertEquals("Name", name.get("name"));
        assertEquals("string", name.get("type"));
        assertEquals(Boolean.TRUE, name.get("required"));
        assertEquals(Boolean.TRUE, name.get("unique"));
        assertEquals(255.0, ((Number) name.get("length")).doubleValue());
        assertEquals(Boolean.TRUE, name.get("major"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void derivesTheMultilingualSampleFromInlineSeeds() {
        Map<String, Object> country = entity(AppTestIntentGenerator.buildManifest("countries", "countries", model, edm()), "Country");
        Map<String, Object> sample = (Map<String, Object>) country.get("multilingualSample");
        assertNotNull(sample, "an inline base + bg seed pair yields a multilingual sample");
        assertEquals("bg", sample.get("language"));
        assertEquals("Albania", sample.get("base"));
        assertEquals("Албания", sample.get("translated"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void emitsToOneRelationsAsDropdowns() {
        Map<String, Object> city = entity(AppTestIntentGenerator.buildManifest("countries", "countries", model, edm()), "City");
        List<Map<String, Object>> relations = (List<Map<String, Object>>) city.get("relations");
        assertNotNull(relations);
        assertEquals(3, relations.size());
        Map<String, Object> country = relations.get(0);
        assertEquals("Country", country.get("name"));
        assertEquals("manyToOne", country.get("kind"));
        assertEquals("Country", country.get("to"));
        assertEquals(Boolean.TRUE, country.get("required"));
        assertEquals("dropdown", country.get("widget"));
        assertEquals("Name", country.get("labelFrom"));
        // same-model targets carry their relative controller path (resolvable even for a
        // composition detail excluded from the manifest's entities list)
        assertEquals("/settings/CountryController", country.get("api"));
        assertNull(country.get("crossModel"));

        // dependsOn + where ride into the manifest so the runner picks consistent, offered samples
        Map<String, Object> twin = relations.get(2);
        assertEquals("Twin", twin.get("name"));
        assertEquals("Country", ((Map<String, Object>) twin.get("dependsOn")).get("relation"));
        assertEquals("Country", ((Map<String, Object>) twin.get("dependsOn")).get("filterBy"));
        assertEquals("Name", ((Map<String, Object>) twin.get("where")).get("by"));
        assertEquals("Plovdiv", ((Map<String, Object>) twin.get("where")).get("value"));

        Map<String, Object> cityEntity = entity(AppTestIntentGenerator.buildManifest("countries", "countries", model, edm()), "City");
        assertEquals(List.of(List.of("Uuid", "Slug")), cityEntity.get("exactlyOne"));
        // A compare check rides into the manifest too: the sample values are per-type constants, so
        // two dates come out equal and a strict comparison would have the sample record refused with
        // 400 - the runner derives the left operand from the right by the operator's own step.
        assertEquals(List.of(Map.of("field", "Renamed", "op", "gt", "than", "Founded")), cityEntity.get("compare"));

        // the cross-model relation resolves an absolute controller URL in the OWNER module (naming
        // convention here - no generation context; the real pass resolves against the owner's .model)
        Map<String, Object> currency = relations.get(1);
        assertEquals("Currency", currency.get("name"));
        assertEquals(Boolean.TRUE, currency.get("crossModel"));
        assertEquals("/services/java/currencies/gen/currencies/api/currency/CurrencyController", currency.get("apiAbsolute"));
        assertEquals("Name", currency.get("labelFrom"));
    }

    @Test
    void skipsProjectionAndDetailEntities() {
        Map<String, Map<String, Object>> edm = edm();
        edm.put("Extra", edmEntity("Extra", "Extra", "Extras", "MANAGE_DETAILS", "Extras", "master-data", "KF_MOD_COUNTRIES_EXTRA", false));
        // still only Country + City + Account — the detail child is excluded
        Map<String, Object> manifest = AppTestIntentGenerator.buildManifest("countries", "countries", model, edm);
        List<?> entities = (List<?>) manifest.get("entities");
        assertEquals(3, entities.size());
        assertNull(entityOrNull(manifest, "Extra"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void emitsThePersonalSurfaceContract() {
        String intent = """
                name: claims
                entities:
                  - name: Person
                    identity: email
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, required: true, length: 200 }
                      - { name: email, type: string, required: true, unique: true, length: 320 }
                  - name: Claim
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: note, type: string, length: 200 }
                      - { name: rate, type: decimal, sensitive: true }
                    relations:
                      - { name: Person, kind: manyToOne, to: Person, required: true, personal: true }
                """;
        Map<String, Map<String, Object>> edm = new LinkedHashMap<>();
        edm.put("Person", edmEntity("Person", "Person", "Persons", "MANAGE_LIST", "People", "hr", "KF_MOD_CLAIMS_PERSON", false));
        edm.put("Claim", edmEntity("Claim", "Claim", "Claims", "MANAGE_LIST", "Claims", "hr", "KF_MOD_CLAIMS_CLAIM", false));

        Map<String, Object> manifest = AppTestIntentGenerator.buildManifest("claims", "claims", IntentParser.parse(intent), edm);

        Map<String, Object> claim = entity(manifest, "Claim");
        Map<String, Object> personal = (Map<String, Object>) claim.get("personal");
        assertTrue(personal != null, "a personal: relation must emit the personal surface contract");
        assertEquals("/claims/ClaimMyController", personal.get("api"));
        assertEquals("Person", personal.get("owner"));
        assertEquals(List.of("Rate"), personal.get("sensitive"));
        // the identity entity itself has no personal relation - no personal block
        assertNull(entity(manifest, "Person").get("personal"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void mirrorsThePlatformOwnedFieldsAndTheDeleteGuardOfTheModel() {
        String intent = """
                name: invoices
                entities:
                  - name: SalesInvoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, length: 100, number: { series: Sales Invoice, stampOn: issue } }
                      - { name: email, type: string, length: 320, format: email }
                      - { name: total, type: decimal }
                      - { name: reference, type: string, length: 200 }
                """;
        Map<String, Object> edm = edmEntity("SalesInvoice", "Sales Invoice", "Sales Invoices", "MANAGE_LIST", "Invoices", "sales",
                "KF_MOD_INVOICES_SALESINVOICE", false);
        edm.put("properties", List.of(property("Id", Map.of("dataPrimaryKey", "true")),
                // what the EDM generator emits for a `number:` field: the platform stamps it and the
                // form renders it read-only
                property("Number", Map.of("isReadOnlyProperty", "true")), property("Email", Map.of("widgetPattern", EMAIL_PATTERN)),
                // on a numeric property the same attribute is the DISPLAY format, never a guard
                property("Total", Map.of("widgetPattern", "### ##0.00")), property("Reference", Map.of())));
        edm.put("processDeleteGuards", "SalesInvoicePosting:Sales Invoice Posting");
        Map<String, Map<String, Object>> edmEntities = new LinkedHashMap<>();
        edmEntities.put("SalesInvoice", edm);

        Map<String, Object> invoice = entity(
                AppTestIntentGenerator.buildManifest("invoices", "invoices", IntentParser.parse(intent), edmEntities), "SalesInvoice");
        Map<String, Map<String, Object>> fields = new LinkedHashMap<>();
        for (Map<String, Object> field : (List<Map<String, Object>>) invoice.get("fields")) {
            fields.put(String.valueOf(field.get("name")), field);
        }

        // the number field is the platform's: the DAO stamps and preserves it, so the runner must
        // neither write it nor pick it as the value it flips to prove an update landed
        assertEquals(Boolean.TRUE, fields.get("Number")
                                         .get("readOnly"));
        // the shape guard travels, so the sample value can be made to match instead of being refused
        assertEquals(EMAIL_PATTERN, fields.get("Email")
                                          .get("pattern"));
        assertNull(fields.get("Total")
                         .get("pattern"),
                "a numeric widgetPattern is a display format, not an input guard");
        assertNull(fields.get("Reference")
                         .get("readOnly"));
        assertNull(fields.get("Reference")
                         .get("pattern"));
        // the process guard: the delete of a record whose instance still runs is refused with 409
        assertEquals(List.of("Sales Invoice Posting"), invoice.get("deleteGuardedByProcess"));
    }

    /** dirigible #7534: the translated seed rows leave the untranslated ISO code out. */
    private static final String LANGUAGES = """
            name: languages
            languages: [en, bg]
            entities:
              - name: Language
                multilingual: true
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: code, type: string, required: true, unique: true, length: 3 }
                  - { name: name, type: string, required: true, length: 100 }
            seeds:
              - name: languages
                entity: Language
                rows:
                  - { id: 1, code: en, name: English }
              - name: languages-bg
                entity: Language
                language: bg
                rows:
                  - %s
            """;

    @SuppressWarnings("unchecked")
    @Test
    void takesTheSampleFromAFieldTheTranslatedRowCarries() {
        IntentModel languages = IntentParser.parse(LANGUAGES.formatted("{ id: 1, name: \"Английски\" }"));
        IntentGenerationContext context = TestContexts.context(languages);

        Map<String, Object> language =
                entity(AppTestIntentGenerator.buildManifest("languages", "languages", languages, languagesEdm(), context), "Language");

        // `code` comes first and has a language column, but the bg row does not set it - the sample
        // must come from `name`, the first field BOTH rows carry
        assertEquals(Map.of("language", "bg", "base", "English", "translated", "Английски"), language.get("multilingualSample"));
        assertTrue(context.getIssues()
                          .isEmpty(),
                String.valueOf(context.getIssues()));
    }

    @Test
    void reportsATranslationSampleThatCannotBeDerived() {
        IntentModel languages = IntentParser.parse(LANGUAGES.formatted("{ id: 2, name: \"Английски\" }"));
        IntentGenerationContext context = TestContexts.context(languages);

        Map<String, Object> language =
                entity(AppTestIntentGenerator.buildManifest("languages", "languages", languages, languagesEdm(), context), "Language");

        assertEquals(Boolean.TRUE, language.get("multilingual"));
        assertNull(language.get("multilingualSample"));
        // the dropped assertion is named, never silent
        assertEquals(1, context.getIssues()
                               .size());
        String issue = context.getIssues()
                              .get(0);
        assertTrue(issue.contains("[Language]") && issue.contains("[bg]") && issue.contains("languages-bg"), issue);
    }

    @Test
    void staysQuietWithoutInlineTranslatedRows() {
        // only a base seed: nothing to assert, nothing to report
        IntentModel languages = IntentParser.parse(LANGUAGES.substring(0, LANGUAGES.indexOf("  - name: languages-bg")));
        IntentGenerationContext context = TestContexts.context(languages);

        Map<String, Object> language =
                entity(AppTestIntentGenerator.buildManifest("languages", "languages", languages, languagesEdm(), context), "Language");

        assertNull(language.get("multilingualSample"));
        assertTrue(context.getIssues()
                          .isEmpty(),
                String.valueOf(context.getIssues()));
    }

    private static Map<String, Map<String, Object>> languagesEdm() {
        Map<String, Map<String, Object>> edm = new LinkedHashMap<>();
        edm.put("Language", edmEntity("Language", "Language", "Languages", "MANAGE_LIST", "Settings", "master-data",
                "KF_MOD_LANGUAGES_LANGUAGE", true));
        return edm;
    }

    @SuppressWarnings("unchecked")
    @Test
    void omitsTheRouteOfAChildAndCarriesAgreeAndCompositeKeys() {
        String intent = """
                name: stock
                entities:
                  - name: Company
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, required: true, length: 200 }
                  - name: Store
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, required: true, length: 200 }
                    relations:
                      - { name: company, kind: manyToOne, to: Company, required: true }
                  - name: Transfer
                    unique:
                      - fields: [fromStore, toStore]
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: note, type: string, length: 200 }
                    relations:
                      - { name: fromStore, kind: manyToOne, to: Store, required: true }
                      - { name: toStore, kind: manyToOne, to: Store, required: true }
                    checks:
                      - { kind: agree, relations: [fromStore, toStore], onProperty: company, message: "same company" }
                """;
        Map<String, Map<String, Object>> edm = new LinkedHashMap<>();
        edm.put("Company", edmEntity("Company", "Company", "Companies", "MANAGE", "Stock", "stock", "KF_MOD_STOCK_COMPANY", false));
        // a composition child owning line items: a document layout, yet no power page of its own
        Map<String, Object> store = edmEntity("Store", "Store", "Stores", "MANAGE_DOCUMENT", "Stock", "", "KF_MOD_STOCK_STORE", false);
        store.put("type", "DEPENDENT");
        edm.put("Store", store);
        edm.put("Transfer", edmEntity("Transfer", "Transfer", "Transfers", "MANAGE", "Stock", "stock", "KF_MOD_STOCK_TRANSFER", false));

        Map<String, Object> manifest = AppTestIntentGenerator.buildManifest("stock", "stock", IntentParser.parse(intent), edm);

        assertNull(entity(manifest, "Store").get("route"), "a DEPENDENT entity has no power page to route to");
        assertEquals("#/Company", entity(manifest, "Company").get("route"));
        Map<String, Object> transfer = entity(manifest, "Transfer");
        assertEquals(List.of(List.of("FromStore", "ToStore")), transfer.get("uniqueKeys"));
        List<Map<String, Object>> agree = (List<Map<String, Object>>) transfer.get("agree");
        assertEquals(List.of("FromStore", "ToStore"), agree.get(0)
                                                           .get("relations"));
        assertEquals("Company", agree.get(0)
                                     .get("onProperty"));
        assertNull(entity(manifest, "Company").get("uniqueKeys"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void marksTheRelationsTheFormRendersReadOnly() {
        // dirigible #7554: a relation recomputed on update renders as a disabled combobox - without a
        // readOnly flag the UI flow tried to pick it and timed out on a correct app
        String intent =
                """
                        name: expenses
                        entities:
                          - name: Company
                            fields:
                              - { name: id, type: integer, primaryKey: true, generated: true }
                              - { name: name, type: string, required: true, length: 200 }
                          - name: Employee
                            fields:
                              - { name: id, type: integer, primaryKey: true, generated: true }
                              - { name: name, type: string, required: true, length: 200 }
                          - name: ExpenseClaim
                            fields:
                              - { name: id, type: integer, primaryKey: true, generated: true }
                              - { name: note, type: string, length: 200 }
                            relations:
                              - { name: Employee, kind: manyToOne, to: Employee, required: true }
                              - { name: Company, kind: manyToOne, to: Company, required: true, calculatedActionOnCreate: org.example.CompanyOf, calculatedActionOnUpdate: org.example.CompanyOf }
                              - { name: Approver, kind: manyToOne, to: Employee, calculatedActionOnCreate: org.example.DefaultApprover }
                              - { name: Owner, kind: manyToOne, to: Employee }
                        """;
        Map<String, Object> claimEdm = edmEntity("ExpenseClaim", "Expense Claim", "Expense Claims", "MANAGE_LIST", "Expenses", "hr",
                "KF_MOD_EXPENSES_EXPENSECLAIM", false);
        // a platform-owned FK: the EDM marks it read-only, the form renders no input for it
        claimEdm.put("properties",
                List.of(property("Id", Map.of("dataPrimaryKey", "true")), property("Owner", Map.of("isReadOnlyProperty", "true"))));
        Map<String, Map<String, Object>> edm = new LinkedHashMap<>();
        edm.put("Company", edmEntity("Company", "Company", "Companies", "MANAGE_LIST", "Settings", "hr", "KF_MOD_EXPENSES_COMPANY", false));
        edm.put("Employee",
                edmEntity("Employee", "Employee", "Employees", "MANAGE_LIST", "Employees", "hr", "KF_MOD_EXPENSES_EMPLOYEE", false));
        edm.put("ExpenseClaim", claimEdm);

        Map<String, Object> claim =
                entity(AppTestIntentGenerator.buildManifest("expenses", "expenses", IntentParser.parse(intent), edm), "ExpenseClaim");
        Map<String, Map<String, Object>> relations = new LinkedHashMap<>();
        for (Map<String, Object> relation : (List<Map<String, Object>>) claim.get("relations")) {
            relations.put(String.valueOf(relation.get("name")), relation);
        }

        assertNull(relations.get("Employee")
                            .get("readOnly"));
        assertEquals(Boolean.TRUE, relations.get("Company")
                                            .get("readOnly"));
        // a create-only action is a server-side DEFAULT - the form keeps it editable (#6696)
        assertNull(relations.get("Approver")
                            .get("readOnly"));
        assertEquals(Boolean.TRUE, relations.get("Owner")
                                            .get("readOnly"));
    }

    /**
     * The three things a curated, guarded module needs the runner to know (#7663, #7664, #7667): which
     * target rows the picker actually offers, which columns the list actually renders, and which parent
     * state the child refuses outright.
     */
    @SuppressWarnings("unchecked")
    @Test
    void carriesThePickerRuleTheListColumnsAndTheParentStateAChildRefuses() {
        String intent = """
                name: billing
                entities:
                  - name: InvoiceStatus
                    kind: setting
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Invoice
                    fields:
                      - { name: id,     type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, length: 100 }
                    relations:
                      - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                  - name: CreditNote
                    list: [number, date, Invoice]
                    checks:
                      - { kind: forbidWhen, when: "Invoice.Status == DRAFT", message: "A credit note can only correct an issued invoice" }
                    fields:
                      - { name: id,           type: integer, primaryKey: true, generated: true }
                      - { name: number,       type: string, length: 100 }
                      - { name: date,         type: date }
                      - { name: taxEventDate, type: date }
                    relations:
                      - name: Invoice
                        kind: manyToOne
                        to: Invoice
                        required: true
                        pickable: { when: [Status != 1], message: "Only an issued invoice can be corrected" }
                seeds:
                  - name: invoice-statuses
                    entity: InvoiceStatus
                    rows:
                      - { id: 1, name: DRAFT }
                      - { id: 2, name: ISSUED }
                """;
        Map<String, Map<String, Object>> edmEntities = new LinkedHashMap<>();
        edmEntities.put("InvoiceStatus", edmEntity("InvoiceStatus", "Invoice Status", "Invoice Statuses", "MANAGE_LIST", "Settings",
                "billing", "KF_MOD_BILLING_INVOICESTATUS", false));
        edmEntities.put("Invoice",
                edmEntity("Invoice", "Invoice", "Invoices", "MANAGE_LIST", "Billing", "billing", "KF_MOD_BILLING_INVOICE", false));
        Map<String, Object> creditNote = edmEntity("CreditNote", "Credit Note", "Credit Notes", "MANAGE_LIST", "Billing", "billing",
                "KF_MOD_BILLING_CREDITNOTE", false);
        // what applyListColumns writes onto the .model entity for a declared `list:`
        creditNote.put("listOrder", "Number,Date,Invoice");
        edmEntities.put("CreditNote", creditNote);

        Map<String, Object> manifest = AppTestIntentGenerator.buildManifest("billing", "billing", IntentParser.parse(intent), edmEntities);
        Map<String, Object> note = entity(manifest, "CreditNote");

        // #7664: the curated columns, in order - the list renders these and no other, whatever each
        // field's own `major` says, so TaxEventDate must not be expected as a header.
        assertEquals(List.of("Number", "Date", "Invoice"), note.get("list"));

        Map<String, Object> invoice = ((List<Map<String, Object>>) note.get("relations")).get(0);

        // #7663: the picker offers only the rows the rule admits, so the runner must sample one of
        // those - the seeded name is already an id by the time the manifest is written.
        Map<String, Object> pickable = (Map<String, Object>) invoice.get("pickable");
        assertEquals(List.of(Map.of("by", "Status", "op", "ne", "value", 1L)), pickable.get("when"));
        assertEquals(Boolean.FALSE, pickable.get("hide"));

        // #7667: and the REST flow, which never sees a picker, is told the same thing by the check
        // that would refuse its create with 400.
        List<Map<String, Object>> forbidden = (List<Map<String, Object>>) invoice.get("forbiddenTarget");
        assertEquals(1, forbidden.size());
        assertEquals("Status", forbidden.get(0)
                                        .get("by"));
        assertEquals("eq", forbidden.get(0)
                                    .get("op"));
        assertEquals(1L, forbidden.get(0)
                                  .get("value"));
        assertEquals("A credit note can only correct an issued invoice", forbidden.get(0)
                                                                                  .get("message"));
        // Unscoped, it covers create, update and delete (#7372) and the manifest says nothing more.
        assertNull(forbidden.get(0)
                            .get("verbs"));

        // #7710: a scoped rule states its verbs, so a flow expects the delete to succeed.
        Map<String, Object> scopedNote = entity(AppTestIntentGenerator.buildManifest("billing", "billing",
                IntentParser.parse(intent.replace("message: \"A credit note", "verbs: [create, update], message: \"A credit note")),
                edmEntities), "CreditNote");
        Map<String, Object> scopedInvoice = ((List<Map<String, Object>>) scopedNote.get("relations")).get(0);
        assertEquals(List.of("create", "update"), ((List<Map<String, Object>>) scopedInvoice.get("forbiddenTarget")).get(0)
                                                                                                                    .get("verbs"));
    }

    /** An uncurated, unguarded relation carries none of the three - the manifest stays as it was. */
    @SuppressWarnings("unchecked")
    @Test
    void anUncuratedEntityCarriesNeitherListNorPickerRule() {
        Map<String, Object> city = entity(AppTestIntentGenerator.buildManifest("countries", "countries", model, edm()), "City");

        assertNull(city.get("list"));
        Map<String, Object> country = ((List<Map<String, Object>>) city.get("relations")).get(0);
        assertNull(country.get("pickable"));
        assertNull(country.get("forbiddenTarget"));
    }

    // ---- helpers: a minimal .model-shaped metadata map -------------------------------------------

    private static Map<String, Map<String, Object>> edm() {
        Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
        byName.put("Country",
                edmEntity("Country", "Country", "Countries", "MANAGE_MASTER", "Settings", "master-data", "KF_MOD_COUNTRIES_COUNTRY", true));
        byName.put("City", edmEntity("City", "City", "Cities", "MANAGE_MASTER", "Settings", "master-data", "KF_MOD_COUNTRIES_CITY", false));
        byName.put("Account",
                edmEntity("Account", "Account", "Accounts", "MANAGE_LIST", "Accounts", "master-data", "KF_MOD_COUNTRIES_ACCOUNT", false));
        return byName;
    }

    /** One {@code .model} property: its PascalCase name plus the attributes the manifest reads. */
    private static Map<String, Object> property(String name, Map<String, String> attributes) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", name);
        property.putAll(attributes);
        return property;
    }

    private static Map<String, Object> edmEntity(String name, String label, String plural, String layoutType, String perspective,
            String navId, String table, boolean multilingual) {
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("name", name);
        entity.put("entityLabel", label);
        entity.put("menuLabel", plural);
        entity.put("layoutType", layoutType);
        entity.put("perspectiveName", perspective);
        entity.put("perspectiveNavId", navId);
        entity.put("dataName", table);
        entity.put("multilingual", String.valueOf(multilingual));
        Map<String, Object> pk = new LinkedHashMap<>();
        pk.put("name", "Id");
        pk.put("dataPrimaryKey", "true");
        entity.put("properties", List.of(pk));
        return entity;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(Map<String, Object> manifest, String name) {
        Map<String, Object> found = entityOrNull(manifest, name);
        assertTrue(found != null, "entity " + name + " present");
        return found;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entityOrNull(Map<String, Object> manifest, String name) {
        for (Object entity : (List<Object>) manifest.get("entities")) {
            Map<String, Object> map = (Map<String, Object>) entity;
            if (name.equals(map.get("name"))) {
                return map;
            }
        }
        return null;
    }
}
