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
import org.junit.jupiter.api.Test;

/**
 * Entity {@code phases:} and the {@code onPhase} event axis (#6929) - the enrichment channel.
 *
 * <p>
 * An enrichment a listener computes on create is written back event-silently, so a consumer bound
 * to {@code onCreate} races it and may read the un-enriched row with every step green. Every rule
 * here exists because, unenforced, it reproduces exactly that silence: an undeclared phase binds a
 * topic nothing publishes to, a {@code phase:} without {@code onPhase:} is dropped, and a phase
 * named after a platform channel re-fires that channel's consumers instead.
 */
class EntityPhaseIntentTest {

    private static final String INVENTORY = """
            name: inventory
            entities:
              - name: StockMovement
                phases: [costed]
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal }
                  - { name: costValue, type: decimal }
            """;

    @Test
    void anEntityDeclaresTheEnrichmentMomentsItAnnounces() {
        IntentModel model = IntentParser.parse(INVENTORY);

        assertEquals(List.of("costed"), entity(model, "StockMovement").getPhases());
    }

    @Test
    void anEntityDeclaringNoPhaseAnswersAnEmptyList() {
        IntentModel model = IntentParser.parse(INVENTORY.replace("    phases: [costed]\n", ""));

        assertTrue(entity(model, "StockMovement").getPhases()
                                                 .isEmpty(),
                "absent must mean an empty list, not a null the generators trip over");
    }

    @Test
    void aPhaseThatIsNotAnIdentifierIsRejectedBecauseItBecomesAMethodNameAndATopic() {
        assertIssue(INVENTORY.replace("[costed]", "[\"cost value\"]"), "must be a lower-camel identifier");
    }

    @Test
    void aPhaseNamedAfterAPlatformChannelIsRejected() {
        assertIssue(INVENTORY.replace("[costed]", "[updated]"), "is a platform channel");
    }

    @Test
    void aPhaseDeclaredTwiceIsRejected() {
        assertIssue(INVENTORY.replace("[costed]", "[costed, costed]"), "more than once");
    }

    @Test
    void aNotificationBindsADeclaredPhase() {
        IntentModel model = IntentParser.parse(INVENTORY + """
                notifications:
                  - name: costedMovement
                    event: { onPhase: StockMovement, phase: costed }
                    to: "ops@example.com"
                    subject: "Movement {id} costed"
                    body: "The movement has been costed."
                """);

        assertEquals("StockMovement", model.getNotifications()
                                           .get(0)
                                           .getEvent()
                                           .get("onPhase"));
    }

    @Test
    void aConsumerBindingAnUndeclaredPhaseIsRejected() {
        assertIssue(INVENTORY + """
                notifications:
                  - name: costedMovement
                    event: { onPhase: StockMovement, phase: priced }
                    to: "ops@example.com"
                    subject: "Movement {id}"
                    body: "."
                """, "binds phase [priced] which entity [StockMovement] does not declare");
    }

    @Test
    void anOnPhaseBindingWithoutAPhaseNameIsRejected() {
        assertIssue(INVENTORY + """
                notifications:
                  - name: costedMovement
                    event: { onPhase: StockMovement }
                    to: "ops@example.com"
                    subject: "Movement {id}"
                    body: "."
                """, "onPhase requires `phase: <name>`");
    }

    @Test
    void aPhaseKeyOnAnotherAxisIsRejectedRatherThanSilentlyDropped() {
        assertIssue(INVENTORY + """
                notifications:
                  - name: costedMovement
                    event: { onCreate: StockMovement, phase: costed }
                    to: "ops@example.com"
                    subject: "Movement {id}"
                    body: "."
                """, "without `onPhase:`");
    }

    @Test
    void twoAxesInOneBindingAreRejected() {
        assertIssue(INVENTORY + """
                notifications:
                  - name: costedMovement
                    event: { onCreate: StockMovement, onPhase: StockMovement, phase: costed }
                    to: "ops@example.com"
                    subject: "Movement {id}"
                    body: "."
                """, "exactly one of onCreate/onUpdate/onDelete/onTransition/onPhase/onStepReached/onStepCompleted");
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
                    .filter(candidate -> name.equals(candidate.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no entity [" + name + "]"));
    }
}
