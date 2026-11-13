/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.parsers.document.ColumnNode;
import org.eclipse.dirigible.parsers.document.Node;
import org.eclipse.dirigible.parsers.document.RowNode;
import org.eclipse.dirigible.parsers.document.TableNode;
import org.eclipse.dirigible.parsers.document.parser.DocumentParser;
import org.junit.Test;

public class DataBinderTest {

    private final DocumentParser parser = new DocumentParser();
    private final DataBinder binder = new DataBinder();

    @Test
    public void substitutesPlaceholdersInTextAndAttributes() {
        Node root = parser.parse("<document><field label=\"No\">{{invoice.number}}</field><image src=\"{{company.logo}}\"/></document>");
        Node bound = binder.bind(root, Map.of("invoice", Map.of("number", "SI-1"), "company", Map.of("logo", "/logo.png")));
        assertEquals("SI-1", bound.children()
                                  .get(0)
                                  .text());
        assertEquals("/logo.png", bound.children()
                                       .get(1)
                                       .attributes()
                                       .get("src"));
    }

    @Test
    public void unresolvedPlaceholdersRenderEmpty() {
        Node root = parser.parse("<document><text>Hi {{missing.path}}!</text></document>");
        Node bound = binder.bind(root, Map.of());
        assertEquals("Hi !", bound.children()
                                  .get(0)
                                  .text());
    }

    @Test
    public void mixedTextAroundPlaceholdersIsPreserved() {
        Node root = parser.parse("<document><text>Page {{page}} of {{pages}}</text></document>");
        Node bound = binder.bind(root, Map.of("page", 1, "pages", 3));
        assertEquals("Page 1 of 3", bound.children()
                                         .get(0)
                                         .text());
    }

    @Test
    public void tableExpandsOneRowPerSourceElement() {
        Node root = parser.parse("""
                <document>
                    <table source="invoice.items">
                        <column width="3*">{{description}}</column>
                        <column width="*" align="right">{{amount}}</column>
                    </table>
                </document>
                """);
        Node bound = binder.bind(root, Map.of("invoice", Map.of("items",
                List.of(Map.of("description", "Widget", "amount", "10.00"), Map.of("description", "Gadget", "amount", "20.00")))));
        TableNode table = (TableNode) bound.children()
                                           .get(0);
        // 2 column definitions (templates dropped) + 2 expanded rows
        assertEquals(4, table.children()
                             .size());
        assertTrue(table.children()
                        .get(0) instanceof ColumnNode);
        assertEquals("", table.children()
                              .get(0)
                              .text());
        RowNode firstRow = (RowNode) table.children()
                                          .get(2);
        assertEquals("Widget", firstRow.children()
                                       .get(0)
                                       .text());
        assertEquals("10.00", firstRow.children()
                                      .get(1)
                                      .text());
        assertEquals("right", firstRow.children()
                                      .get(1)
                                      .attributes()
                                      .get("align"));
    }

    @Test
    public void rowScopeFallsBackToTheDocumentScope() {
        Node root = parser.parse("""
                <document>
                    <table source="items">
                        <column>{{name}} ({{currency}})</column>
                    </table>
                </document>
                """);
        Node bound = binder.bind(root, Map.of("currency", "EUR", "items", List.of(Map.of("name", "Widget"))));
        TableNode table = (TableNode) bound.children()
                                           .get(0);
        assertEquals("Widget (EUR)", table.children()
                                          .get(1)
                                          .children()
                                          .get(0)
                                          .text());
    }

    @Test
    public void emptyOrMissingTableSourceYieldsNoRows() {
        Node root = parser.parse("<document><table source=\"missing\"><column>{{x}}</column></table></document>");
        TableNode table = (TableNode) binder.bind(root, Map.of())
                                            .children()
                                            .get(0);
        assertEquals(1, table.children()
                             .size());
    }

    @Test
    public void forExpandsItsChildrenPerElement() {
        Node root = parser.parse("""
                <document>
                    <for source="transactions">
                        <text>{{date}}: {{amount}}</text>
                    </for>
                </document>
                """);
        Node bound = binder.bind(root,
                Map.of("transactions", List.of(Map.of("date", "01-01", "amount", "5"), Map.of("date", "01-02", "amount", "7"))));
        assertEquals(2, bound.children()
                             .size());
        assertEquals("01-01: 5", bound.children()
                                      .get(0)
                                      .text());
        assertEquals("01-02: 7", bound.children()
                                      .get(1)
                                      .text());
    }

