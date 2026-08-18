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

import org.eclipse.dirigible.components.intent.model.OutboundTargetIntent;

/**
 * Reads an {@code OutboundIntent}'s {@code to:} block as the channel the generated publisher sends
 * on - which SDK {@code Producer} call it makes, and to which destination name. Pure (no Spring /
 * IO), so the one rule that matters here (exactly one channel, and which one won) is unit-tested
 * directly.
 *
 * <p>
 * The destination name is passed to the platform <b>verbatim</b>. It is resolved at send time by
 * the platform's own destination naming, which tenant-prefixes an application-owned name - so two
 * deployments that must share a queue need the platform's external-contract marker rather than a
 * name this layer rewrites. Nothing about that resolution belongs in the intent layer.
 */
public final class OutboundSupport {

    /** The channel a departure leaves on. */
    public enum Channel {

        /** Point-to-point - exactly one consumer receives the message. */
        QUEUE("sendToQueue"),
        /** Pub/sub - every active subscriber receives the message. */
        TOPIC("sendToTopic");

        private final String producerMethod;

        Channel(String producerMethod) {
            this.producerMethod = producerMethod;
        }

        /**
         * @return the {@code sdk.messaging.Producer} method that sends on this channel
         */
        public String producerMethod() {
            return producerMethod;
        }
    }

    /**
     * One resolved departure channel: the kind and the destination name, as authored.
     *
     * @param channel the channel kind
     * @param destination the destination name, passed to the platform verbatim
     */
    public record Target(Channel channel, String destination) {

        /**
         * @return the {@code sdk.messaging.Producer} method the generated publisher calls
         */
        public String producerMethod() {
            return channel.producerMethod();
        }
    }

    private OutboundSupport() {}

    /**
     * Resolve the authored {@code to:} block.
     *
     * @param to the target block, may be {@code null}
     * @return the resolved target, or {@code null} when it names no single channel (the parser has
     *         already reported that)
     */
    public static Target target(OutboundTargetIntent to) {
        if (to == null) {
            return null;
        }
        String queue = trimmed(to.getQueue());
        String topic = trimmed(to.getTopic());
        if (queue != null && topic == null) {
            return new Target(Channel.QUEUE, queue);
        }
        if (topic != null && queue == null) {
            return new Target(Channel.TOPIC, topic);
        }
        return null;
    }

    private static String trimmed(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
