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

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts the FINAL failed attempt of a {@code flowable:class} service task into the caught
 * {@link #ERROR_CODE} BPMN error - the runtime half of the intent DSL's declarative step resilience
 * ({@code retry:} / {@code onError:}, dirigible #6762).
 *
 * <p>
 * A BPMN error boundary event fires only on a {@link BpmnError}; a delegate's plain
 * {@code RuntimeException} fails the async job instead (rollback, retry cycle, dead-letter
 * incident), so an {@code onError:} route would never be taken. This helper closes that gap at the
 * one moment the outcome is decided: when an attempt fails AND the task carries an intent error
 * boundary AND no further retry will run, the failure message is published as the
 * {@link #ERROR_MESSAGE_VARIABLE} process variable (a {@code BpmnError} commits, so the variable
 * persists for the error route's {@code {error}} reads) and the error is propagated to the
 * boundary. Every other failure is left to the engine's own machinery - a task without the boundary
 * behaves exactly as before.
 *
 * <p>
 * "Final" mirrors Flowable's {@code JobRetryCmd} arithmetic exactly: with no
 * {@code failedJobRetryTimeCycle} the first failure is final (the intent declared no retry); with a
 * cycle {@code R<n>/<every>}, the first failure (no exception message on the job yet) is final only
 * when {@code n <= 1}, and a later attempt is final when the job's remaining retries are down to
 * one. The error code and the variable name are string contracts with the intent BPMN generator
 * ({@code ProcessResilienceSupport} in {@code engine-intent}), like the {@code ${JavaTask}}
 * delegate-expression literal.
 */
final class IntentStepResilience {

    /** The BPMN error code the generated {@code onError} boundary events catch. */
    static final String ERROR_CODE = "INTENT_STEP_FAILED";

    /** The process variable carrying the failure message for the error route's {@code {error}}. */
    static final String ERROR_MESSAGE_VARIABLE = "__errorMessage";

    private static final Logger LOGGER = LoggerFactory.getLogger(IntentStepResilience.class);

    private IntentStepResilience() {}

    /**
     * Convert the failure to the caught intent BPMN error when this is the final attempt of a task
     * carrying an intent error boundary.
     *
     * @param execution the failing execution
     * @param exception the delegate's failure
     * @return {@code true} when the failure was converted and routed (the caller must not rethrow);
     *         {@code false} when the engine's own failure handling should proceed
     */
    static boolean convertFinalFailure(DelegateExecution execution, RuntimeException exception) {
        FlowElement element = execution.getCurrentFlowElement();
        if (!(element instanceof ServiceTask serviceTask) || !hasIntentErrorBoundary(execution, serviceTask)) {
            return false;
        }
        Job job = currentJob(execution, serviceTask);
        if (!isFinalAttempt(serviceTask.getFailedJobRetryTimeCycleValue(), job == null ? null : job.getExceptionMessage(),
                job == null ? null : job.getRetries())) {
            return false;
        }
        String message = exception.getMessage() == null || exception.getMessage()
                                                                    .isBlank() ? exception.getClass()
                                                                                          .getName()
                                                                            : exception.getMessage();
        LOGGER.debug("Routing the final failed attempt of [{}] to its onError boundary: {}", serviceTask.getId(), message, exception);
        execution.setVariable(ERROR_MESSAGE_VARIABLE, message);
        ErrorPropagation.propagateError(new BpmnError(ERROR_CODE, message), execution);
        return true;
    }

    /**
     * Whether the task carries an error boundary event catching the intent error code - the generated
     * marker that opts a task into the conversion. A hand-authored process without it (or with a
     * boundary for a different code) keeps today's behaviour untouched.
     */
    private static boolean hasIntentErrorBoundary(DelegateExecution execution, ServiceTask serviceTask) {
        Process process = ProcessDefinitionUtil.getProcess(execution.getProcessDefinitionId());
        BpmnModel model = ProcessDefinitionUtil.getBpmnModel(execution.getProcessDefinitionId());
        if (process == null || model == null) {
            return false;
        }
        for (BoundaryEvent boundary : process.findFlowElementsOfType(BoundaryEvent.class, true)) {
            if (!serviceTask.getId()
                            .equals(boundary.getAttachedToRefId())) {
                continue;
            }
            for (EventDefinition definition : boundary.getEventDefinitions()) {
                if (definition instanceof ErrorEventDefinition errorDefinition
                        && ERROR_CODE.equals(resolveErrorCode(model, errorDefinition.getErrorCode()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The error code an event definition's reference resolves to: a reference to a declared
     * {@code <error>} maps through the model's error registry, a bare code stands for itself (the same
     * resolution {@code ErrorPropagation} applies when matching a thrown error).
     */
    private static String resolveErrorCode(BpmnModel model, String errorReference) {
        if (errorReference == null) {
            return null;
        }
        String mapped = model.getErrors()
                             .get(errorReference);
        return mapped != null ? mapped : errorReference;
    }

    /**
     * The async job currently executing this task, or null when none is visible (a synchronous
     * execution). Looked up by PROCESS INSTANCE and matched on execution + element id - deliberately
     * not {@code findJobsByExecutionId}, which does not surface the very job being executed (verified
     * against a live engine in {@code ResilientClassDelegateEngineTest}), while the instance-scoped
     * query does.
     */
    private static Job currentJob(DelegateExecution execution, ServiceTask serviceTask) {
        List<JobEntity> jobs = CommandContextUtil.getJobService()
                                                 .findJobsByProcessInstanceId(execution.getProcessInstanceId());
        if (jobs == null || jobs.isEmpty()) {
            return null;
        }
        JobEntity elementMatch = null;
        for (JobEntity job : jobs) {
            boolean sameElement = serviceTask.getId()
                                             .equals(job.getElementId());
            if (sameElement && execution.getId()
                                        .equals(job.getExecutionId())) {
                return job;
            }
            if (sameElement && elementMatch == null) {
                elementMatch = job;
            }
        }
        return elementMatch;
    }

    /**
     * Whether no further retry will run after this failure - the exact mirror of {@code JobRetryCmd}'s
     * decision, so the conversion fires precisely where the engine would otherwise dead-letter.
     *
     * @param retryCycle the task's {@code failedJobRetryTimeCycle} ({@code R<n>/<every>}), or null when
     *        the step declares no retry
     * @param jobExceptionMessage the executing job's recorded exception message - null exactly on the
     *        first attempt (that is how {@code JobRetryCmd} detects it too)
     * @param jobRetries the executing job's remaining retries, or null when no job is visible
     * @return whether this failure is the final one
     */
    static boolean isFinalAttempt(String retryCycle, String jobExceptionMessage, Integer jobRetries) {
        if (retryCycle == null || retryCycle.isBlank()) {
            return true; // no declared retry: the non-retried failure routes immediately
        }
        if (jobRetries == null) {
            return true; // no job to re-run - nothing would ever retry this attempt
        }
        if (jobExceptionMessage == null) {
            // First failure of a cycle job: JobRetryCmd reads the attempt budget off the cycle, so the
            // job's current retries (still the engine default) say nothing yet.
            return cycleAttempts(retryCycle) <= 1;
        }
        return jobRetries <= 1;
    }

    /**
     * The total attempts of an {@code R<n>/<every>} cycle, or {@link Integer#MAX_VALUE} for a shape
     * this helper does not recognize - never claim finality on a cycle the engine may read differently.
     */
    private static int cycleAttempts(String retryCycle) {
        String trimmed = retryCycle.trim();
        int slash = trimmed.indexOf('/');
        if (!trimmed.startsWith("R") || slash < 2) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(trimmed.substring(1, slash));
        } catch (NumberFormatException unrecognizedCycleShape) {
            return Integer.MAX_VALUE;
        }
    }
}