    @Test
    public void ifKeepsChildrenWhenTruthyAndDropsThemWhenFalsy() {
        String template = "<document><if source=\"invoice.hasDiscount\"><text>Discount!</text></if></document>";
        Node kept = binder.bind(parser.parse(template), Map.of("invoice", Map.of("hasDiscount", true)));
        assertEquals(1, kept.children()
                            .size());
        for (Object falsy : new Object[] {false, "", 0, List.of(), Map.of()}) {
            Node dropped = binder.bind(parser.parse(template), Map.of("invoice", Map.of("hasDiscount", falsy)));
            assertEquals("Expected no children for " + falsy, 0, dropped.children()
                                                                        .size());
        }
        Node missing = binder.bind(parser.parse(template), Map.of());
        assertEquals(0, missing.children()
                               .size());
    }

    @Test
    public void tableFilterAloneKeepsTruthyRows() {
        Node root = parser.parse("""
                <document>
                    <table source="items" filter="billable">
                        <column>{{name}}</column>
                    </table>
                </document>
                """);
        Node bound = binder.bind(root, Map.of("items",
                List.of(Map.of("name", "Kept", "billable", true), Map.of("name", "Dropped", "billable", false), Map.of("name", "NoFlag"))));
        TableNode table = (TableNode) bound.children()
                                           .get(0);
        // 1 column definition + only the truthy row
        assertEquals(2, table.children()
                             .size());
        assertEquals("Kept", table.children()
                                  .get(1)
                                  .children()
                                  .get(0)
                                  .text());
    }

    @Test
    public void tableFilterWithMatchKeepsTheListedValues() {
        Node root = parser.parse("""
                <document>
                    <table source="items" filter="kind" match="CONTRIBUTION | TAX">
                        <column>{{name}}</column>
                    </table>
                </document>
                """);
        Node bound = binder.bind(root,
                Map.of("items", List.of(Map.of("kind", "BASE", "name", "Base salary"), Map.of("kind", "CONTRIBUTION", "name", "Pensions"),
                        Map.of("kind", "TAX", "name", "Income tax"), Map.of("name", "Kindless"))));
        TableNode table = (TableNode) bound.children()
                                           .get(0);
        // 1 column definition + the two matching rows, in source order
        assertEquals(3, table.children()
                             .size());
        assertEquals("Pensions", table.children()
                                      .get(1)
                                      .children()
                                      .get(0)
                                      .text());
        assertEquals("Income tax", table.children()
                                        .get(2)
                                        .children()
                                        .get(0)
                                        .text());
    }

    @Test
    public void forFilterWithMatchExpandsOnlyMatchingElements() {
        Node root = parser.parse("""
                <document>
                    <for source="lines" filter="side" match="DEBIT">
                        <text>{{account}}</text>
                    </for>
                </document>
                """);
        Node bound = binder.bind(root,
                Map.of("lines", List.of(Map.of("side", "DEBIT", "account", "601"), Map.of("side", "CREDIT", "account", "401"))));
        assertEquals(1, bound.children()
                             .size());
        assertEquals("601", bound.children()
                                 .get(0)
                                 .text());
    }

    @Test
    public void ifMatchComparesTheResolvedValueInsteadOfTruthiness() {
        String template = "<document><if source=\"status\" match=\"POSTED | SENT\"><text>Final</text></if></document>";
        Node matched = binder.bind(parser.parse(template), Map.of("status", "POSTED"));
        assertEquals(1, matched.children()
                               .size());
        // a truthy-but-unlisted value does not match, and a missing value never matches
        Node other = binder.bind(parser.parse(template), Map.of("status", "DRAFT"));
        assertEquals(0, other.children()
                             .size());
        Node missing = binder.bind(parser.parse(template), Map.of());
        assertEquals(0, missing.children()
                               .size());
    }

