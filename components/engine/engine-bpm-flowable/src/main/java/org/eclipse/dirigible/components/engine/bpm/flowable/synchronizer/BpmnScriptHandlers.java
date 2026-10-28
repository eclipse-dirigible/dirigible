/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.synchronizer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.DirigibleCallDelegate;
import org.eclipse.dirigible.components.engine.bpm.flowable.delegate.DirigibleTaskListener;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.impl.util.io.InputStreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds the script handler a process definition runs, which only the JavaScript engine can run
 * (#8174): a service task or a listener bound to the {@code JSTask} / {@code JSListener} beans - by
 * delegate expression or by class - with the script in its {@code handler} field.
 */
final class BpmnScriptHandlers {

    private static final Logger logger = LoggerFactory.getLogger(BpmnScriptHandlers.class);

    private static final String HANDLER_FIELD = "handler";

    private static final Set<String> SCRIPT_DELEGATE_EXPRESSIONS = Set.of("${JSTask}", "${JSListener}");

    private static final Set<String> SCRIPT_DELEGATE_CLASSES =
            Set.of(DirigibleCallDelegate.class.getName(), DirigibleTaskListener.class.getName());

    private BpmnScriptHandlers() {}

    /**
     * The script the process definition runs, when it runs one.
     *
     * @param location the location of the definition
     * @param content the BPMN XML
     * @return the handler of the first script step, or the id of the step when it names none; empty
     *         when the definition runs no script, or cannot be read - the deployment reports that
     */
    static Optional<String> find(String location, String content) {
        BpmnModel model;
        try {
            model = new BpmnXMLConverter().convertToBpmnModel(
                    new InputStreamSource(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))), false, true);
        } catch (RuntimeException ex) {
            logger.debug("BPMN [{}] could not be read for its script handlers", location, ex);
            return Optional.empty();
        }
        for (Process process : model.getProcesses()) {
            Optional<String> handler = find(process.getId(), process.getExecutionListeners());
            if (handler.isPresent()) {
                return handler;
            }
            for (FlowElement element : process.findFlowElementsOfType(FlowElement.class, true)) {
                handler = find(element);
                if (handler.isPresent()) {
                    return handler;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> find(FlowElement element) {
        if (element instanceof ServiceTask task && isScriptDelegate(task.getImplementationType(), task.getImplementation())) {
            return Optional.of(handler(task.getId(), task.getFieldExtensions()));
        }
        List<FlowableListener> listeners = new ArrayList<>(element.getExecutionListeners());
        if (element instanceof UserTask task) {
            listeners.addAll(task.getTaskListeners());
        }
        return find(element.getId(), listeners);
    }

    private static Optional<String> find(String elementId, List<FlowableListener> listeners) {
        return listeners.stream()
                        .filter(listener -> isScriptDelegate(listener.getImplementationType(), listener.getImplementation()))
                        .findFirst()
                        .map(listener -> handler(elementId, listener.getFieldExtensions()));
    }

    private static boolean isScriptDelegate(String implementationType, String implementation) {
        if (implementation == null) {
            return false;
        }
        String trimmed = implementation.trim();
        if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(implementationType)) {
            return SCRIPT_DELEGATE_EXPRESSIONS.contains(trimmed.replace(" ", ""));
        }
        return ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(implementationType) && SCRIPT_DELEGATE_CLASSES.contains(trimmed);
    }

    private static String handler(String elementId, List<FieldExtension> fields) {
        return fields.stream()
                     .filter(field -> HANDLER_FIELD.equals(field.getFieldName()))
                     .map(field -> field.getStringValue() != null ? field.getStringValue() : field.getExpression())
                     .filter(value -> value != null && !value.isBlank())
                     .map(String::trim)
                     .findFirst()
                     .orElse(elementId);
    }
}
