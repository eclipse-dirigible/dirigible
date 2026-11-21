/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * dirigible #8569: a delete refusal names every kind of referencing record with its count, in the
 * language of the request - the sentence from the deleted entity's catalog, the entities by their
 * labels from their own catalogs - and falls back to the default-language text piece by piece.
 */
class DeleteRefusalsTest {

    private static final String CATALOG = "products:products-model";
    private static final String PRODUCT_LABEL_KEY = CATALOG + ".t.PRODUCT";
    private static final String PRODUCT_PRICE_LABEL_KEY = CATALOG + ".t.PRODUCT_PRICE";
    private static final List<DeleteRefusals.Reference> REFERENCES =
            List.of(new DeleteRefusals.Reference("Product Cost", "products:products-model.t.PRODUCT_COST", 1),
                    new DeleteRefusals.Reference("Stock Movement", "inventory:inventory-model.t.STOCK_MOVEMENT", 2));

    private static final Map<String, String> BG = Map.of( //
            CATALOG + ".checks.deleteReferenced", "{entity} не може да бъде изтрит: използва се в {references}.", //
            CATALOG + ".checks.deleteReference", "{count} {label}", //
            CATALOG + ".checks.deleteReferencedByOthers", "{entity} се използва от други записи и не може да бъде изтрит", //
            CATALOG + ".checks.deleteHasChildren", "{entity} все още има записи {children} - първо изтрийте тях", //
            PRODUCT_LABEL_KEY, "Продукт", //
            PRODUCT_PRICE_LABEL_KEY, "Цени на продукта", //
            "inventory:inventory-model.t.STOCK_MOVEMENT", "Складови движения");

    private static final CheckMessages.CatalogReader CATALOGS = (language, key) -> "bg".equals(language) ? BG.get(key) : null;

    @Test
    void everyReferencingEntityIsListedWithItsCount() {
        DeleteRestrictionException refusal = DeleteRefusals.referenced(null, CATALOGS, CATALOG, "Product", PRODUCT_LABEL_KEY, REFERENCES);

        assertEquals("This Product is referenced by 1 Product Cost record, 2 Stock Movement records and cannot be deleted",
                refusal.getMessage());
        assertEquals(CATALOG + ".checks.deleteReferenced", refusal.getMessageKey());
        assertEquals(Map.of("entity", "Product", "references", "1 Product Cost record, 2 Stock Movement records"),
                refusal.getMessageParams());
    }

    /** A label the language's catalog does not translate keeps its default-language text. */
    @Test
    void theRefusalIsTranslatedWithTheEntitiesTranslatedLabels() {
        DeleteRestrictionException refusal = DeleteRefusals.referenced("bg", CATALOGS, CATALOG, "Product", PRODUCT_LABEL_KEY, REFERENCES);

        assertEquals("Продукт не може да бъде изтрит: използва се в 1 Product Cost, 2 Складови движения.", refusal.getMessage());
        assertEquals(Map.of("entity", "Продукт", "references", "1 Product Cost, 2 Складови движения"), refusal.getMessageParams());
    }

    /**
     * A repository generated without a catalog - or a hand-written one - refuses in the default text.
     */
    @Test
    void withoutACatalogTheRefusalCarriesNoKey() {
        DeleteRestrictionException refusal = DeleteRefusals.referenced("bg", CATALOGS, null, "Product", null,
                List.of(new DeleteRefusals.Reference("Product Cost", null, 1)));

        assertEquals("This Product is referenced by 1 Product Cost record and cannot be deleted", refusal.getMessage());
        assertNull(refusal.getMessageKey());
    }

    @Test
    void aRecordReferencedByRecordsNoRuleCountsIsRefusedInTheReadersLanguage() {
        assertEquals("This Product is referenced by other records and cannot be deleted",
                DeleteRefusals.referencedByOthers(null, CATALOGS, CATALOG, "Product", PRODUCT_LABEL_KEY)
                              .getMessage());
        DeleteRestrictionException refusal = DeleteRefusals.referencedByOthers("bg", CATALOGS, CATALOG, "Product", PRODUCT_LABEL_KEY);
        assertEquals("Продукт се използва от други записи и не може да бъде изтрит", refusal.getMessage());
        assertEquals(CATALOG + ".checks.deleteReferencedByOthers", refusal.getMessageKey());
    }

