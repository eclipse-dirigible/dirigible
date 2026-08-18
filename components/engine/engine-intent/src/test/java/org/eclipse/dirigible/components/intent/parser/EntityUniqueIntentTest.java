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

import java.util.List;
import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.UniqueIntent;
import org.junit.jupiter.api.Test;

/**
 * Entity-level {@code unique} - the business key spanning more than one column (#6763). Its parse
 * rules exist because every one of them, unenforced, produces a constraint the database cannot
 * create or a rule that silently constrains nothing.
 */
class EntityUniqueIntentTest {

    private static final String PROVISIONING = """
            name: provisioning
            entities:
              - name: Tenant
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Application
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: TenantApplication
                unique:
                  - { fields: [tenant, application], message: "This application is already provisioned for the tenant" }
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: plan, type: string }
                relations:
                  - { name: tenant, kind: manyToOne, to: Tenant, required: true }
                  - { name: application, kind: manyToOne, to: Application, required: true }
            """;

    @Test
    void aKeyOverTwoToOneRelationsParses() {
        IntentModel model = IntentParser.parse(PROVISIONING);

        List<UniqueIntent> keys = entity(model, "TenantApplication").getUnique();
        assertEquals(1, keys.size());
        assertEquals(List.of("tenant", "application"), keys.get(0)
                                                           .getFields(),
                "the declared order is the constrained order");
        assertEquals("This application is already provisioned for the tenant", keys.get(0)
                                                                                   .getMessage());
    }

    @Test
    void aKeyMayMixFieldsAndRelations() {
        IntentModel model = IntentParser.parse(PROVISIONING.replace("[tenant, application]", "[tenant, plan]"));

        assertEquals(List.of("tenant", "plan"), entity(model, "TenantApplication").getUnique()
                                                                                  .get(0)
                                                                                  .getFields());
    }

    @Test
    void anEntityWithNoKeysIsUnaffected() {
        IntentModel model = IntentParser.parse(PROVISIONING);

        assertTrue(entity(model, "Tenant").getUnique()
                                          .isEmpty(),
                "unique is absent by default, and absent must mean an empty list, not a null the generators trip over");
    }

    @Test
    void aSingleFieldKeyIsRejectedNamingTheFieldAttributeItDuplicates() {
        assertIssue(PROVISIONING.replace("[tenant, application]", "[tenant]"), "declare unique: true on the field itself");
    }

    @Test
    void aNameThatIsNeitherFieldNorRelationIsRejected() {
        assertIssue(PROVISIONING.replace("[tenant, application]", "[tenant, tier]"),
                "names [tier], which is not a field or to-one relation");
    }

    @Test
    void aToManyRelationIsRejectedBecauseItHasNoColumnHere() {
        String yaml = PROVISIONING.replace("  - { name: application, kind: manyToOne, to: Application, required: true }",
                "  - { name: application, kind: oneToMany, to: Application }");

        assertIssue(yaml, "only a field or a to-one relation has a column on this entity to constrain");
    }

    @Test
    void aCrossModelRelationIsRejected() {
        String yaml = """
                name: provisioning
                uses:
                  - { name: catalog }
                entities:
                  - name: Tenant
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: TenantApplication
                    unique:
                      - { fields: [tenant, application] }
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: tenant, kind: manyToOne, to: Tenant, required: true }
                      - { name: application, kind: manyToOne, to: Application, model: catalog, required: true }
                """;

        assertIssue(yaml, "cross-model relation [application]");
    }

    @Test
    void aRepeatedNameWithinOneKeyIsRejected() {
        assertIssue(PROVISIONING.replace("[tenant, application]", "[tenant, tenant]"), "names [tenant] twice");
    }

    @Test
    void theSameKeyDeclaredTwiceIsRejected() {
        String yaml = PROVISIONING.replace(
                "  - { fields: [tenant, application], message: \"This application is already provisioned for the tenant\" }",
                "  - { fields: [tenant, application] }\n      - { fields: [tenant, application] }");

        assertIssue(yaml, "is declared twice");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException exception = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));

        assertTrue(exception.getMessage()
                            .contains(expected),
                () -> "expected an issue containing [" + expected + "] but got: " + exception.getMessage());
    }

    private static EntityIntent entity(IntentModel model, String name) {
        return model.getEntities()
                    .stream()
                    .filter(entity -> name.equals(entity.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no entity [" + name + "]"));
    }
}
