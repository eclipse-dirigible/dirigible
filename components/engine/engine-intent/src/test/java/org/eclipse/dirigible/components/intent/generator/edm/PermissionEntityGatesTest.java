/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.edm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.IntentSettings;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * An entity's read / write GATES come from the intent's {@code permissions[].can:} tokens whenever
 * a token names it (dirigible #6760). Before this, the gate was always the convention-derived
 * {@code <project>.<perspective>.<Entity>FullAccess} - a name no intent construct mentions - so
 * granting an authored role granted nothing at all, silently.
 */
class PermissionEntityGatesTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: Order
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: total, type: decimal }
                relations:
                  - { name: items, kind: oneToMany, to: OrderItem }
              - name: OrderItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: integer }
                relations:
                  - { name: Order, kind: manyToOne, to: Order, composition: true, required: true }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            permissions:
              - { role: SalesAdmin,  can: [Order:read, Order:create, Order:delete] }
              - { role: SalesViewer, can: [Order:read] }
            """;

    @Test
    void anAuthoredGrantBecomesTheEntitysGate() {
        Map<String, Object> order = entity(YAML, "Order");
        assertEquals("SalesAdmin,SalesViewer", order.get("roleRead"),
                "every grantee of the entity, read or write, must satisfy the read gate");
        assertEquals("SalesAdmin", order.get("roleWrite"));
        // The authored roles are already declared by <intent>.roles; re-declaring them from the
        // template's default-roles artefact would produce a second row per role name.
        assertEquals("false", order.get("generateDefaultRoles"));
    }

    @Test
    void anEntityNoTokenNamesKeepsTheConventionGates() {
        Map<String, Object> customer = entity(YAML, "Customer");
        assertEquals("sales.Customer.CustomerReadOnly", customer.get("roleRead"));
        assertEquals("sales.Customer.CustomerFullAccess", customer.get("roleWrite"));
        assertEquals("true", customer.get("generateDefaultRoles"), "the convention roles must still be declared and grantable");
    }

    @Test
    void aCompositionChildIsGatedByTheMasterItIsManagedUnder() {
        Map<String, Object> item = entity(YAML, "OrderItem");
        assertEquals("SalesAdmin,SalesViewer", item.get("roleRead"),
                "a document's items must be reachable to exactly the roles the document is");
        assertEquals("SalesAdmin", item.get("roleWrite"));
    }

    @Test
    void aReadOnlyAllowListLeavesNoOneAbleToWrite() {
        Map<String, Object> order = entity(YAML.replace("can: [Order:read, Order:create, Order:delete]", "can: [Order:read]"), "Order");
        assertEquals("SalesAdmin,SalesViewer", order.get("roleRead"));
        // No grant may write, so the write gate names a role that is never declared - the honest
        // encoding of an allow-list that only grants reads.
        assertEquals("sales.Order.OrderFullAccess", order.get("roleWrite"));
        assertEquals("false", order.get("generateDefaultRoles"));
    }

    @Test
    void aWildcardGrantsBothGates() {
        Map<String, Object> order = entity(YAML.replace("can: [Order:read, Order:create, Order:delete]", "can: [Order:*]"), "Order");
        assertEquals("SalesAdmin,SalesViewer", order.get("roleRead"));
        assertEquals("SalesAdmin", order.get("roleWrite"));
    }

    /**
     * {@code .settings} {@code access.extraRoles} appends the deployment's roles to every gate without
     * displacing the gate's own (dirigible #7652): a multitenant app's tenant members hold
     * {@code Owner} / {@code User}, and the convention roles must stay declared beside them.
     */
    @Test
    void theDeploymentsExtraRolesAreAppendedToAConventionGateWhichStaysDeclared() {
        Map<String, Object> customer = entity(YAML, "Customer", EXTRA_ROLES);
        assertEquals("sales.Customer.CustomerReadOnly,Owner,User,Auditor", customer.get("roleRead"));
        assertEquals("sales.Customer.CustomerFullAccess,Owner,User", customer.get("roleWrite"),
                "a read-only extra must not reach the write gate");
        assertEquals("true", customer.get("generateDefaultRoles"), "the extras must not suppress the convention roles");
    }

    @Test
    void theDeploymentsExtraRolesAreAppendedToAnAuthoredGate() {
        Map<String, Object> order = entity(YAML, "Order", EXTRA_ROLES);
        assertEquals("SalesAdmin,SalesViewer,Owner,User,Auditor", order.get("roleRead"));
        assertEquals("SalesAdmin,Owner,User", order.get("roleWrite"));
        assertEquals("false", order.get("generateDefaultRoles"));
        Map<String, Object> item = entity(YAML, "OrderItem", EXTRA_ROLES);
        assertEquals("SalesAdmin,Owner,User", item.get("roleWrite"), "a composition child inherits the master's gate, extras included");
    }

    @Test
    void anExtraRoleTheGateAlreadyNamesIsNotRepeated() {
        Map<String, Object> order = entity(YAML.replace("role: SalesAdmin,", "role: Owner,"), "Order", EXTRA_ROLES);
        assertEquals("Owner,User", order.get("roleWrite"));
        assertEquals("Owner,SalesViewer,User,Auditor", order.get("roleRead"));
    }

    /**
     * An {@code exclusive: true} grant keeps the deployment's extras off the gates of what it names
     * (issue #8112): any tenant member could rewrite a colleague's pay-relevant record otherwise.
     */
    @Test
    void anExclusiveGrantTakesNoExtraRolesAndNeitherDoItsCompositionChildren() {
        String yaml = YAML.replace("{ role: SalesAdmin,  can: [Order:read, Order:create, Order:delete] }",
                "{ role: SalesAdmin,  can: [Order:read, Order:create, Order:delete], exclusive: true }");
        Map<String, Object> order = entity(yaml, "Order", EXTRA_ROLES);
        assertEquals("SalesAdmin,SalesViewer", order.get("roleRead"));
        assertEquals("SalesAdmin", order.get("roleWrite"));
        Map<String, Object> item = entity(yaml, "OrderItem", EXTRA_ROLES);
        assertEquals("SalesAdmin,SalesViewer", item.get("roleRead"), "a composition child is gated as its master is");
        assertEquals("SalesAdmin", item.get("roleWrite"));
        Map<String, Object> customer = entity(yaml, "Customer", EXTRA_ROLES);
        assertEquals("sales.Customer.CustomerFullAccess,Owner,User", customer.get("roleWrite"), "a resource it does not name keeps them");
    }

    /**
     * An {@code additive: true} entry adds its role to the gates of what it names and leaves the
     * convention roles there, first and declared (issue #8852): a role for people beside the roles the
     * integration clients hold, with nothing re-declared by hand.
     */
    @Test
    void anAdditiveGrantJoinsTheConventionGateWhichStaysDeclared() {
        String yaml = YAML + "  - { role: Clerk, can: [Customer:read], additive: true }\n";
        Map<String, Object> customer = entity(yaml, "Customer");
        assertEquals("sales.Customer.CustomerReadOnly,sales.Customer.CustomerFullAccess,Clerk", customer.get("roleRead"),
                "the convention roles stay first - the template layer declares a gate's first role");
        assertEquals("sales.Customer.CustomerFullAccess", customer.get("roleWrite"), "a read token grants no write");
        assertEquals("true", customer.get("generateDefaultRoles"), "the convention roles must stay declared");

        Map<String, Object> withExtras = entity(yaml, "Customer", EXTRA_ROLES);
        assertEquals("sales.Customer.CustomerReadOnly,sales.Customer.CustomerFullAccess,Clerk,Owner,User,Auditor",
                withExtras.get("roleRead"), "the deployment's extras come after the added role");
        assertEquals("sales.Customer.CustomerFullAccess,Owner,User", withExtras.get("roleWrite"));

        Map<String, Object> order = entity(yaml, "Order");
        assertEquals("SalesAdmin,SalesViewer", order.get("roleRead"), "a resource the entry does not name is untouched");
    }

    @Test
    void anAdditiveWriteGrantJoinsTheWriteGateAndTheReadGate() {
        for (String action : List.of("write", "*")) {
            Map<String, Object> customer =
                    entity(YAML + "  - { role: Clerk, can: [\"Customer:" + action + "\"], additive: true }\n", "Customer", EXTRA_ROLES);
            assertEquals("sales.Customer.CustomerReadOnly,sales.Customer.CustomerFullAccess,Clerk,Owner,User,Auditor",
                    customer.get("roleRead"), action);
            assertEquals("sales.Customer.CustomerFullAccess,Clerk,Owner,User", customer.get("roleWrite"), action);
            assertEquals("true", customer.get("generateDefaultRoles"), action);
        }
    }

    /**
     * An additive entry never removes anything: beside a non-additive entry on the same resource, that
     * entry's roles replace the convention roles as they always did and the additive role joins them -
     * wherever the additive entry is declared.
     */
    @Test
    void anAdditiveGrantBesideAnAuthoredOneIsAppendedToTheAuthoredRoles() {
        String yaml = YAML.replace("permissions:\n", "permissions:\n  - { role: Clerk, can: [Order:read], additive: true }\n");
        Map<String, Object> order = entity(yaml, "Order");
        assertEquals("SalesAdmin,SalesViewer,Clerk", order.get("roleRead"));
        assertEquals("SalesAdmin", order.get("roleWrite"));
        assertEquals("false", order.get("generateDefaultRoles"), "the non-additive entries still replace the convention roles");
    }

    @Test
    void anAdditiveWriterOfAReadOnlyAllowListIsTheOneWhoMayWrite() {
        String yaml = YAML.replace("can: [Order:read, Order:create, Order:delete]", "can: [Order:read]")
                + "  - { role: Clerk, can: [Order:write], additive: true }\n";
        Map<String, Object> order = entity(yaml, "Order");
        assertEquals("SalesAdmin,SalesViewer,Clerk", order.get("roleRead"));
        assertEquals("Clerk", order.get("roleWrite"), "the never-declared placeholder is for a gate nobody may pass");
    }

    /**
     * An {@code exclusive} entry of another role decides the resource as before - convention roles
     * replaced, the deployment's extras kept off - and the additive role is added to that gate.
     */
    @Test
    void anAdditiveGrantOnAnExclusiveResourceIsAddedAndTheExtrasStayOff() {
        String yaml = YAML.replace("{ role: SalesAdmin,  can: [Order:read, Order:create, Order:delete] }",
                "{ role: SalesAdmin,  can: [Order:read, Order:create, Order:delete], exclusive: true }")
                + "  - { role: Clerk, can: [Order:read], additive: true }\n";
        Map<String, Object> order = entity(yaml, "Order", EXTRA_ROLES);
        assertEquals("SalesAdmin,SalesViewer,Clerk", order.get("roleRead"));
        assertEquals("SalesAdmin", order.get("roleWrite"));
        assertEquals("false", order.get("generateDefaultRoles"));
        Map<String, Object> item = entity(yaml, "OrderItem", EXTRA_ROLES);
        assertEquals("SalesAdmin,SalesViewer,Clerk", item.get("roleRead"), "a composition child is gated as its master is");
    }

    @Test
    void aCompositionChildTakesItsMastersAdditiveRolesBesideItsOwnConventionRoles() {
        String yaml = YAML.substring(0, YAML.indexOf("permissions:")) + """
                permissions:
                  - { role: Clerk, can: [Order:write], additive: true }
                """;
        Map<String, Object> item = entity(yaml, "OrderItem", EXTRA_ROLES);
        assertEquals("sales.Order.OrderItemReadOnly,sales.Order.OrderItemFullAccess,Clerk,Owner,User,Auditor", item.get("roleRead"));
        assertEquals("sales.Order.OrderItemFullAccess,Clerk,Owner,User", item.get("roleWrite"));
        assertEquals("true", item.get("generateDefaultRoles"));
    }

    /**
     * An additive entry naming the child itself adds to whatever the child had: it does not stop the
     * child inheriting its master's gate, authored or additive.
     */
    @Test
    void anAdditiveGrantOnACompositionChildDoesNotStopItInheritingItsMastersGate() {
        Map<String, Object> underAuthored = entity(YAML + "  - { role: Packer, can: [OrderItem:write], additive: true }\n", "OrderItem");
        assertEquals("SalesAdmin,SalesViewer,Packer", underAuthored.get("roleRead"));
        assertEquals("SalesAdmin,Packer", underAuthored.get("roleWrite"));
        assertEquals("false", underAuthored.get("generateDefaultRoles"));
        assertEquals("SalesAdmin",
                entity(YAML + "  - { role: Packer, can: [OrderItem:write], additive: true }\n", "Order").get("roleWrite"),
                "the child's entry adds nothing to the master");

        String additiveOnly = YAML.substring(0, YAML.indexOf("permissions:")) + """
                permissions:
                  - { role: Clerk,  can: [Order:read], additive: true }
                  - { role: Packer, can: [OrderItem:read], additive: true }
                """;
        assertEquals("sales.Order.OrderItemReadOnly,sales.Order.OrderItemFullAccess,Clerk,Packer",
                entity(additiveOnly, "OrderItem").get("roleRead"));
    }

    /**
     * The same intent with and without an additive entry that names another resource: nothing but that
     * resource's gates may differ.
     */
    @Test
    void anAdditiveEntryChangesOnlyTheGatesOfWhatItNames() {
        String yaml = YAML + "  - { role: Clerk, can: [Customer:read], additive: true }\n";
        for (String name : List.of("Order", "OrderItem")) {
            Map<String, Object> before = entity(YAML, name, EXTRA_ROLES);
            Map<String, Object> after = entity(yaml, name, EXTRA_ROLES);
            assertEquals(before.get("roleRead"), after.get("roleRead"), name);
            assertEquals(before.get("roleWrite"), after.get("roleWrite"), name);
            assertEquals(before.get("generateDefaultRoles"), after.get("generateDefaultRoles"), name);
        }
    }

    private static final IntentSettings EXTRA_ROLES =
            IntentSettings.parse("{\"access\": {\"extraRoles\": [\"Owner\", \" User \", \"\"], \"extraRolesReadOnly\": [\"Auditor\"]}}");

    private static Map<String, Object> entity(String yaml, String name) {
        return entity(yaml, name, new IntentSettings());
    }

    private static Map<String, Object> entity(String yaml, String name, IntentSettings settings) {
        IntentModel model = IntentParser.parse(yaml);
        Map<String, Object> json = EdmIntentGenerator.buildModelJsonForTest(model, "sales", settings);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) json.get("model")).get("entities");
        return entities.stream()
                       .filter(entity -> name.equals(entity.get("name")))
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no entity [" + name + "] was emitted"));
    }

}