    @Test
    public void nestedStructuresBindRecursively() {
        Node root = parser.parse("<document><section><row><stack><text>{{a.b}}</text></stack></row></section></document>");
        Node bound = binder.bind(root, Map.of("a", Map.of("b", "deep")));
        assertEquals("deep", bound.children()
                                  .get(0)
                                  .children()
                                  .get(0)
                                  .children()
                                  .get(0)
                                  .children()
                                  .get(0)
                                  .text());
    }

    @Test
    public void floatingPointValuesPrintInTheFormMoneyPattern() {
        Node root = parser.parse("<document><total align=\"right\">{{total}}</total><text>{{qty}} x {{price}}</text></document>");
        Node bound = binder.bind(root, Map.of("total", 1234567.5d, "qty", 2L, "price", new java.math.BigDecimal("100.00")));
        // doubles/BigDecimals: space-grouped thousands + two decimals (the generated forms' pattern);
        // integral numbers stay unformatted
        assertEquals("1 234 567.50", bound.children()
                                          .get(0)
                                          .text());
        assertEquals("2 x 100.00", bound.children()
                                        .get(1)
                                        .text());
    }

    @Test
    public void bindingIsRepeatableOnTheSameTemplate() {
        Node root = parser.parse("<document><text>{{n}}</text></document>");
        assertEquals("1", binder.bind(root, Map.of("n", 1))
                                .children()
                                .get(0)
                                .text());
        assertEquals("2", binder.bind(root, Map.of("n", 2))
                                .children()
                                .get(0)
                                .text());
    }

    @Test
    public void bareRelationNodeRendersItsLabelAndPathsDescend() {
        // A relation is provided as a nested object carrying __label (the print feeder shape): a bare
        // {{document.Customer}} shows the label, while {{document.Customer.Address.Street}} descends.
        Node root = parser.parse("<document><text>{{document.Customer}} @ {{document.Customer.Address.Street}}</text></document>");
        Map<String, Object> data =
                Map.of("document", Map.of("Customer", Map.of("__label", "BoomData", "Address", Map.of("Street", "558 Pacific Highway"))));
        Node bound = binder.bind(root, data);
        assertEquals("BoomData @ 558 Pacific Highway", bound.children()
                                                            .get(0)
                                                            .text());
    }

    @Test
    public void relationNodeWithoutLabelRendersEmpty() {
        Node root = parser.parse("<document><text>[{{document.Customer}}]</text></document>");
        Node bound = binder.bind(root, Map.of("document", Map.of("Customer", Map.of())));
        assertEquals("[]", bound.children()
                                .get(0)
                                .text());
    }

    @Test
    public void alternativePathsRenderTheFirstNonBlankOperand() {
        Node root = parser.parse("<document><text>{{document.NameLocal|document.Name}}</text></document>");
        Node bound = binder.bind(root, Map.of("document", Map.of("NameLocal", "Metafor OOD", "Name", "Metaphor Ltd.")));
        assertEquals("Metafor OOD", bound.children()
                                         .get(0)
                                         .text());
    }

    @Test
    public void alternativePathsFallThroughMissingBlankAndWhitespaceOnlyOperands() {
        Node root = parser.parse("<document><text>{{document.Missing|document.Empty|document.Spaces|document.Name}}</text></document>");
        // The three skipped shapes of "blank": absent from the context, the empty string, and
        // whitespace only - each one leaves the hole this fallback exists to close.
        Node bound = binder.bind(root, Map.of("document", Map.of("Empty", "", "Spaces", "   ", "Name", "Metaphor Ltd.")));
        assertEquals("Metaphor Ltd.", bound.children()
                                           .get(0)
                                           .text());
    }

    @Test
    public void allBlankAlternativesRenderEmptyExactlyAsASinglePathDoes() {
        Node root = parser.parse("<document><text>[{{document.NameLocal|document.Name}}][{{document.Name}}]</text></document>");
        Node bound = binder.bind(root, Map.of("document", Map.of()));
        assertEquals("[][]", bound.children()
                                  .get(0)
                                  .text());
    }

