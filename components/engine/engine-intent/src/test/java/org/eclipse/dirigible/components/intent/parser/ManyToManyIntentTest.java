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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.junit.jupiter.api.Test;

/**
 * Coverage for {@code kind: manyToMany} - materialised into the intermediate (link) entity, never
 * accepted and dropped (#6718).
 */
class ManyToManyIntentTest {

    private static final String ORDERS = """
            name: orders
            entities:
              - name: Order
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                relations:
                  - { name: products, kind: manyToMany, to: Product }
              - name: Product
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            """;

    @Test
    void materializesTheLinkEntityRightAfterItsOwner() {
        IntentModel model = IntentParser.parse(ORDERS);

        assertEquals(List.of("Order", "OrderProduct", "Product"), names(model), "the link entity is inserted right after its owner");

        EntityIntent link = entity(model, "OrderProduct");
        FieldIntent id = link.getFields()
                             .get(0);
        assertEquals("id", id.getName());
        assertTrue(id.isPrimaryKey() && id.isGenerated(), "the link carries a generated integer key");

        RelationIntent toOwner = link.getRelations()
                                     .get(0);
        assertEquals("Order", toOwner.getTo());
        assertEquals("manyToOne", toOwner.getKind());
        assertTrue(toOwner.isComposition(), "the link is a detail of the declaring entity");
        assertTrue(toOwner.isRequired());

        RelationIntent toTarget = link.getRelations()
                                      .get(1);
        assertEquals("Product", toTarget.getTo());
        assertEquals("manyToOne", toTarget.getKind());
        assertFalse(toTarget.isComposition(), "the target end is an association, not a second owner");
        assertTrue(toTarget.isRequired(), "a link row that points at only one end is not a link");
    }

    @Test
    void theAuthoredRelationBecomesNavigationToTheLink() {
        IntentModel model = IntentParser.parse(ORDERS);

        RelationIntent products = entity(model, "Order").getRelations()
                                                        .get(0);
        assertEquals("products", products.getName(), "the authored name is kept");
        assertEquals("oneToMany", products.getKind(), "no manyToMany survives into the generators");
        assertEquals("OrderProduct", products.getTo());
    }

    @Test
    void throughNamesTheLinkEntity() {
        String yaml = ORDERS.replace("kind: manyToMany, to: Product", "kind: manyToMany, to: Product, through: OrderLine");
        IntentModel model = IntentParser.parse(yaml);

        assertEquals(List.of("Order", "OrderLine", "Product"), names(model));
        assertEquals("OrderLine", entity(model, "Order").getRelations()
                                                        .get(0)
                                                        .getTo());
    }

    @Test
    void throughIsRejectedOnAnyOtherKind() {
        String yaml = """
                name: orders
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer, through: OrderCustomer }
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains("only a manyToMany materialises an intermediate entity"),
                ex.getMessage());
    }

    @Test
    void thePickerAttributesTravelToTheLinksTargetRelation() {
        String yaml = """
                name: orders
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: products, kind: manyToMany, to: Product, major: false, size: 4, where: { Kind: 1 }, show: [code] }
                  - name: Product
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: code, type: string }
                      - { name: Kind, type: integer }
                """;
        IntentModel model = IntentParser.parse(yaml);

        RelationIntent toTarget = entity(model, "OrderProduct").getRelations()
                                                               .get(1);
        assertFalse(toTarget.isMajor());
        assertEquals(4, toTarget.getSize());
        assertEquals(1L, toTarget.getWhere()
                                 .get("Kind"));
        assertEquals(List.of("code"), toTarget.getShow());

        // Nothing is left behind on the navigation relation, which ignores them.
        RelationIntent products = entity(model, "Order").getRelations()
                                                        .get(0);
        assertEquals(null, products.getWhere());
        assertEquals(null, products.getShow());
    }

    @Test
    void aCrossModelTargetKeepsItsModelAliasOnTheLink() {
        String yaml = """
                name: orders
                uses:
                  - { model: products }
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: products, kind: manyToMany, to: Product, model: products }
                """;
        IntentModel model = IntentParser.parse(yaml);

        RelationIntent toTarget = entity(model, "OrderProduct").getRelations()
                                                               .get(1);
        assertEquals("products", toTarget.getModel(), "the link owns the cross-model association");
        assertTrue(toTarget.isCrossModel());
        assertEquals(null, entity(model, "Order").getRelations()
                                                 .get(0)
                                                 .getModel(),
                "the navigation relation points at the local link entity");
    }

    @Test
    void anUndeclaredCrossModelAliasIsReportedOnTheAuthoredRelation() {
        String yaml = """
                name: orders
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: products, kind: manyToMany, to: Product, model: products }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains("entity [Order] relation [products] references undeclared model [products]"),
                ex.getMessage());
    }

    @Test
    void aSelfReferencingManyToManyKeepsItsTwoEndsApart() {
        String yaml = """
                name: catalog
                entities:
                  - name: Product
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: relatedProducts, kind: manyToMany, to: Product }
                """;
        IntentModel model = IntentParser.parse(yaml);

        EntityIntent link = entity(model, "ProductRelatedProducts");
        assertEquals("Product", link.getRelations()
                                    .get(0)
                                    .getName());
        assertEquals("RelatedProducts", link.getRelations()
                                            .get(1)
                                            .getName(),
                "both ends target the same entity, so the second FK is named after the authored relation");
    }

    @Test
    void bothSidesDeclaringTheSamePairIsRejectedOnce() {
        String yaml = """
                name: school
                entities:
                  - name: Student
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: courses, kind: manyToMany, to: Course }
                  - name: Course
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: students, kind: manyToMany, to: Student }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        String message = ex.getMessage();
        assertTrue(message.contains("both declare a manyToMany to each other"), message);
        assertEquals(message.indexOf("both declare a manyToMany"), message.lastIndexOf("both declare a manyToMany"),
                "the pair is reported once, not once per side");
    }

    @Test
    void aNameClashWithADeclaredEntityIsRejected() {
        String yaml = """
                name: orders
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: products, kind: manyToMany, to: Product }
                  - name: Product
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: OrderProduct
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: quantity, type: decimal }
                    relations:
                      - { name: Order, kind: manyToOne, to: Order, composition: true, required: true }
                      - { name: Product, kind: manyToOne, to: Product, required: true }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains("through: <Name>"),
                "the clash message must offer both ways out: " + ex.getMessage());
    }

    @Test
    void toOneOnlyAttributesAreRejectedRatherThanDropped() {
        String yaml = """
                name: orders
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: products, kind: manyToMany, to: Product, composition: true, init: "1" }
                  - name: Product
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains("[composition, init]"),
                "every unsupported attribute is listed at once: " + ex.getMessage());
    }

    @Test
    void anUnknownTargetIsReportedOnTheAuthoredRelation() {
        String yaml = """
                name: orders
                entities:
                  - name: Order
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: products, kind: manyToMany, to: Product }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getMessage()
                     .contains("entity [Order] relation [products] points to unknown entity [Product]"),
                ex.getMessage());
    }

    private static List<String> names(IntentModel model) {
        return model.getEntities()
                    .stream()
                    .map(EntityIntent::getName)
                    .toList();
    }

    private static EntityIntent entity(IntentModel model, String name) {
        EntityIntent found = model.getEntities()
                                  .stream()
                                  .filter(entity -> name.equals(entity.getName()))
                                  .findFirst()
                                  .orElse(null);
        assertNotNull(found, "entity [" + name + "] must be present, model has " + names(model));
        return found;
    }
}
