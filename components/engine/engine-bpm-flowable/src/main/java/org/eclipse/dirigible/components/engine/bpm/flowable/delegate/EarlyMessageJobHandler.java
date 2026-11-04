/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import java.util.List;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.event.MessageEventHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.EventSubscriptionUtil;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The job {@link EarlyMessages#checkWhenArmed} schedules when a message catch event is armed: once
 * that has committed, it delivers the message kept for the instance before the catch event
 * subscribed, if there is one. Nothing kept, or a catch event resumed meanwhile, is a no-op.
 */
public class EarlyMessageJobHandler implements JobHandler {

    /** The job handler type. */
    public static final String TYPE = "dirigible-early-message";

    private static final Logger LOGGER = LoggerFactory.getLogger(EarlyMessageJobHandler.class);

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String messageName, VariableScope variableScope, CommandContext commandContext) {
        ProcessEngineConfigurationImpl configuration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExecutionEntity processInstance = configuration.getExecutionEntityManager()
                                                       .findById(job.getProcessInstanceId());
        if (processInstance == null || processInstance.getVariable(EarlyMessages.variableName(messageName)) == null) {
            return;
        }
        List<EventSubscriptionEntity> subscriptions = configuration.getEventSubscriptionServiceConfiguration()
                                                                   .getEventSubscriptionService()
                                                                   .findEventSubscriptionsByNameAndExecution(
                                                                           MessageEventHandler.EVENT_HANDLER_TYPE, messageName,
                                                                           job.getExecutionId());
        if (subscriptions.isEmpty()) {
            return;
        }
        LOGGER.debug("Process instance [{}] reached its catch event for message [{}] after the message arrived - delivering it now",
                job.getProcessInstanceId(), messageName);
        EventSubscriptionUtil.eventReceived(subscriptions.get(0), null, false);
    }
}
