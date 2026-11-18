/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.webhooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The checks of a webhook registry: each problem is named, and all of them at once.
 */
class TenantWebhookSyncValidationTest {

    private static final String TYPE = "hr-Leave-Leave-transitioned";
    private static final String SECRET = "a-signing-secret-of-enough-length";

    private static List<String> problems(TenantWebhookSync body, Set<String> stored, int max) {
        return TenantWebhookSyncValidation.problems(body, TYPE::equals, stored::contains, max,
                url -> url.startsWith("https://") ? Optional.empty() : Optional.of("must be an https URL"));
    }

    private static TenantWebhookSync.Subscription subscription(String id, String url, List<String> events, String status, String secret,
            String previous) {
        return new TenantWebhookSync.Subscription(id, "Scheduling", url, events, status, secret, previous, "owner@acme.test",
                "2026-11-17T12:00:00Z", null);
    }

    private static TenantWebhookSync.Subscription valid(String id) {
        return subscription(id, "https://hooks.example.com/" + id, List.of(TYPE), "ACTIVE", SECRET, null);
    }

    @Test
    void acceptsAValidRegistry() {
        assertEquals(List.of(), problems(new TenantWebhookSync(3L, List.of(valid("s1"), valid("s2"))), Set.of(), 20));
        assertEquals(List.of(), problems(new TenantWebhookSync(0L, List.of()), Set.of(), 20), "an empty registry removes everything");
    }

    @Test
    void namesEveryProblemOfEverySubscription() {
        List<TenantWebhookSync.Subscription> subscriptions = new ArrayList<>();
        subscriptions.add(subscription("bad id/../", "http://hooks.example.com/", List.of(), "FAILING", "short", null));
        subscriptions.add(subscription("s2", "https://hooks.example.com/", List.of("hr-Leave-Leave-rekeyed"), null, null, SECRET));
        subscriptions.add(valid("s3"));
        subscriptions.add(valid("s3"));

        List<String> problems = problems(new TenantWebhookSync(-1L, subscriptions), Set.of(), 20);

        assertTrue(problems.contains("revision must be a whole number of at least 0"), problems.toString());
        assertTrue(problems.stream()
                           .anyMatch(p -> p.startsWith("subscriptions[0].subscriptionId")),
                problems.toString());
        assertTrue(problems.contains("subscriptions[0].url must be an https URL"), problems.toString());
        assertTrue(problems.contains("subscriptions[0].events must name at least one event type"), problems.toString());
        assertTrue(problems.contains("subscriptions[0].status must be ACTIVE or DISABLED"), problems.toString());
        assertTrue(problems.contains("subscriptions[0].secret must be 24 to 512 characters"), problems.toString());
        assertTrue(problems.stream()
                           .anyMatch(p -> p.startsWith("subscriptions[1].events names [hr-Leave-Leave-rekeyed]")),
                problems.toString());
        assertTrue(problems.contains("subscriptions[1].secret is required for a new subscription"), problems.toString());
        assertTrue(problems.contains("subscriptions[1].previousSecret is only read with a secret"), problems.toString());
        assertTrue(problems.contains("subscriptions[3].subscriptionId [s3] is named twice"), problems.toString());
    }

    @Test
    void aStoredSubscriptionMayLeaveItsSecretOut() {
        TenantWebhookSync.Subscription withoutSecret = subscription("s1", "https://hooks.example.com/", List.of(TYPE), null, null, null);
        assertEquals(List.of(), problems(new TenantWebhookSync(4L, List.of(withoutSecret)), Set.of("s1"), 20));
    }

    @Test
    void capsTheSubscriptionsOfATenant() {
        List<String> problems = problems(new TenantWebhookSync(1L, List.of(valid("s1"), valid("s2"), valid("s3"))), Set.of(), 2);
        assertEquals(List.of("a tenant may hold at most 2 subscriptions, the registry names 3"), problems);
    }

    @Test
    void theStatusSentNeverOverridesFailingButDisabledAlwaysWins() {
        assertEquals(TenantWebhookStatus.ACTIVE, TenantWebhookRegistryService.statusAfter(null, null));
        assertEquals(TenantWebhookStatus.ACTIVE, TenantWebhookRegistryService.statusAfter(TenantWebhookStatus.REMOVED, "ACTIVE"));
        assertEquals(TenantWebhookStatus.ACTIVE, TenantWebhookRegistryService.statusAfter(TenantWebhookStatus.DISABLED, "ACTIVE"));
        assertEquals(TenantWebhookStatus.FAILING, TenantWebhookRegistryService.statusAfter(TenantWebhookStatus.FAILING, "ACTIVE"));
        assertEquals(TenantWebhookStatus.DISABLED, TenantWebhookRegistryService.statusAfter(TenantWebhookStatus.FAILING, "DISABLED"));
    }
}
