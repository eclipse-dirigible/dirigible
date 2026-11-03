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

import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.event.EventDefinitionExpressionUtil;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.JobUtil;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.springframework.stereotype.Component;

/**
 * A message that reaches a running process instance before the instance is parked on the catch
 * event waiting for it (dirigible #8106).
 * <p>
 * A message catch event subscribes only once the process reaches it, so a message delivered in the
 * window between the step before it and the subscription - the entity event an intent {@code wait}
 * is resumed by, arriving while the process still runs the asynchronous steps after a user task -
 * found nothing to resume, and the instance parked on a wait whose event had already happened. Such
 * a message is now KEPT on the instance ({@code BpmProviderFlowable.deliverMessageEvent} writes the
 * {@link #variableName(String) marker variable}) and the catch event consumes it when it is armed.
 * <p>
 * Three expression listeners in the BPMN bind this bean:
 * <ul>
 * <li>{@link #checkWhenArmed(DelegateExecution)} on the catch event's {@code start}: schedules an
 * exclusive asynchronous job ({@link EarlyMessageJobHandler}) that delivers a kept message. A job,
 * not a check in place, because it runs after the arming transaction COMMITS: a message kept after
 * an in-place check read the instance, but before the subscription was visible to its own delivery
 * attempt, would otherwise be lost again - the window this exists to close.</li>
 * <li>{@link #consumed(DelegateExecution)} on the catch event's {@code end}: whatever resumed it,
 * the message kept for it is spent, so a later return to the same wait does not resume on it
 * again.</li>
 * <li>{@link #discardAll(DelegateExecution)} on each user task's {@code end}: what arrived before a
 * person completed a task is not an answer to what the process asks after it - only a message kept
 * since the process left its previous user task counts.</li>
 * </ul>
 */
@Component(EarlyMessages.BEAN_NAME)
public class EarlyMessages {

    /** The bean name the BPMN expression listeners use. */
    public static final String BEAN_NAME = "EarlyMessages";

    /** The prefix of the process variables marking a kept message. */
    static final String VARIABLE_PREFIX = "__earlyMessage_";

    /**
     * The process variable marking a message kept for the instance.
     *
     * @param messageName the message name
     * @return the variable name
     */
    public static String variableName(String messageName) {
        return VARIABLE_PREFIX + messageName;
    }

    /**
     * Schedules the delivery of a kept message for the message catch event the execution is entering,
     * to run once the transaction arming it has committed.
     *
     * @param execution the execution entering the catch event
     */
    public void checkWhenArmed(DelegateExecution execution) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        ProcessEngineConfigurationImpl configuration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        JobEntity job = JobUtil.createJob((ExecutionEntity) execution, EarlyMessageJobHandler.TYPE, configuration);
        job.setJobHandlerConfiguration(messageName(execution, commandContext));
        JobService jobService = configuration.getJobServiceConfiguration()
                                             .getJobService();
        jobService.createAsyncJob(job, true);
        jobService.scheduleAsyncJob(job);
    }

    /**
     * Forgets the message kept for the catch event the execution is leaving.
     *
     * @param execution the execution leaving the catch event
     */
    public void consumed(DelegateExecution execution) {
        execution.removeVariable(variableName(messageName(execution, CommandContextUtil.getCommandContext())));
    }

    /**
     * Forgets every message kept for the instance.
     *
     * @param execution the execution leaving a user task
     */
    public void discardAll(DelegateExecution execution) {
        List<String> kept = execution.getVariableNames()
                                     .stream()
                                     .filter(name -> name.startsWith(VARIABLE_PREFIX))
                                     .toList();
        kept.forEach(execution::removeVariable);
    }

    private static String messageName(DelegateExecution execution, CommandContext commandContext) {
        FlowElement element = execution.getCurrentFlowElement();
        if (element instanceof Event event) {
            for (Object definition : event.getEventDefinitions()) {
                if (definition instanceof MessageEventDefinition messageDefinition) {
                    return EventDefinitionExpressionUtil.determineMessageName(commandContext, messageDefinition, execution);
                }
            }
        }
        throw new IllegalStateException("The " + BEAN_NAME + " listener is bound to [" + execution.getCurrentActivityId()
                + "] of process definition [" + execution.getProcessDefinitionId() + "], which is not a message catch event");
    }
}
