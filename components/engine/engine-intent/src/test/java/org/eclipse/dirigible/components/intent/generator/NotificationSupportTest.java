/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.NotificationIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.junit.jupiter.api.Test;

class NotificationSupportTest {

    private static FieldIntent field(String name) {
        FieldIntent f = new FieldIntent();
        f.setName(name);
        f.setType("string");
        return f;
    }

    private static RelationIntent toOne(String name, String to) {
        RelationIntent r = new RelationIntent();
        r.setName(name);
        r.setKind("manyToOne");
        r.setTo(to);
        return r;
    }

    /** Order(id, total) --customer--> Customer(id, name, email). */
    private static Map<String, EntityIntent> libraryModel() {
        EntityIntent customer = new EntityIntent();
        customer.setName("Customer");
        customer.setFields(List.of(field("id"), field("name"), field("email")));
        EntityIntent order = new EntityIntent();
        order.setName("Order");
        order.setFields(List.of(field("id"), field("total")));
        order.setRelations(List.of(toOne("customer", "Customer")));
        Map<String, EntityIntent> byName = new LinkedHashMap<>();
        byName.put("Customer", customer);
        byName.put("Order", order);
        return byName;
    }

    private static NotificationIntent notification(String to, String subject) {
        NotificationIntent n = new NotificationIntent();
        n.setName("orderNote");
        n.setEvent(new LinkedHashMap<>(Map.of("onUpdate", "Order")));
        n.setTo(to);
        n.setSubject(subject);
        return n;
    }

    @Test
    void resolvesEventAndTopicSuffix() {
        NotificationIntent n = notification("ops@x.com", "s");
        assertEquals("onUpdate", NotificationSupport.eventKind(n));
        assertEquals("Order", NotificationSupport.eventEntity(n));
        assertEquals("", NotificationSupport.topicSuffix("onCreate"));
        assertEquals("-updated", NotificationSupport.topicSuffix("onUpdate"));
        assertEquals("-deleted", NotificationSupport.topicSuffix("onDelete"));
    }

    @Test
    void directFieldsAndLiterals() {
        Map<String, EntityIntent> byName = libraryModel();
        NotificationSupport.Plan plan =
                NotificationSupport.plan(notification("ops@x.com", "Order {id} total {total}"), byName.get("Order"), byName, Map.of());
        assertTrue(plan.loads()
                       .isEmpty(),
                "no relations referenced -> no loads");
        assertEquals("\"ops@x.com\"", plan.toExpression());
        assertEquals("\"Order \" + entity.Id + \" total \" + entity.Total", plan.subjectExpression());
    }

    @Test
    void oneHopRelationFieldLoadsTheRelatedEntity() {
        Map<String, EntityIntent> byName = libraryModel();
        NotificationSupport.Plan plan = NotificationSupport.plan(notification("customer.email", "Order for {customer.name}"),
                byName.get("Order"), byName, Map.of());

        assertEquals(1, plan.loads()
                            .size());
        NotificationSupport.RelationLoad load = plan.loads()
                                                    .get(0);
        assertEquals("customer", load.local());
        assertEquals("Customer", load.targetEntity());
        assertEquals("Customer", load.fkProperty());
        assertEquals("(customer == null ? null : customer.Email)", plan.toExpression());
        assertEquals("\"Order for \" + (customer == null ? null : customer.Name)", plan.subjectExpression());
    }

    @Test
    void unresolvableRecipientRelationYieldsNoPlan() {
        Map<String, EntityIntent> byName = libraryModel();
        // 'nope' is not a to-one relation of Order -> recipient cannot resolve -> skip the notification.
        assertNull(NotificationSupport.plan(notification("nope.email", "s"), byName.get("Order"), byName, Map.of()));
    }

    /** Order --partner (cross-model)--> Partner owned by another model, referenced as partner.email. */
    private static Map<String, EntityIntent> crossModelEventModel() {
        RelationIntent partner = toOne("partner", "Partner");
        partner.setModel("acme-partners"); // cross-model: Partner is NOT a local entity
        EntityIntent order = new EntityIntent();
        order.setName("Order");
        order.setFields(List.of(field("id"), field("total")));
        order.setRelations(List.of(partner));
        Map<String, EntityIntent> byName = new LinkedHashMap<>();
        byName.put("Order", order); // Partner is intentionally absent from the local byName map
        return byName;
    }

    @Test
    void crossModelRecipientResolvesThroughTheLookupAndLoadsFromTheOwner() {
        Map<String, EntityIntent> byName = crossModelEventModel();
        NotificationSupport.CrossModelLookup lookup = relation -> new NotificationSupport.CrossModelTarget("Partner", "acme-partners",
                "acme-partners", java.util.Set.of("Id", "Name", "Email"));
        NotificationSupport.Plan plan = NotificationSupport.plan(notification("partner.email", "Order for {partner.name}"),
                byName.get("Order"), byName, Map.of(), lookup);

        assertEquals(1, plan.loads()
                            .size());
        NotificationSupport.RelationLoad load = plan.loads()
                                                    .get(0);
        assertTrue(load.crossModel(), "a cross-model relation load must be flagged so the owner package is imported");
        assertEquals("Partner", load.targetEntity());
        assertEquals("Partner", load.targetPerspective());
        assertEquals("acme-partners", load.targetModel());
        assertEquals("acme-partners", load.targetProject());
        assertEquals("Partner", load.fkProperty());
        assertEquals("(partner == null ? null : partner.Email)", plan.toExpression());
    }

    @Test
    void crossModelRecipientFieldValidatedAgainstOwnerProperties() {
        Map<String, EntityIntent> byName = crossModelEventModel();
        // The owner model has no 'ceo' property -> the field is rejected -> no plan (recipient
        // unresolvable).
        NotificationSupport.CrossModelLookup lookup = relation -> new NotificationSupport.CrossModelTarget("Partner", "acme-partners",
                "acme-partners", java.util.Set.of("Id", "Name", "Email"));
        assertNull(NotificationSupport.plan(notification("partner.ceo", "s"), byName.get("Order"), byName, Map.of(), lookup));
    }

    @Test
    void crossModelRecipientWithoutLookupYieldsNoPlan() {
        Map<String, EntityIntent> byName = crossModelEventModel();
        // No lookup (e.g. a unit path with no repository) -> a cross-model recipient cannot resolve.
        assertNull(NotificationSupport.plan(notification("partner.email", "s"), byName.get("Order"), byName, Map.of()));
    }

    @Test
    void guardTranslatesSingleComparisonElseFiresAlways() {
        assertEquals("true", NotificationSupport.guard(null));
        assertEquals("true", NotificationSupport.guard("  "));
        assertEquals("java.util.Objects.equals(entity.Status, \"APPROVED\")", NotificationSupport.guard("status == 'APPROVED'"));
        assertEquals("!java.util.Objects.equals(entity.Status, \"CLOSED\")", NotificationSupport.guard("status != 'CLOSED'"));
        assertEquals("true", NotificationSupport.guard("amount > 10 and status == 'X'"));
    }
}