    @Test
    public void alternativePathsResolveInTheRowScopeAndAreTrimmed() {
        Node root = parser.parse("""
                <document>
                    <table source="items">
                        <column width="*">{{nameLocal | name}}</column>
                    </table>
                </document>
                """);
        // Every operand obeys the same scope rules as a single path: the row first, then the document.
        Node bound = binder.bind(root, Map.of("items", List.of(Map.of("nameLocal", "Widget-BG"), Map.of("name", "Gadget"))));
        TableNode table = (TableNode) bound.children()
                                           .get(0);
        assertEquals("Widget-BG", table.children()
                                       .get(1)
                                       .children()
                                       .get(0)
                                       .text());
        assertEquals("Gadget", table.children()
                                    .get(2)
                                    .children()
                                    .get(0)
                                    .text());
    }

    @Test
    public void aSingleWhitespaceOnlyPathStillRendersItsValue() {
        // No alternative to fall through to, so nothing changes for a lone path - the last operand is
        // always rendered as-is, which is what keeps every existing template byte-identical.
        Node root = parser.parse("<document><text>[{{document.Spaces}}]</text></document>");
        Node bound = binder.bind(root, Map.of("document", Map.of("Spaces", "  ")));
        assertEquals("[  ]", bound.children()
                                  .get(0)
                                  .text());
    }

    @Test
    public void formatSpecifierMoneyFormatsIntegralNumbers() {
        // The reason the specifier exists: a whole-figure money value arrives as an integral JSON
        // number (the browser strips the trailing .00), lands as a Long and the default rendering
        // rightly leaves integers alone - only the template author knows the column is money.
        Node root = parser.parse("<document><text>{{price:#,##0.00}} / {{big:#,##0.00}}</text></document>");
        Node bound = binder.bind(root, Map.of("price", 5390L, "big", 1234567L));
        assertEquals("5 390.00 / 1 234 567.00", bound.children()
                                                     .get(0)
                                                     .text());
    }

    @Test
    public void formatSpecifierOverridesTheDefaultMoneyPattern() {
        Node root = parser.parse("<document><text>{{total:0.00}}</text></document>");
        Node bound = binder.bind(root, Map.of("total", 5390.5d));
        // the explicit pattern wins over the default space-grouped one
        assertEquals("5390.50", bound.children()
                                     .get(0)
                                     .text());
    }

    @Test
    public void formatSpecifierFormatsTemporalsAndTheirIsoStrings() {
        // Feeders emit temporals as their ISO string - both the string and a real temporal reformat.
        Node root = parser.parse("<document><text>{{document.Date:dd.MM.yyyy}} / {{document.Day:dd.MM.yyyy}}</text></document>");
        Node bound = binder.bind(root, Map.of("document", Map.of("Date", "2026-08-29", "Day", java.time.LocalDate.of(2026, 8, 29))));
        assertEquals("29.08.2026 / 29.08.2026", bound.children()
                                                     .get(0)
                                                     .text());
    }

    @Test
    public void formatSpecifierSplitsAtTheFirstColonSoTimePatternsKeepTheirs() {
        Node root = parser.parse("<document><text>{{at:HH:mm}}</text></document>");
        Node bound = binder.bind(root, Map.of("at", "2026-08-29T10:15:30"));
        assertEquals("10:15", bound.children()
                                   .get(0)
                                   .text());
    }

    @Test
    public void formatAPatternCannotSatisfyFallsBackToTheDefaultRendering() {
        // A string is neither a number nor a temporal ('Widget'), and 'bb' is not a valid date
        // pattern - both render exactly as without the specifier: a printout never shows an exception.
        Node root = parser.parse("<document><text>{{name:0.00}} / {{document.Date:bb}}</text></document>");
        Node bound = binder.bind(root, Map.of("name", "Widget", "document", Map.of("Date", "2026-08-29")));
        assertEquals("Widget / 2026-08-29", bound.children()
                                                 .get(0)
                                                 .text());
    }

    @Test
    public void formatSpecifierCombinesWithAlternativePaths() {
        // Each operand carries its own format; blankness is judged on the rendered result.
        Node root = parser.parse("<document><text>{{document.Missing:0.00|document.Price:#,##0.00}}</text></document>");
        Node bound = binder.bind(root, Map.of("document", Map.of("Price", 5390L)));
        assertEquals("5 390.00", bound.children()
                                      .get(0)
                                      .text());
    }

