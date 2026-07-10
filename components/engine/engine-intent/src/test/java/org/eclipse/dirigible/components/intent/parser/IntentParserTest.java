/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

class IntentParserTest {

    private static final String HEAD = """
            name: lib
            entities:
              - name: Member
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string }
              - name: Loan
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: member, kind: manyToOne, to: Member }
            notifications:
              - name: loanUpdated
                event: { onUpdate: Loan }
                subject: "x"
            """;

    @Test
    void braceRecipientReportsACleanIssueInsteadOfA500() {
        // `to: {member.email}` is YAML flow-mapping (an object), not a string. The typed mapping used
        // to throw a raw Gson JsonSyntaxException (-> 500); it must now be a clean validation issue.
        String yaml = HEAD.stripTrailing() + "\n    to: {member.email}\n";
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("wrong type")),
                "a wrong-typed scalar should be reported as a validation issue, got: " + ex.getIssues());
    }

    @Test
    void bareOneHopRelationFieldRecipientParses() {
        // `to: member.email` (bare, no braces) is the supported one-hop relation.field recipient.
        String yaml = HEAD.stripTrailing() + "\n    to: member.email\n";
        IntentModel model = IntentParser.parse(yaml);
        assertEquals("member.email", model.getNotifications()
                                          .get(0)
                                          .getTo());
    }

    @Test
    void crossModelRelationParsesWhenModelIsDeclaredInUses() {
        String yaml = """
                name: customers
                uses:
                  - { model: countries }
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Country, kind: manyToOne, to: Country, model: countries }
                """;
        IntentModel model = IntentParser.parse(yaml);
        assertEquals("countries", model.getEntities()
                                       .get(0)
                                       .getRelations()
                                       .get(0)
                                       .getModel());
        assertTrue(model.getEntities()
                        .get(0)
                        .getRelations()
                        .get(0)
                        .isCrossModel());
    }

    @Test
    void crossModelRelationToUndeclaredModelIsRejected() {
        String yaml = """
                name: customers
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Country, kind: manyToOne, to: Country, model: countries }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("undeclared model")),
                "expected an undeclared-model issue, got: " + ex.getIssues());
    }

    @Test
    void crossModelCompositionIsRejected() {
        String yaml = """
                name: sales
                uses:
                  - { model: customers }
                entities:
                  - name: SalesInvoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer, model: customers, composition: true }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("cannot be a composition")),
                "expected a cross-model composition issue, got: " + ex.getIssues());
    }

    @Test
    void intraModelDanglingRelationTargetStillRejected() {
        String yaml = """
                name: lib
                entities:
                  - name: Loan
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: book, kind: manyToOne, to: Book }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("unknown entity")),
                "expected an unknown-entity issue, got: " + ex.getIssues());
    }

    @Test
    void uniqueAndCalculatedFieldAttributesParse() {
        String yaml = """
                name: sales
                entities:
                  - name: SalesInvoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: uuid, type: uuid, unique: true }
                      - { name: number, type: string, calculatedOnCreate: "java.util.UUID.randomUUID().toString()" }
                """;
        IntentModel model = IntentParser.parse(yaml);
        var fields = model.getEntities()
                          .get(0)
                          .getFields();
        assertTrue(fields.get(1)
                         .isUnique());
        assertTrue(fields.get(2)
                         .isCalculated());
    }

    /** A shop model with the two canonical Depends-On shapes: a cascade and a scalar auto-populate. */
    private static final String DEPENDS_ON_HEAD = """
            name: shop
            entities:
              - name: Country
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: City
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                relations:
                  - { name: Country, kind: manyToOne, to: Country }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                relations:
                  - { name: Country, kind: manyToOne, to: Country }
            """;

    @Test
    void dependsOnCascadeAndAutoPopulateParse() {
        // filterBy/valueFrom reference the target's properties by their AUTHORED names (a field by its
        // lower-camel name, a relation by its declared name) - City's FK to Country is the relation
        // named `Country`.
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: City, kind: manyToOne, to: City, dependsOn: { relation: Country, filterBy: Country } }
                """;
        IntentModel model = IntentParser.parse(yaml);
        var dependsOn = model.getEntities()
                             .get(2)
                             .getRelations()
                             .get(1)
                             .getDependsOn();
        assertEquals("Country", dependsOn.getRelation());
        assertEquals("Country", dependsOn.getFilterBy());
    }

    @Test
    void immutableInParsesAndRequiresAnEntityStatus() {
        String yaml = """
                name: ledger
                entities:
                  - name: EntryStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: JournalEntry
                    immutableIn: [2, 3]
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                """;
        IntentModel model = IntentParser.parse(yaml);
        assertEquals(java.util.List.of(2, 3), model.getEntities()
                                                   .get(1)
                                                   .getImmutableIn());

        String noStatus = """
                name: ledger
                entities:
                  - name: JournalEntry
                    immutableIn: [2]
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(noStatus));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("requires a `function: EntityStatus` relation")),
                "expected a missing-status issue, got: " + ex.getIssues());
    }

    @Test
    void checksParseAndValidate() {
        String yaml = """
                name: ledger
                entities:
                  - name: EntryStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: JournalEntry
                    checks:
                      - { kind: itemsSumEqual, over: [debit, credit], status: 2, message: "Must balance" }
                      - { kind: itemsMin, count: 1, status: 2, message: "Needs a line" }
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                  - name: JournalEntryItem
                    checks:
                      - { kind: exactlyOne, fields: [debit, credit], message: "Debit or credit" }
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal }
                      - { name: credit, type: decimal }
                    relations:
                      - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
                """;
        IntentModel model = IntentParser.parse(yaml);
        assertEquals(2, model.getEntities()
                             .get(1)
                             .getChecks()
                             .size());

        // A document check without a status gate would forbid drafting - rejected.
        String noGate = yaml.replace(", status: 2, message: \"Must balance\"", ", message: \"Must balance\"");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(noGate));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("requires a `status` gate")),
                "expected a gate issue, got: " + ex.getIssues());
    }

    @Test
    void hierarchyAndLeafOnlyParse() {
        String yaml = """
                name: ledger
                entities:
                  - name: Account
                    hierarchy: Parent
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, required: true, length: 10 }
                    relations:
                      - { name: Parent, kind: manyToOne, to: Account }
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal }
                    relations:
                      - { name: Account, kind: manyToOne, to: Account, required: true, leafOnly: true }
                """;
        IntentModel model = IntentParser.parse(yaml);
        assertEquals("Parent", model.getEntities()
                                    .get(0)
                                    .getHierarchy());
        assertTrue(model.getEntities()
                        .get(1)
                        .getRelations()
                        .get(0)
                        .isLeafOnly());
    }

    @Test
    void hierarchyMustNameAnOptionalSelfRelation() {
        String yaml = """
                name: ledger
                entities:
                  - name: Category
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Account
                    hierarchy: Category
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Category, kind: manyToOne, to: Category }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("must target the entity itself")),
                "expected a self-relation issue, got: " + ex.getIssues());
    }

    @Test
    void leafOnlyRequiresAHierarchicalTarget() {
        String yaml = """
                name: ledger
                entities:
                  - name: Account
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Account, kind: manyToOne, to: Account, leafOnly: true }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("declares no hierarchy")),
                "expected a no-hierarchy issue, got: " + ex.getIssues());
    }

    @Test
    void whereStaticOptionFilterParses() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: HomeCity, kind: manyToOne, to: City, where: { Country: 1 } }
                """;
        IntentModel model = IntentParser.parse(yaml);
        var where = model.getEntities()
                         .get(2)
                         .getRelations()
                         .get(1)
                         .getWhere();
        assertEquals(1, where.size());
        assertEquals(1L, where.get("Country"));
    }

    @Test
    void whereWithUnknownTargetPropertyIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: HomeCity, kind: manyToOne, to: City, where: { region: 1 } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("where [region] is not a field or to-one relation")),
                "expected an unknown-property issue, got: " + ex.getIssues());
    }

    @Test
    void whereWithMultipleConditionsIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: HomeCity, kind: manyToOne, to: City, where: { Country: 1, name: Sofia } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("where must be a single")),
                "expected a single-pair issue, got: " + ex.getIssues());
    }

    @Test
    void dependsOnUnknownTriggerRelationIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: City, kind: manyToOne, to: City, dependsOn: { relation: Region, filterBy: country } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("dependsOn relation [Region] is not a to-one relation")),
                "expected a dangling-trigger issue, got: " + ex.getIssues());
    }

    @Test
    void dependsOnSelfTriggerIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: City, kind: manyToOne, to: City, dependsOn: { relation: City, filterBy: country } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("cannot reference itself")),
                "expected a self-trigger issue, got: " + ex.getIssues());
    }

    @Test
    void dependsOnUnknownFilterByOnOwnTargetIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: City, kind: manyToOne, to: City, dependsOn: { relation: Country, filterBy: region } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("filterBy [region] is not a field or to-one relation of [City]")),
                "expected an unknown-filterBy issue, got: " + ex.getIssues());
    }

    @Test
    void dependsOnUnknownValueFromOnTriggerTargetIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: City, kind: manyToOne, to: City, dependsOn: { relation: Country, valueFrom: iso, filterBy: country } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("valueFrom [iso] is not a field or to-one relation of [Country]")),
                "expected an unknown-valueFrom issue, got: " + ex.getIssues());
    }

    @Test
    void dependsOnRelationWithNeitherValueFromNorFilterByIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing() + """

                      - { name: City, kind: manyToOne, to: City, dependsOn: { relation: Country } }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("requires `valueFrom` and/or `filterBy`")),
                "expected a missing-valueFrom/filterBy issue, got: " + ex.getIssues());
    }

    @Test
    void dependsOnFieldRequiresValueFromAndForbidsFilterBy() {
        String yaml = """
                name: shop
                entities:
                  - name: Product
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: price, type: decimal }
                  - name: OrderItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: price, type: decimal, dependsOn: { relation: Product, filterBy: price } }
                    relations:
                      - { name: Product, kind: manyToOne, to: Product }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("requires `valueFrom`")),
                "expected a missing-valueFrom issue, got: " + ex.getIssues());
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("`filterBy` applies only to a relation")),
                "expected a filterBy-on-field issue, got: " + ex.getIssues());
    }

    /** A multilingual UoM setting with a Bulgarian translation seed. */
    private static final String MULTILINGUAL_HEAD = """
            name: uoms
            languages: [en, bg]
            entities:
              - name: UoM
                kind: setting
                multilingual: true
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }
                  - { name: numerator, type: decimal }
            seeds:
              - name: uoms-bg
                entity: UoM
                language: bg
                rows:
                  - { id: 1, name: "Килограм" }
            """;

    @Test
    void multilingualEntityAndLanguageSeedParse() {
        IntentModel model = IntentParser.parse(MULTILINGUAL_HEAD);
        assertTrue(model.getEntities()
                        .get(0)
                        .isMultilingual());
        assertEquals("bg", model.getSeeds()
                                .get(0)
                                .getLanguage());
        assertEquals(java.util.List.of("en", "bg"), model.getLanguages());
    }

    @Test
    void languageSeedOnNonMultilingualEntityIsRejected() {
        String yaml = MULTILINGUAL_HEAD.replace("    multilingual: true\n", "");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("is not multilingual - add `multilingual: true`")),
                "expected a not-multilingual issue, got: " + ex.getIssues());
    }

    @Test
    void languageSeedWithNonTranslatableRowKeyIsRejected() {
        String yaml = MULTILINGUAL_HEAD.replace("name: \"Килограм\"", "name: \"Килограм\", numerator: 5");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("[numerator] which is not the id or a translatable")),
                "expected a non-translatable row-key issue, got: " + ex.getIssues());
    }

    @Test
    void malformedLanguageCodesAreRejected() {
        String yaml = MULTILINGUAL_HEAD.replace("languages: [en, bg]", "languages: [en, Bulgarian]")
                                       .replace("language: bg", "language: BG");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("languages entry [Bulgarian]")),
                "expected a languages-entry issue, got: " + ex.getIssues());
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("language [BG] must be a short lowercase language code")),
                "expected a seed-language issue, got: " + ex.getIssues());
    }

    @Test
    void fileSeedParsesAndRootLevelOrAbsolutePathsAreRejected() {
        String head = """
                name: countries
                entities:
                  - name: Country
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, required: true }
                seeds:
                  - name: countries
                    entity: Country
                """;
        // A subfolder path with no inline rows is the valid shape for a large authored data set.
        IntentModel model = IntentParser.parse(head.stripTrailing() + "\n    file: data/countries.csv\n");
        assertEquals("data/countries.csv", model.getSeeds()
                                                .get(0)
                                                .getFile());

        // A root-level file would be scrubbed by the intent regeneration - rejected with guidance.
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(head.stripTrailing() + "\n    file: countries.csv\n"));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("must live in a subfolder")),
                "expected a subfolder issue, got: " + ex.getIssues());

        // Absolute / escaping paths are rejected.
        ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(head.stripTrailing() + "\n    file: /countries/data.csv\n"));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("must be a project-relative path")),
                "expected a relative-path issue, got: " + ex.getIssues());

        // file and inline rows are mutually exclusive.
        ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(head.stripTrailing() + """

                    file: data/countries.csv
                    rows:
                      - { id: 1, name: Afghanistan }
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("declares both `file` and inline `rows`")),
                "expected a mutual-exclusion issue, got: " + ex.getIssues());
    }

    @Test
    void dependsOnOnEntityStatusRelationIsRejected() {
        String yaml = DEPENDS_ON_HEAD.stripTrailing()
                + """

                              - { name: City, kind: manyToOne, to: City, function: EntityStatus, dependsOn: { relation: Country, filterBy: country } }
                        """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("EntityStatus (a read-only badge) so it cannot declare dependsOn")),
                "expected an EntityStatus-dependent issue, got: " + ex.getIssues());
    }

    private static final String WIDGET_HEAD = """
            name: sales
            entities:
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: issuedOn, type: date }
                  - { name: total, type: decimal }
            reports:
              - name: RevenueByMonth
                source: Invoice
                dimensions: ["month(issuedOn)"]
                measures: ["sum(total)"]
                widget:
                  value: "sum(total)"
                  at: { "month(issuedOn)": now }
            """;

    @Test
    void reportWidgetParses() {
        IntentModel model = IntentParser.parse(WIDGET_HEAD);
        assertEquals("sum(total)", model.getReports()
                                        .get(0)
                                        .getWidget()
                                        .getValue());
        assertEquals("now", model.getReports()
                                 .get(0)
                                 .getWidget()
                                 .getAt()
                                 .get("month(issuedOn)"));
    }

    @Test
    void widgetValueMustNameADeclaredMeasure() {
        String yaml = WIDGET_HEAD.replace("value: \"sum(total)\"", "value: \"avg(total)\"");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("does not name a declared measure")),
                "expected an unknown-measure issue, got: " + ex.getIssues());
    }

    @Test
    void widgetPinMustNameADeclaredDimension() {
        String yaml = WIDGET_HEAD.replace("at: { \"month(issuedOn)\": now }", "at: { \"year(issuedOn)\": now }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("pins unknown dimension [year(issuedOn)]")),
                "expected an unknown-dimension issue, got: " + ex.getIssues());
    }

    @Test
    void widgetUnknownKindIsRejected() {
        String yaml = WIDGET_HEAD.replace("value: \"sum(total)\"", "kind: chart");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("unknown kind [chart]")),
                "expected an unknown-kind issue, got: " + ex.getIssues());
    }

    @Test
    void widgetLimitIsListOnly() {
        String yaml = WIDGET_HEAD.stripTrailing() + "\n      limit: 5\n";
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("must not declare `limit`")),
                "expected a limit-misuse issue, got: " + ex.getIssues());
    }

    @Test
    void widgetValueOnCountKindIsRejected() {
        String yaml = WIDGET_HEAD.replace("widget:", "widget:\n      kind: count");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("must not declare `value`")),
                "expected a value-on-count issue, got: " + ex.getIssues());
    }

    private static final String CUSTOM_WIDGET_HEAD = """
            name: sales
            entities:
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
            widgets:
              - name: SystemHealth
                kind: kpi
                url: /services/js/sales/custom/health.js
                label: System Health
                icon: activity
              - name: SalesFunnel
                kind: page
                url: /services/web/sales/custom/funnel/index.html
            """;

    @Test
    void customWidgetsParse() {
        IntentModel model = IntentParser.parse(CUSTOM_WIDGET_HEAD);
        assertEquals(2, model.getWidgets()
                             .size());
        assertEquals("kpi", model.getWidgets()
                                 .get(0)
                                 .getKind());
        assertEquals("/services/web/sales/custom/funnel/index.html", model.getWidgets()
                                                                          .get(1)
                                                                          .getUrl());
    }

    @Test
    void customWidgetWithoutUrlIsRejected() {
        String yaml = CUSTOM_WIDGET_HEAD.replace("    url: /services/js/sales/custom/health.js\n", "");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("widget [SystemHealth] has no url")),
                "expected a missing-url issue, got: " + ex.getIssues());
    }

    @Test
    void customWidgetWithCrossOriginUrlIsRejected() {
        String yaml = CUSTOM_WIDGET_HEAD.replace("/services/js/sales/custom/health.js", "https://example.com/kpi");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("url must be a same-origin path")),
                "expected a same-origin issue, got: " + ex.getIssues());
    }

    @Test
    void customWidgetWithUnknownKindIsRejected() {
        String yaml = CUSTOM_WIDGET_HEAD.replace("kind: kpi", "kind: chart");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("has unknown kind [chart]")),
                "expected an unknown-kind issue, got: " + ex.getIssues());
    }

    private static final String SCHEDULE_GEN_HEAD = """
            name: hr
            entities:
              - name: Employee
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: status, type: string }
              - name: EmployeeTimesheet
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee }
            schedules:
              - name: monthly-timesheets
                cron: "0 0 1 1 * ?"
                entity: Employee
            """;

    @Test
    void scheduleGenerateParsesWithoutIssues() {
        String yaml = SCHEDULE_GEN_HEAD + """
                    generate:
                      to: EmployeeTimesheet
                      map:
                        Employee: id
                """;
        // A well-formed scheduled generation validates cleanly (no exception).
        IntentParser.parse(yaml);
    }

    @Test
    void scheduleWithBothNotifyAndGenerateIsRejected() {
        String yaml = SCHEDULE_GEN_HEAD + """
                    notify:
                      to: status
                      subject: "x"
                      body: "y"
                    generate:
                      to: EmployeeTimesheet
                      map:
                        Employee: id
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("has both notify and generate")),
                "expected a both-actions issue, got: " + ex.getIssues());
    }

    @Test
    void scheduleWithNoActionIsRejected() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(SCHEDULE_GEN_HEAD));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("has no action")),
                "expected a no-action issue, got: " + ex.getIssues());
    }

    @Test
    void scheduleGenerateWithUnknownTargetIsRejected() {
        String yaml = SCHEDULE_GEN_HEAD + """
                    generate:
                      to: Nonexistent
                      map:
                        Employee: id
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("generate to references unknown entity [Nonexistent]")),
                "expected an unknown-target issue, got: " + ex.getIssues());
    }

    @Test
    void scheduleGenerateWithItemsIsRejected() {
        String yaml = SCHEDULE_GEN_HEAD + """
                    generate:
                      to: EmployeeTimesheet
                      map:
                        Employee: id
                      items:
                        from: Employee
                        to: EmployeeTimesheet
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("item cloning is not supported for a scheduled generation")),
                "expected an items-not-supported issue, got: " + ex.getIssues());
    }

    @Test
    void scheduleGenerateWithBadMapSourceIsRejected() {
        String yaml = SCHEDULE_GEN_HEAD + """
                    generate:
                      to: EmployeeTimesheet
                      map:
                        Employee: nonexistentField
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("generate map source [nonexistentField] is not a field or to-one relation of [Employee]")),
                "expected a bad-map-source issue, got: " + ex.getIssues());
    }
}
