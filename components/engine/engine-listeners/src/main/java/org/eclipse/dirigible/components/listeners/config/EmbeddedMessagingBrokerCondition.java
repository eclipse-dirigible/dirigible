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
 * Matches when no external messaging broker is configured, i.e. when the platform runs its own
 * embedded broker. It is the exact negation of {@link ExternalMessagingBrokerCondition}, so exactly
 * one of the two always matches - the messaging beans can never be defined twice or not at all.
 * <p>
 * Anything that only makes sense against the in-process broker - the {@code BrokerService} bean
 * itself, the monitoring component that introspects it - is conditional on this. See
 * {@link ExternalMessagingBrokerCondition} for why the decision is read from
 * {@link DirigibleConfig} instead of the Spring {@code Environment}.
 */
public class EmbeddedMessagingBrokerCondition implements Condition {

    /**
     * Matches.
     *
     * @param context the context
     * @param metadata the metadata
     * @return true, if no external broker is configured
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return !ExternalMessagingBrokerCondition.isExternalBrokerConfigured();
    }
}