    @Test
    public void wordsFormatSpellsTheAmountInTheTemplateLanguage() {
        Node root = parser.parse("<document><text>{{document.Total:words(document.Currency.Code)}}</text></document>");
        Node bound = new DataBinder("bg").bind(root, Map.of("document", Map.of("Total", 5264.44, "Currency", Map.of("Code", "EUR"))));
        assertEquals("ПЕТ ХИЛЯДИ ДВЕСТА ШЕСТДЕСЕТ И ЧЕТИРИ ЕВРО И 44 ЕВРОЦЕНТА", bound.children()
                                                                                      .get(0)
                                                                                      .text());
    }

    @Test
    public void wordsFormatTakesALiteralCurrencyCodeAndANumericString() {
        Node root = parser.parse("<document><text>{{Total:words(BGN)}}</text></document>");
        Node bound = new DataBinder("bg").bind(root, Map.of("Total", "2.50"));
        assertEquals("ДВА ЛЕВА И 50 СТОТИНКИ", bound.children()
                                                    .get(0)
                                                    .text());
    }

    @Test
    public void wordsFormatRendersEmptyWhenItCannotSpell() {
        Node root = parser.parse("<document><text>[{{Total:words(EUR)}}]</text><text>[{{Total:words(USD)}}]</text>"
                + "<text>[{{Name:words(EUR)}}]</text><text>[{{Total:words}}]</text></document>");
        Map<String, Object> data = Map.of("Total", 10, "Name", "ACME");
        Node german = new DataBinder("de").bind(root, data);
        Node bulgarian = new DataBinder("bg").bind(root, data);
        Node unknown = binder.bind(root, data);
        assertEquals("[]", german.children()
                                 .get(0)
                                 .text());
        assertEquals("[]", unknown.children()
                                  .get(0)
                                  .text());
        for (int i = 1; i < 4; i++) {
            assertEquals("[]", bulgarian.children()
                                        .get(i)
                                        .text());
        }
    }

    @Test
    public void wordsFormatSpellsInEnglishForAnEnglishTemplate() {
        Node root = parser.parse("<document><text>{{Total:words(Currency)}}</text></document>");
        Node bound = new DataBinder("en").bind(root, Map.of("Total", 5264.44, "Currency", "EUR"));
        assertEquals("FIVE THOUSAND TWO HUNDRED SIXTY-FOUR EUROS AND 44 CENTS", bound.children()
                                                                                     .get(0)
                                                                                     .text());
    }

    @Test
    public void wordsFormatCombinesWithAlternativePaths() {
        Node root = parser.parse("<document><text>{{document.Gross:words(EUR)|document.Total:words(EUR)}}</text></document>");
        Node bound = new DataBinder("bg").bind(root, Map.of("document", Map.of("Total", 1)));
        assertEquals("ЕДНО ЕВРО И 00 ЕВРОЦЕНТА", bound.children()
                                                      .get(0)
                                                      .text());
    }

    /**
     * Invoice lines at two VAT rates, the numbers JSON-decoded as doubles (the print payload's shape).
     */
    private static final List<Map<String, Object>> MIXED_RATE_LINES = List.of(Map.of("VatRate", 20.0, "Net", 10.0, "Vat", 2.0),
            Map.of("VatRate", 9.0, "Net", 22.8, "Vat", 2.05), Map.of("VatRate", 20.0, "Net", 15.0, "Vat", 3.0));

    @Test
    public void forGroupByExpandsOnePerRateWithExactSumsInRateOrder() {
        Node root = parser.parse("""
                <document>
                    <for source="items" groupBy="VatRate">
                        <text>{{VatRate:0.##}}%: {{sum(Net)}} / {{sum(Vat)}} ({{count()}})</text>
                    </for>
                </document>
                """);
        Node bound = binder.bind(root, Map.of("items", MIXED_RATE_LINES));
        assertEquals(2, bound.children()
                             .size());
        assertEquals("9%: 22.80 / 2.05 (1)", bound.children()
                                                  .get(0)
                                                  .text());
        assertEquals("20%: 25.00 / 5.00 (2)", bound.children()
                                                   .get(1)
                                                   .text());
    }