    /** dirigible #7826: a child under a referenced leaf is refused, naming every referencing kind. */
    @Test
    void aChildUnderAReferencedLeafIsRefusedNamingTheReferences() {
        ValidationException refusal = DeleteRefusals.leafReferenced(null, CATALOGS, CATALOG, "Product", PRODUCT_LABEL_KEY, REFERENCES);

        assertEquals(
                "This Product is referenced as a leaf by 1 Product Cost record(s), 2 Stock Movement record(s) and cannot be given a child",
                refusal.getMessage());
        assertEquals(CATALOG + ".checks.leafReferenced", refusal.getMessageKey());
        assertEquals(Map.of("entity", "Product", "references", "1 Product Cost record(s), 2 Stock Movement record(s)"),
                refusal.getMessageParams());
    }

    /**
     * dirigible #8635: a master that still has refusing children is refused in the reader's language.
     */
    @Test
    void aMasterWithChildrenIsRefusedInTheReadersLanguage() {
        assertEquals("This Product still has Product Price records - delete those first",
                DeleteRefusals.hasChildren(null, CATALOGS, CATALOG, "Product", PRODUCT_LABEL_KEY, "Product Price", PRODUCT_PRICE_LABEL_KEY)
                              .getMessage());
        ValidationException refusal =
                DeleteRefusals.hasChildren("bg", CATALOGS, CATALOG, "Product", PRODUCT_LABEL_KEY, "Product Price", PRODUCT_PRICE_LABEL_KEY);
        assertEquals("Продукт все още има записи Цени на продукта - първо изтрийте тях", refusal.getMessage());
        assertEquals(CATALOG + ".checks.deleteHasChildren", refusal.getMessageKey());
        assertEquals(Map.of("entity", "Продукт", "children", "Цени на продукта"), refusal.getMessageParams());
    }

    /**
     * dirigible #8777: a catalog's plural forms are picked by the count - "1 запис", "5 записа" - and
     * the label opting into lower case reads so inside the sentence; a catalog written before keeps its
     * plain entry.
     */
    @Test
    void eachReferenceTakesThePluralFormOfItsCount() {
        Map<String, String> bg = Map.of( //
                CATALOG + ".checks.deleteReferenced", "{entity} не може да бъде изтрит: използва се в {references}.", //
                CATALOG + ".checks.deleteReference_one", "{count} запис в {label, lowercase}", //
                CATALOG + ".checks.deleteReference_other", "{count} записа в {label, lowercase}", //
                "inventory:inventory-model.t.STOCK_MOVEMENT", "Складови движения");
        CheckMessages.CatalogReader catalogs = (language, key) -> "bg".equals(language) ? bg.get(key) : null;

        DeleteRestrictionException refusal = DeleteRefusals.referenced("bg", catalogs, CATALOG, "Продукт", null,
                List.of(new DeleteRefusals.Reference("Stock Movement", "inventory:inventory-model.t.STOCK_MOVEMENT", 1),
                        new DeleteRefusals.Reference("Stock Movement", "inventory:inventory-model.t.STOCK_MOVEMENT", 5)));

        assertEquals("Продукт не може да бъде изтрит: използва се в 1 запис в складови движения, 5 записа в складови движения.",
                refusal.getMessage());
    }

    /** dirigible #8777: a master refusing its delete counts its children, in their plural form. */
    @Test
    void aMasterWithChildrenNamesHowManyInTheirPluralForm() {
        assertEquals("This Product still has 1 Product Price record - delete it first",
                DeleteRefusals.hasChildren(null, CATALOGS, CATALOG, "Product", PRODUCT_LABEL_KEY, "Product Price", PRODUCT_PRICE_LABEL_KEY,
                        1)
                              .getMessage());
        assertEquals("This Product still has 3 Product Price records - delete those first",
                DeleteRefusals.hasChildren(null, CATALOGS, CATALOG, "Product", PRODUCT_LABEL_KEY, "Product Price", PRODUCT_PRICE_LABEL_KEY,
                        3)
                              .getMessage());
        // A catalog written before carries the plain, countless entry - it still answers.
        ValidationException refusal = DeleteRefusals.hasChildren("bg", CATALOGS, CATALOG, "Product", PRODUCT_LABEL_KEY, "Product Price",
                PRODUCT_PRICE_LABEL_KEY, 3);
        assertEquals("Продукт все още има записи Цени на продукта - първо изтрийте тях", refusal.getMessage());
        assertEquals(Map.of("entity", "Продукт", "children", "Цени на продукта", "count", 3L), refusal.getMessageParams());
    }
}
