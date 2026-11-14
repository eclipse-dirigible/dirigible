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
    private static final List<DeleteRefusals.Reference> REFERENCES =
            List.of(new DeleteRefusals.Reference("Product Cost", "products:products-model.t.PRODUCT_COST", 1),
                    new DeleteRefusals.Reference("Stock Movement", "inventory:inventory-model.t.STOCK_MOVEMENT", 2));

    private static final Map<String, String> BG = Map.of( //
            CATALOG + ".checks.deleteReferenced", "{entity} не може да бъде изтрит: използва се в {references}.", //
            CATALOG + ".checks.deleteReference", "{count} {label}", //
            CATALOG + ".checks.deleteReferencedByOthers", "{entity} се използва от други записи и не може да бъде изтрит", //
            PRODUCT_LABEL_KEY, "Продукт", //
            "inventory:inventory-model.t.STOCK_MOVEMENT", "Складови движения");

    private static final CheckMessages.CatalogReader CATALOGS = (language, key) -> "bg".equals(language) ? BG.get(key) : null;

    @Test
    void everyReferencingEntityIsListedWithItsCount() {
        DeleteRestrictionException refusal = DeleteRefusals.referenced(null, CATALOGS, CATALOG, "Product", PRODUCT_LABEL_KEY, REFERENCES);

        assertEquals("This Product is referenced by 1 Product Cost record(s), 2 Stock Movement record(s) and cannot be deleted",
                refusal.getMessage());
        assertEquals(CATALOG + ".checks.deleteReferenced", refusal.getMessageKey());
        assertEquals(Map.of("entity", "Product", "references", "1 Product Cost record(s), 2 Stock Movement record(s)"),
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

        assertEquals("This Product is referenced by 1 Product Cost record(s) and cannot be deleted", refusal.getMessage());
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
}