    @Test
    public void tableGroupByRendersOneRowPerRate() {
        Node root = parser.parse("""
                <document>
                    <table source="items" groupBy="VatRate">
                        <column>{{VatRate:0.##}}%</column>
                        <column>{{sum(Net)}}</column>
                        <column>{{sum(Vat)}}</column>
                    </table>
                </document>
                """);
        TableNode table = (TableNode) binder.bind(root, Map.of("items", MIXED_RATE_LINES))
                                            .children()
                                            .get(0);
        // 3 column definitions + one row per rate
        assertEquals(5, table.children()
                             .size());
        Node twentyPercent = table.children()
                                  .get(4);
        assertEquals("20%", twentyPercent.children()
                                         .get(0)
                                         .text());
        assertEquals("25.00", twentyPercent.children()
                                           .get(1)
                                           .text());
        assertEquals("5.00", twentyPercent.children()
                                          .get(2)
                                          .text());
    }

    @Test
    public void groupByKeepsOnlyFilteredRowsAndGroupsNumbersByValue() {
        Node root = parser.parse("""
                <document>
                    <for source="items" filter="billable" groupBy="VatRate">
                        <text>{{VatRate}}: {{sum(Net)}}</text>
                    </for>
                </document>
                """);
        // 20 (an int) and 20.0 (a double) are one rate; the unbillable line is not counted
        Node bound = binder.bind(root, Map.of("items", List.of(Map.of("VatRate", 20, "Net", 1.5, "billable", true),
                Map.of("VatRate", 20.0, "Net", 2.5, "billable", true), Map.of("VatRate", 20, "Net", 100.0, "billable", false))));
        assertEquals(1, bound.children()
                             .size());
        assertEquals("20: 4.00", bound.children()
                                      .get(0)
                                      .text());
    }

    @Test
    public void groupByOrdersTextKeysByTextAndPutsRowsWithoutTheKeyLast() {
        Node root = parser.parse("<document><for source=\"items\" groupBy=\"Kind\"><text>[{{Kind}}] {{count()}}</text></for></document>");
        Node bound = binder.bind(root,
                Map.of("items", List.of(Map.of("Kind", "TAX"), Map.of("Net", 1.0), Map.of("Kind", "BASE"), Map.of("Kind", "TAX"))));
        assertEquals(3, bound.children()
                             .size());
        assertEquals("[BASE] 1", bound.children()
                                      .get(0)
                                      .text());
        assertEquals("[TAX] 2", bound.children()
                                     .get(1)
                                     .text());
        assertEquals("[] 1", bound.children()
                                  .get(2)
                                  .text());
    }

    @Test
    public void aggregatesAndGroupKeysReadTheRowItselfNeverTheEnclosingContext() {
        Node root =
                parser.parse("<document><for source=\"items\" groupBy=\"VatRate\"><text>{{VatRate}}: {{sum(Vat)}}</text></for></document>");
        // the document-level Vat and VatRate must not leak into a row that lacks them
        Node bound = binder.bind(root, Map.of("Vat", 99.0, "VatRate", 7, "items",
                List.of(Map.of("VatRate", 20, "Net", 10.0, "Vat", 2.0), Map.of("VatRate", 20, "Net", 5.0), Map.of("Net", 1.0))));
        assertEquals(2, bound.children()
                             .size());
        assertEquals("20: 2.00", bound.children()
                                      .get(0)
                                      .text());
        // the row without a rate is its own group, last; its placeholder still falls back as any row does
        assertEquals("7: 0.00", bound.children()
                                     .get(1)
                                     .text());
    }

    @Test
    public void aggregatesTakeFormatsAndRenderEmptyOutsideAGroup() {
        Node root = parser.parse("""
                <document>
                    <text>[{{sum(Net)}}][{{count()}}]</text>
                    <for source="items" groupBy="VatRate">
                        <text>{{sum(Net):#,##0.0}}</text>
                    </for>
                </document>
                """);
        Node bound = binder.bind(root, Map.of("items", List.of(Map.of("VatRate", 20, "Net", 1234.5), Map.of("VatRate", 20, "Net", 0.25))));
        assertEquals("[][]", bound.children()
                                  .get(0)
                                  .text());
        assertEquals("1 234.8", bound.children()
                                     .get(1)
                                     .text());
    }

