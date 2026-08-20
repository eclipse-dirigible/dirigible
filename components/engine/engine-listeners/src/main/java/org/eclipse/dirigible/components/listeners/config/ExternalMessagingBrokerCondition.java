/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.config;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches when an external messaging broker is configured, i.e. when
 * {@link DirigibleConfig#MESSAGING_BROKER_URL} holds a value.
 * <p>
 * The decision is read from Dirigible's own configuration rather than through
 * {@code @ConditionalOnProperty}: Dirigible resolves a key across its own layers (runtime, tenant,
 * environment, deployment, module) and a value placed there programmatically - which is how the
 * integration tests select the mode - never reaches the Spring {@code Environment}.
 * <p>
 * Public because {@link EmbeddedMessagingBrokerCondition}, its counterpart, is referenced from the
 * messaging monitoring component in another module.
 */
public class ExternalMessagingBrokerCondition implements Condition {

    /**
     * Whether a broker URL is configured. Blank counts as unset - an empty environment variable is a
     * common way to "not set" one in a container.
     *
     * @return true if an external broker is configured
     */
    static boolean isExternalBrokerConfigured() {
        String brokerUrl = DirigibleConfig.MESSAGING_BROKER_URL.getStringValue();
        return null != brokerUrl && !brokerUrl.isBlank();
    }

    /**
     * Matches.
     *
     * @param context the context
     * @param metadata the metadata
     * @return true, if an external broker is configured
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return isExternalBrokerConfigured();
    }
}
