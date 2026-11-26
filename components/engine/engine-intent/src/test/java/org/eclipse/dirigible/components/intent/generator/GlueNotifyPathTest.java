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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * A notify path walks a chain of to-one relations (dirigible #8030): what the four notify call
 * sites carry for {@code {subscription.customer.name}} - one load per distinct relation prefix, in
 * first-use order, each further hop reading its foreign key off the previous prefix's local - and
 * that a one-hop path carries exactly what it always did.
 */
class GlueNotifyPathTest {

    private static final String YAML = """
            name: renewals
            entities:
              - name: Country
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Customer
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string }
                  - { name: email, type: string }
                relations:
                  - { name: country, kind: manyToOne, to: Country }
              - name: Plan
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: name,      type: string }
                  - { name: portalUrl, type: string }
              - name: Subscription
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer, required: true }
                  - { name: plan,     kind: manyToOne, to: Plan,     required: true }
              - name: RenewalStatus
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Renewal
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: requestedBy, type: string }
                relations:
                  - { name: subscription, kind: manyToOne, to: Subscription, required: true }
                  - { name: Status, kind: manyToOne, to: RenewalStatus, function: EntityStatus, init: 1 }
              - name: RenewalContact
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string }
                relations:
                  - { name: renewal,      kind: manyToOne, to: Renewal }
                  - { name: subscription, kind: manyToOne, to: Subscription }
            notifications:
              - name: renewalRequested
                event: { onCreate: Renewal }
                to: requestedBy
                subject: "Your {subscription.plan.name} renewal for {subscription.customer.name}"
                body: "Open it at {subscription.plan.portalUrl}"
            """;

    /** The same chain at the other three sites: a process step, a transition and a schedule. */
    private static final String EVERY_SITE = YAML + """
            processes:
              - name: Renewing
                trigger: { onCreate: Renewal }
                steps:
                  - name: notify
                    kind: serviceTask
                    args:
                      notify:
                        to: subscription.customer.email
                        subject: "Renewing for {subscription.customer.name}"
                        body: "Renewing."
                      next: done
                  - { name: done, kind: end }
            transitions:
              - name: Confirm
                forEntity: Renewal
                from: [1]
                setStatus: 2
                notify:
                  to: subscription.customer.email
                  subject: "Confirmed for {subscription.customer.name}"
                  body: "Confirmed."
            schedules:
              - name: renewalReminder
                cron: "0 0 8 * * ?"
                entity: Renewal
                notify:
                  to: subscription.customer.email
                  subject: "Reminder for {subscription.customer.name}"
                  body: "Reminder."
            """;

    /** The catalog key the notification's texts are resolved under (#8787), less the part. */
    private static final String NOTIFICATION = "renewals:renewals-model.mail.notifications.renewalRequested.";

    private static final String CUSTOMER_NAME = "(subscription_customer == null ? null : subscription_customer.Name)";
    private static final String CUSTOMER_EMAIL = "(subscription_customer == null ? null : subscription_customer.Email)";
    private static final List<String> CUSTOMER_CHAIN =
            List.of("subscription <- entity.Subscription", "subscription_customer <- subscription.Customer");

    @Test
    void theIssueModelReadsThroughTheSubscription() {
        Map<String, Object> notification = notification(YAML);
        assertEquals(GlueRendering.message(NOTIFICATION + "subject",
                "Your {subscription.plan.name} renewal for {subscription.customer.name}", "subscription.plan.name",
                "(subscription_plan == null ? null : subscription_plan.Name)", "subscription.customer.name", CUSTOMER_NAME),
                notification.get("subjectExpression"));
        assertEquals(GlueRendering.message(NOTIFICATION + "body", "Open it at {subscription.plan.portalUrl}", "subscription.plan.portalUrl",
                "(subscription_plan == null ? null : subscription_plan.PortalUrl)"), notification.get("bodyExpression"));
        assertEquals(List.of("subscription <- entity.Subscription", "subscription_plan <- subscription.Plan",
                "subscription_customer <- subscription.Customer"), loads(notification.get("relationLoads")));
    }

    @Test
    void aTwoHopPlaceholderLoadsTheChain() {
        Map<String, Object> notification = notification(subject("{subscription.customer.name}"));
        assertEquals(GlueRendering.message(NOTIFICATION + "subject", "{subscription.customer.name}", "subscription.customer.name",
                CUSTOMER_NAME), notification.get("subjectExpression"));
        assertEquals(CUSTOMER_CHAIN, loads(notification.get("relationLoads")));
    }

    @Test
    void aSharedPrefixIsLoadedOnce() {
        Map<String, Object> notification = notification(subject("{subscription.customer.name} {subscription.plan.name} {subscription.id}"));
        assertEquals(List.of("subscription <- entity.Subscription", "subscription_customer <- subscription.Customer",
                "subscription_plan <- subscription.Plan"), loads(notification.get("relationLoads")));
    }

    @Test
    void aThreeHopPathResolves() {
        Map<String, Object> notification = notification(subject("{subscription.customer.country.name}"));
        assertEquals(
                GlueRendering.message(NOTIFICATION + "subject", "{subscription.customer.country.name}",
                        "subscription.customer.country.name",
                        "(subscription_customer_country == null ? null : subscription_customer_country.Name)"),
                notification.get("subjectExpression"));
        assertEquals(List.of("subscription <- entity.Subscription", "subscription_customer <- subscription.Customer",
                "subscription_customer_country <- subscription_customer.Country"), loads(notification.get("relationLoads")));
    }

    @Test
    void aRecipientWalksTheChain() {
        Map<String, Object> notification = notification(subject("Renewal").replace("to: requestedBy", "to: subscription.customer.email"));
        assertEquals(CUSTOMER_EMAIL, notification.get("toExpression"));
        assertEquals(CUSTOMER_CHAIN, loads(notification.get("relationLoads")));
    }

    @Test
    void everySiteResolvesTheChain() {
        IntentModel model = IntentParser.parse(EVERY_SITE);
        Map<String, Object> send = GlueIntentGenerator.buildSendsForTest(model)
                                                      .get(0);
        assertEquals(CUSTOMER_EMAIL, send.get("notifyToExpression"));
        assertEquals(
                GlueRendering.message("renewals:renewals-model.mail.steps.Renewing.notify.subject",
                        "Renewing for {subscription.customer.name}", "subscription.customer.name", CUSTOMER_NAME),
                send.get("notifySubjectExpression"));
        assertEquals(CUSTOMER_CHAIN, loads(send.get("notifyRelationLoads")));

        Map<String, Object> transition = GlueIntentGenerator.buildTransitionsForTest(model)
                                                            .get(0);
        assertEquals(CUSTOMER_EMAIL, transition.get("notifyToExpression"));
        assertEquals(
                GlueRendering.message("renewals:renewals-model.mail.transitions.Confirm.subject",
                        "Confirmed for {subscription.customer.name}", "subscription.customer.name", CUSTOMER_NAME),
                transition.get("notifySubjectExpression"));
        assertEquals(CUSTOMER_CHAIN, loads(transition.get("notifyRelationLoads")));

        Map<String, Object> schedule = GlueIntentGenerator.buildSchedulesForTest(model)
                                                          .get(0);
        assertEquals(CUSTOMER_EMAIL, schedule.get("toExpression"));
        assertEquals(
                GlueRendering.message("renewals:renewals-model.mail.schedules.renewalReminder.subject",
                        "Reminder for {subscription.customer.name}", "subscription.customer.name", CUSTOMER_NAME),
                schedule.get("subjectExpression"));
        assertEquals(CUSTOMER_CHAIN, loads(schedule.get("relationLoads")));
    }

    /** Inside a fan-out the walk starts at the ROW - the contact's own subscription. */
    @Test
    void aForEachRowPathWalksFromTheRow() {
        String yaml =
                EVERY_SITE.replace("      to: subscription.customer.email\n      subject: \"Confirmed for {subscription.customer.name}\"\n",
                        "      forEach: RenewalContact\n      to: email\n"
                                + "      subject: \"Confirmed for {subscription.customer.name} ({record.requestedBy})\"\n");
        Map<String, Object> transition = GlueIntentGenerator.buildTransitionsForTest(IntentParser.parse(yaml))
                                                            .get(0);
        assertEquals("RenewalContact", transition.get("forEach"));
        assertEquals("entity.Email", transition.get("notifyToExpression"));
        assertEquals(GlueRendering.message("renewals:renewals-model.mail.transitions.Confirm.subject",
                "Confirmed for {subscription.customer.name} ({record.requestedBy})", "subscription.customer.name", CUSTOMER_NAME,
                "record.requestedBy", "source.RequestedBy"), transition.get("notifySubjectExpression"));
        assertEquals(CUSTOMER_CHAIN, loads(transition.get("notifyRelationLoads")));
    }

    /**
     * A one-hop path projects the very keys it always did - no `source` - so its output is unchanged.
     */
    @Test
    void oneHopOutputIsUnchanged() {
        Map<String, Object> notification = notification(subject("Renewal {subscription.id}"));
        assertEquals(GlueRendering.message(NOTIFICATION + "subject", "Renewal {subscription.id}", "subscription.id",
                "(subscription == null ? null : subscription.Id)"), notification.get("subjectExpression"));
        List<?> loads = (List<?>) notification.get("relationLoads");
        assertEquals(1, loads.size(), loads.toString());
        Map<?, ?> load = (Map<?, ?>) loads.get(0);
        assertEquals(Set.of("local", "targetEntity", "targetPerspective", "fkProperty", "crossModel", "targetModel", "targetProject"),
                load.keySet());
        assertEquals("subscription", load.get("local"));
        assertEquals("Subscription", load.get("fkProperty"));
    }

    private static String subject(String subject) {
        return YAML.replace("subject: \"Your {subscription.plan.name} renewal for {subscription.customer.name}\"",
                "subject: \"" + subject + "\"")
                   .replace("body: \"Open it at {subscription.plan.portalUrl}\"", "body: \"Body\"");
    }

    private static Map<String, Object> notification(String yaml) {
        return GlueIntentGenerator.buildNotificationsForTest(IntentParser.parse(yaml))
                                  .get(0);
    }

    /**
     * Each load as {@code local <- source.fkProperty}, the source being the record when not carried.
     */
    private static List<String> loads(Object loads) {
        List<String> described = new ArrayList<>();
        for (Object load : (List<?>) loads) {
            Map<?, ?> fields = (Map<?, ?>) load;
            Object source = fields.containsKey("source") ? fields.get("source") : "entity";
            described.add(fields.get("local") + " <- " + source + "." + fields.get("fkProperty"));
        }
        return described;
    }
}