    /** The "No." column of an items table: every rendered row knows its 1-based position. */
    @Test
    public void indexNumbersTheRowsOfATableFromOne() {
        Node root = parser.parse("""
                <document>
                    <table source="items">
                        <column width="24" label="№">{{__index}}</column>
                        <column>{{name}}</column>
                    </table>
                </document>
                """);
        Node bound =
                binder.bind(root, Map.of("items", List.of(Map.of("name", "Widget"), Map.of("name", "Gadget"), Map.of("name", "Gizmo"))));

        assertEquals(List.of("1|Widget", "2|Gadget", "3|Gizmo"), rowTexts(bound.children()
                                                                               .get(0)));
    }

    @Test
    public void indexNumbersTheRowsTheFilterKeptContiguously() {
        Node root = parser.parse("""
                <document>
                    <table source="items" filter="kind" match="TAX | BASE">
                        <column>{{__index}}</column>
                        <column>{{name}}</column>
                    </table>
                    <for source="items" filter="billable"><text>{{__index}}. {{name}}</text></for>
                </document>
                """);
        Node bound = binder.bind(root,
                Map.of("items",
                        List.of(Map.of("name", "a", "kind", "BASE", "billable", false), Map.of("name", "b", "kind", "NOTE"),
                                Map.of("name", "c", "kind", "TAX", "billable", true), Map.of("name", "d", "kind", "NOTE", "billable", true),
                                Map.of("name", "e", "kind", "TAX"))));

        assertEquals(List.of("1|a", "2|c", "3|e"), rowTexts(bound.children()
                                                                 .get(0)));
        assertEquals("1. c", bound.children()
                                  .get(1)
                                  .text());
        assertEquals("2. d", bound.children()
                                  .get(2)
                                  .text());
    }

    @Test
    public void indexOfAGroupIsTheOrdinalOfTheGroup() {
        Node root = parser.parse(
                "<document><for source=\"items\" groupBy=\"VatRate\"><text>{{__index}}: {{VatRate}}% x{{count()}}</text></for></document>");
        Node bound = binder.bind(root,
                Map.of("items", List.of(Map.of("VatRate", 20), Map.of("VatRate", 9), Map.of("VatRate", 20), Map.of("VatRate", 20))));

        assertEquals(2, bound.children()
                             .size());
        assertEquals("1: 9% x1", bound.children()
                                      .get(0)
                                      .text());
        assertEquals("2: 20% x3", bound.children()
                                       .get(1)
                                       .text());
    }

    @Test
    public void indexReadsTheInnermostRowWhereLoopsNestAndRestartsPerLoop() {
        Node root = parser.parse("""
                <document>
                    <for source="orders">
                        <text>{{__index}} {{number}}</text>
                        <if source="lines"><text>has lines, still order {{__index}}</text></if>
                        <for source="lines"><text>{{__index}} {{sku}}</text></for>
                    </for>
                </document>
                """);
        Node bound = binder.bind(root,
                Map.of("orders", List.of(Map.of("number", "PO-1", "lines", List.of(Map.of("sku", "A"), Map.of("sku", "B"))),
                        Map.of("number", "PO-2", "lines", List.of(Map.of("sku", "C"))))));

        assertEquals(List.of("1 PO-1", "has lines, still order 1", "1 A", "2 B", "2 PO-2", "has lines, still order 2", "1 C"),
                bound.children()
                     .stream()
                     .map(Node::text)
                     .toList());
    }

    @Test
    public void indexTakesAFormatIsReservedOverARowFieldAndRendersEmptyOutsideARow() {
        Node root = parser.parse("""
                <document>
                    <text>[{{__index}}]</text>
                    <for source="items"><text>{{__index:000}} {{name}}</text></for>
                </document>
                """);
        Node bound = binder.bind(root, Map.of("__index", 99, "items", List.of(Map.of("name", "a", "__index", 7))));

        assertEquals("[]", bound.children()
                                .get(0)
                                .text());
        assertEquals("001 a", bound.children()
                                   .get(1)
                                   .text());
    }

    /** The text of every data row of a bound table, its cells joined by {@code |}. */
    private static List<String> rowTexts(Node table) {
        return table.children()
                    .stream()
                    .filter(child -> child instanceof org.eclipse.dirigible.parsers.document.RowNode)
                    .map(row -> row.children()
                                   .stream()
                                   .map(Node::text)
                                   .collect(java.util.stream.Collectors.joining("|")))
                    .toList();
    }
}
