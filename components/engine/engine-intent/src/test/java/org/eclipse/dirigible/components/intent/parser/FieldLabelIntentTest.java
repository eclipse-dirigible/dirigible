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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.junit.jupiter.api.Test;

/**
 * A field's display {@code label} and its country-scoped variants (#6424) - the two things an
 * authored label could not express: an acronym humanizing a name never produces, and a term that
 * follows the tenant's jurisdiction rather than the reader's language.
 */
class FieldLabelIntentTest {

    private static final String PAYROLL = """
            name: payroll
            entities:
              - name: Employee
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: nationalId
                    type: string
                    label: National ID
                    countryLabels:
                      BG: ЕГН
                      DE: Steuer-ID
                  - { name: name, type: string }
            """;

    /**
     * A picker's caption is the same problem one construct over (#7650): the relation is named for the
     * model, and the generated chooser, its list column and its details row all read as that identifier
     * until the relation itself is labelled.
     */
    private static final String INVOICING = """
            name: invoicing
            entities:
              - name: Company
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - name: issuer
                    kind: manyToOne
                    to: Company
                    label: Issuing company
                    countryLabels:
                      BG: Издател
                  - { name: lines, kind: oneToMany, to: InvoiceLine }
              - name: InvoiceLine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: invoice, kind: manyToOne, to: Invoice, composition: true }
            """;

    @Test
    void aFieldCarriesItsAuthoredLabel() {
        FieldIntent field = field(IntentParser.parse(PAYROLL), "nationalId");

        assertEquals("National ID", field.getLabel());
    }

    @Test
    void theCountryVariantsAreKeptInTheAuthoredOrder() {
        Map<String, String> variants = field(IntentParser.parse(PAYROLL), "nationalId").getCountryLabels();

        assertEquals(2, variants.size());
        assertEquals("ЕГН", variants.get("BG"));
        assertEquals("Steuer-ID", variants.get("DE"));
    }

    @Test
    void aFieldThatDeclaresNeitherIsUnaffected() {
        FieldIntent field = field(IntentParser.parse(PAYROLL), "name");

        assertNull(field.getLabel(), "absent means absent - the pipeline derives the humanized name as before");
        assertTrue(field.getCountryLabels()
                        .isEmpty(),
                "and absent variants must be an empty map, not a null the generators trip over");
    }

    @Test
    void aVariantMayBeAuthoredWithoutABaseLabel() {
        String yaml = PAYROLL.replace("                    label: National ID\n", "");

        assertEquals("ЕГН", field(IntentParser.parse(yaml), "nationalId").getCountryLabels()
                                                                         .get("BG"));
    }

    /**
     * A code that is not a country can never match a tenant, so the label would silently stay the base
     * one forever - the failure mode the whole feature exists to remove.
     */
    @Test
    void aKeyThatIsNotACountryCodeIsRejected() {
        assertIssue(PAYROLL.replace("      BG: ЕГН", "      bulgaria: ЕГН"), "which is not an ISO 3166-1 alpha-2 country code");
    }

    @Test
    void aLanguageCodeMistakenForACountryIsRejected() {
        assertIssue(PAYROLL.replace("      DE: Steuer-ID", "      EN: National insurance number"),
                "which is not an ISO 3166-1 alpha-2 country code");
    }

    @Test
    void aLowerCaseCountryCodeIsAccepted() {
        Map<String, String> variants =
                field(IntentParser.parse(PAYROLL.replace("      BG: ЕГН", "      bg: ЕГН")), "nationalId").getCountryLabels();

        assertEquals("ЕГН", variants.get("bg"), "the case the author wrote is theirs; the generator canonicalizes it");
    }

    @Test
    void aVariantWithNoLabelIsRejected() {
        assertIssue(PAYROLL.replace("      BG: ЕГН", "      BG: \"\""), "countryLabels [BG] has no label");
    }

    @Test
    void aBlankLabelIsRejected() {
        assertIssue(PAYROLL.replace("label: National ID", "label: \"  \""), "declares a blank `label`");
    }

    @Test
    void aToOneRelationCarriesItsAuthoredPickerCaption() {
        RelationIntent relation = relation(IntentParser.parse(INVOICING), "issuer");

        assertEquals("Issuing company", relation.getLabel());
        assertEquals("Издател", relation.getCountryLabels()
                                        .get("BG"));
    }

    @Test
    void aRelationThatDeclaresNeitherIsUnaffected() {
        RelationIntent relation = relation(IntentParser.parse(INVOICING), "lines");

        assertNull(relation.getLabel());
        assertTrue(relation.getCountryLabels()
                           .isEmpty(),
                "absent variants must be an empty map, not a null the generators trip over");
    }

    /** A collection renders no control of its own, so a caption authored there would go nowhere. */
    @Test
    void aLabelOnACollectionRelationIsRejected() {
        assertIssue(
                INVOICING.replace("  - { name: lines, kind: oneToMany, to: InvoiceLine }",
                        "  - { name: lines, kind: oneToMany, to: InvoiceLine, label: Line items }"),
                "a collection relation renders no control of its own");
    }

    @Test
    void aRelationVariantIsHeldToTheSameCountryRule() {
        assertIssue(INVOICING.replace("      BG: Издател", "      bulgaria: Издател"), "which is not an ISO 3166-1 alpha-2 country code");
    }

    private static RelationIntent relation(IntentModel model, String name) {
        return model.getEntities()
                    .stream()
                    .flatMap(entity -> entity.getRelations()
                                             .stream())
                    .filter(relation -> name.equals(relation.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no relation [" + name + "]"));
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException exception = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));

        assertTrue(exception.getMessage()
                            .contains(expected),
                () -> "expected an issue containing [" + expected + "] but got: " + exception.getMessage());
    }

    private static FieldIntent field(IntentModel model, String name) {
        return model.getEntities()
                    .stream()
                    .flatMap(entity -> entity.getFields()
                                             .stream())
                    .filter(field -> name.equals(field.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no field [" + name + "]"));
    }
}
