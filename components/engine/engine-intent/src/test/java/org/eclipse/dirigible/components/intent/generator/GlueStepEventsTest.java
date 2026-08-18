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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * Verifies the process-step half of the glue event axis: the deduplicated {@code stepEvents}
 * emitter collection, and that a notification / integration bound to a step is generated against
 * the process's trigger entity and the step topic its emitter publishes to - i.e. the existing
 * action vocabulary is reused literally, not re-implemented per event kind.
 */
class GlueStepEventsTest {

    private static final String YAML = """
            name: library
            entities:
              - name: Member
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string }
                  - { name: email, type: string }
              - name: Loan
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: status, type: string }
                relations:
                  - { name: member, kind: manyToOne, to: Member }
            processes:
              - name: LoanApproval
                trigger: { onCreate: Loan }
                steps:
                  - { name: librarianReview, kind: userTask,    args: { assignee: librarian, next: activate } }
                  - { name: activate,        kind: serviceTask, args: { setField: status, value: ACTIVE, next: done } }
                  - { name: done,            kind: end }
            notifications:
              - name: reviewPending
                event: { onStepReached: { process: LoanApproval, step: librarianReview } }
                to: member.email
                subject: "Loan {id} is waiting"
                body: "A librarian must approve it."
              - name: alsoPending
                event: { onStepReached: { process: LoanApproval, step: librarianReview } }
                to: ops@example.com
                subject: "Loan {id} is waiting"
                body: "For the record."
            integrations:
              - name: pushActivation
                event: { onStepCompleted: { process: LoanApproval, step: activate } }
                method: POST
                url: "@config:PARTNER_URL"
            permissions:
              - { role: Librarian, description: Librarian, can: [Loan:read] }
            """;

    @Test
    void oneEmitterPerObservedMoment() {
        List<Map<String, Object>> stepEvents = GlueIntentGenerator.buildStepEventsForTest(IntentParser.parse(YAML));

        // Two notifications observe the same moment - the record is published once.
        assertEquals(2, stepEvents.size());
        Map<String, Object> reached = stepEvents.get(0);
        assertEquals("LoanApprovalLibrarianReviewReached", reached.get("className"));
        assertEquals("Loan", reached.get("entity"), "a step event is about the process's trigger entity");
        assertEquals("Id", reached.get("keyProperty"));
        assertEquals("intValue", reached.get("keyAccessor"));
        assertEquals("-step-LoanApproval-librarianReview-reached", reached.get("topicSuffix"));

        Map<String, Object> completed = stepEvents.get(1);
        assertEquals("LoanApprovalActivateCompleted", completed.get("className"));
        assertEquals("-step-LoanApproval-activate-completed", completed.get("topicSuffix"));
    }

    @Test
    void theConsumersBindToTheStepTopicOfTheTriggerEntity() {
        IntentModel model = IntentParser.parse(YAML);

        Map<String, Object> notification = GlueIntentGenerator.buildNotificationsForTest(model)
                                                              .get(0);
        assertEquals("Loan", notification.get("entity"));
        assertEquals("-step-LoanApproval-librarianReview-reached", notification.get("topicSuffix"));
        // The recipient path resolves against the trigger entity exactly as for a lifecycle event.
        assertTrue(notification.get("toExpression")
                               .toString()
                               .contains("Email"),
                "the one-hop relation.field recipient should resolve off the loaded member");

        Map<String, Object> integration = GlueIntentGenerator.buildIntegrationsForTest(model)
                                                             .get(0);
        assertEquals("Loan", integration.get("entity"));
        assertEquals("-step-LoanApproval-activate-completed", integration.get("topicSuffix"));
    }

    @Test
    void aLifecycleBindingIsUnchanged() {
        String yaml =
                YAML.replace("event: { onStepReached: { process: LoanApproval, step: librarianReview } }", "event: { onUpdate: Loan }");
        IntentModel model = IntentParser.parse(yaml);

        assertEquals("-updated", GlueIntentGenerator.buildNotificationsForTest(model)
                                                    .get(0)
                                                    .get("topicSuffix"));
        assertEquals(1, GlueIntentGenerator.buildStepEventsForTest(model)
                                           .size(),
                "only the still-step-bound integration needs an emitter");
    }
}
