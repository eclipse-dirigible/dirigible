/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.data.store.java.store.JavaEntityStore;
import org.eclipse.dirigible.sdk.db.DeleteRestrictionException;
import org.eclipse.dirigible.sdk.db.TargetDeleteRule;
import org.eclipse.dirigible.sdk.extensions.Extensions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * dirigible #8569: a delete refused by the records referencing it names EVERY kind of referencing
 * record with its count - not only the first rule that counted one - and nothing is released or
 * deleted.
 */
class JavaRepositoryDeleteRefusalTest {

    private static final String TARGET = Product.class.getName();

    private JavaEntityStore store;

    private TargetDeleteRule costs;

    private TargetDeleteRule movements;

    private TargetDeleteRule unrelated;

    private MockedStatic<BeanProvider> beans;

    private MockedStatic<Extensions> extensions;

    @BeforeEach
    void setUp() {
        store = mock(JavaEntityStore.class);
        when(store.inUnitOfWork(any())).thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        costs = rule("Product Cost", "products:products-model.t.PRODUCT_COST", 1);
        unrelated = rule("Supplier", null, 0);
        movements = rule("Stock Movement", "inventory:inventory-model.t.STOCK_MOVEMENT", 2);
        beans = mockStatic(BeanProvider.class);
        beans.when(BeanProvider::isInitialzed)
             .thenReturn(true);
        beans.when(() -> BeanProvider.getBean(JavaEntityStore.class))
             .thenReturn(store);
        extensions = mockStatic(Extensions.class);
        extensions.when(() -> Extensions.find(TargetDeleteRule.class))
                  .thenReturn(List.of(costs, unrelated, movements));
    }

    @AfterEach
    void tearDown() {
        extensions.close();
        beans.close();
    }

    @Test
    void theRefusalListsEveryReferencingEntityAndNothingIsReleased() {
        ProductRepository repository = new ProductRepository(store, "products:products-model", "products:products-model.t.PRODUCT");

        DeleteRestrictionException refusal = assertThrows(DeleteRestrictionException.class, () -> repository.deleteById(2));

        assertEquals("This Product is referenced by 1 Product Cost record(s), 2 Stock Movement record(s) and cannot be deleted",
                refusal.getMessage());
        assertEquals("products:products-model.checks.deleteReferenced", refusal.getMessageKey());
        assertEquals(Map.of("entity", "Product", "references", "1 Product Cost record(s), 2 Stock Movement record(s)"),
                refusal.getMessageParams());
        for (TargetDeleteRule rule : List.of(costs, unrelated, movements)) {
            verify(rule).countRestricting(TARGET, 2);
            verify(rule, never()).release(TARGET, 2);
        }
        verify(store, never()).deleteById(Product.class, 2);
    }

    /** A hand-written repository names no catalog: the same refusal, in the default text, no key. */
    @Test
    void aRepositoryWithoutACatalogRefusesInTheDefaultText() {
        ProductRepository repository = new ProductRepository(store, null, null);

        DeleteRestrictionException refusal = assertThrows(DeleteRestrictionException.class, () -> repository.deleteById(2));

        assertEquals("This Product is referenced by 1 Product Cost record(s), 2 Stock Movement record(s) and cannot be deleted",
                refusal.getMessage());
        assertNull(refusal.getMessageKey());
    }

    /**
     * A reference no delete rule counts - the controller's foreign-key fallback - is refused through
     * the repository's own catalog.
     */
    @Test
    void aReferenceNoRuleCountsIsRefusedThroughTheRepositorysCatalog() {
        ProductRepository repository = new ProductRepository(store, "products:products-model", "products:products-model.t.PRODUCT");

        DeleteRestrictionException refusal = repository.referencedByOthers();

        assertEquals("This Product is referenced by other records and cannot be deleted", refusal.getMessage());
        assertEquals("products:products-model.checks.deleteReferencedByOthers", refusal.getMessageKey());
    }

    private static TargetDeleteRule rule(String label, String labelKey, int count) {
        TargetDeleteRule rule = mock(TargetDeleteRule.class);
        when(rule.countRestricting(TARGET, 2)).thenReturn(count);
        when(rule.referencingLabel()).thenReturn(label);
        when(rule.referencingLabelKey()).thenReturn(labelKey);
        return rule;
    }

    static class Product {
    }

    static class ProductRepository extends JavaRepository<Product> {

        private final JavaEntityStore store;

        private final String catalog;

        private final String labelKey;

        ProductRepository(JavaEntityStore store, String catalog, String labelKey) {
            super(Product.class);
            this.store = store;
            this.catalog = catalog;
            this.labelKey = labelKey;
        }

        @Override
        protected JavaEntityStore store() {
            return store;
        }

        @Override
        protected String messageCatalog() {
            return catalog;
        }

        @Override
        protected String labelCatalogKey() {
            return labelKey;
        }
    }
}
