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

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Checks a webhook registry before anything is applied, and names every problem at once.
 *
 * <p>
 * What a registry must satisfy: a non-negative revision; at most the configured number of
 * subscriptions; per subscription a unique id, a name, a URL the {@link WebhookUrlGuard} accepts,
 * at least one event type and only types an application declares, a status of {@code ACTIVE} or
 * {@code DISABLED} when one is sent, a secret of at least {@link #MIN_SECRET_LENGTH} characters
 * when one is sent (and always for a subscription the tenant does not hold yet), and ISO-8601
 * timestamps.
 */
final class TenantWebhookSyncValidation {

    /** The shortest secret accepted - 128 bits of a 64-character alphabet would be 22. */
    static final int MIN_SECRET_LENGTH = 24;

    private static final int MAX_SECRET_LENGTH = 512;

    private static final int MAX_LENGTH = 255;

    private static final int MAX_URL_LENGTH = 2000;

    /** A subscription id: what the provisioning system's ids look like, nothing a path could abuse. */
    private static final Pattern SUBSCRIPTION_ID = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{0,254}$");

    private TenantWebhookSyncValidation() {}

    /**
     * Every problem of a registry.
     *
     * @param body the registry
     * @param declared whether an event type is declared by a published application
     * @param stored whether the tenant already holds a live subscription of an id - one that may leave
     *        its secret out
     * @param maxSubscriptions the most subscriptions a tenant may hold
     * @param urlProblem the URL check
     * @return the problems, empty when the registry may be applied
     */
    static List<String> problems(TenantWebhookSync body, Predicate<String> declared, Predicate<String> stored, int maxSubscriptions,
            Function<String, Optional<String>> urlProblem) {
        List<String> problems = new ArrayList<>();
        if (body == null) {
            problems.add("the body is missing");
            return problems;
        }
        if (body.revision() == null || body.revision() < 0) {
            problems.add("revision must be a whole number of at least 0");
        }
        List<TenantWebhookSync.Subscription> subscriptions = body.subscriptions() == null ? List.of() : body.subscriptions();
        if (subscriptions.size() > maxSubscriptions) {
            problems.add("a tenant may hold at most " + maxSubscriptions + " subscriptions, the registry names " + subscriptions.size());
        }
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < subscriptions.size(); i++) {
            TenantWebhookSync.Subscription subscription = subscriptions.get(i);
            String at = "subscriptions[" + i + "]";
            if (subscription == null) {
                problems.add(at + " is null");
                continue;
            }
            String id = subscription.subscriptionId();
            if (id == null || !SUBSCRIPTION_ID.matcher(id)
                                              .matches()) {
                problems.add(at + ".subscriptionId must be letters, digits, '.', '_', ':' or '-', at most 255");
            } else if (!seen.add(id)) {
                problems.add(at + ".subscriptionId [" + id + "] is named twice");
            }
            if (blank(subscription.name()) || subscription.name()
                                                          .trim()
                                                          .length() > MAX_LENGTH) {
                problems.add(at + ".name is required, at most " + MAX_LENGTH + " characters");
            }
            if (subscription.url() == null || subscription.url()
                                                          .length() > MAX_URL_LENGTH) {
                problems.add(at + ".url is required, at most " + MAX_URL_LENGTH + " characters");
            } else {
                urlProblem.apply(subscription.url())
                          .ifPresent(problem -> problems.add(at + ".url " + problem));
            }
            checkEvents(at, subscription.events(), declared, problems);
            if (subscription.status() != null && !TenantWebhookStatus.ACTIVE.name()
                                                                            .equals(subscription.status())
                    && !TenantWebhookStatus.DISABLED.name()
                                                    .equals(subscription.status())) {
                problems.add(at + ".status must be ACTIVE or DISABLED");
            }
            checkSecrets(at, subscription, id != null && stored.test(id), problems);
            if (subscription.createdAt() != null && instant(subscription.createdAt()) == null) {
                problems.add(at + ".createdAt must be an ISO-8601 instant");
            }
            if (subscription.createdBy() != null && subscription.createdBy()
                                                                .length() > 320) {
                problems.add(at + ".createdBy is at most 320 characters");
            }
        }
        return problems;
    }

    private static void checkEvents(String at, List<String> events, Predicate<String> declared, List<String> problems) {
        if (events == null || events.isEmpty()) {
            problems.add(at + ".events must name at least one event type");
            return;
        }
        for (String event : events) {
            if (blank(event)) {
                problems.add(at + ".events holds an empty event type");
            } else if (!declared.test(event)) {
                problems.add(at + ".events names [" + event + "], which no published application declares - see GET webhook-events");
            }
        }
    }

    private static void checkSecrets(String at, TenantWebhookSync.Subscription subscription, boolean stored, List<String> problems) {
        if (subscription.secret() == null) {
            if (!stored) {
                problems.add(at + ".secret is required for a new subscription");
            }
            if (subscription.previousSecret() != null) {
                problems.add(at + ".previousSecret is only read with a secret");
            }
            return;
        }
        checkSecret(at + ".secret", subscription.secret(), problems);
        if (subscription.previousSecret() != null) {
            checkSecret(at + ".previousSecret", subscription.previousSecret(), problems);
        }
    }

    private static void checkSecret(String at, String secret, List<String> problems) {
        if (secret.length() < MIN_SECRET_LENGTH || secret.length() > MAX_SECRET_LENGTH) {
            problems.add(at + " must be " + MIN_SECRET_LENGTH + " to " + MAX_SECRET_LENGTH + " characters");
        }
    }

    /**
     * Parses an ISO-8601 instant.
     *
     * @param text the text
     * @return the instant, or {@code null} for a missing or unparsable text
     */
    static Instant instant(String text) {
        if (text == null) {
            return null;
        }
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }
}
