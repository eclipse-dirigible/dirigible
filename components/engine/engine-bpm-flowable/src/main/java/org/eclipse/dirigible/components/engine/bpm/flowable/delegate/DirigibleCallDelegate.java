/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.delegate;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.annotation.Nullable;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ExecutionData;
import org.eclipse.dirigible.components.open.telemetry.OpenTelemetryProvider;
import org.eclipse.dirigible.graalium.core.DirigibleJavascriptCodeRunner;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.el.FixedValue;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.eclipse.dirigible.components.engine.bpm.flowable.dto.ActionData.Action.SKIP;
import static org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService.DIRIGIBLE_BPM_INTERNAL_SKIP_STEP;

/**
 * The Class DirigibleCallDelegate.
 */
// don't change the name of the bean or the class name and package
// otherwise processes will stop working
@Component("JSTask")
public class DirigibleCallDelegate implements JavaDelegate {

    /** The js expression regex. */
    private static final Pattern JS_EXPRESSION_REGEX = Pattern.compile("(.*\\.(?:m?js|ts))(?:\\/(\\w*))?(?:\\/(\\w*))?");

    /**
     * The handler.
     */
    private FixedValue handler;

    /**
     * The type.
     */
    private FixedValue type;


    /**
     * The Class JSTask.
     */
    static class JSTask {

        /** The source file path. */
        private final Path sourceFilePath;

        /** The class name. */
        private final @Nullable String className;

        /** The method name. */
        private final @Nullable String methodName;

        /** The has exported class and method. */
        private final boolean hasExportedClassAndMethod;

        /** The has exported method. */
        private final boolean hasExportedMethod;

        /**
         * Instantiates a new JS task.
         *
         * @param sourceFilePath the source file path
         * @param className the class name
         * @param methodName the method name
         */
        JSTask(Path sourceFilePath, @Nullable String className, @Nullable String methodName) {
            this.sourceFilePath = sourceFilePath;
            this.className = className;
            this.methodName = methodName;
            this.hasExportedMethod = className == null && methodName != null;
            this.hasExportedClassAndMethod = className != null && methodName != null;
        }

        /**
         * From repository path.
         *
         * @param repositoryPath the repository path
         * @return the JS task
         */
        static JSTask fromRepositoryPath(RepositoryPath repositoryPath) {
            var matcher = JS_EXPRESSION_REGEX.matcher(repositoryPath.getPath());
            if (!matcher.matches()) {
                throw new BpmnError("Invalid JS expression provided for task! Path [" + repositoryPath.getPath() + "] doesn't match "
                        + JS_EXPRESSION_REGEX);
            }

            String maybeClassName;
            String maybeMethodName;

            if (matcher.group(2) != null && matcher.group(3) != null) {
                maybeClassName = matcher.group(2);
                maybeMethodName = matcher.group(3);
            } else {
                maybeClassName = null;
                maybeMethodName = matcher.group(2);
            }

            Path sourceFilePath = Path.of(matcher.group(1));
            return new JSTask(sourceFilePath, maybeClassName, maybeMethodName);
        }

        /**
         * Gets the source file path.
         *
         * @return the source file path
         */
        public Path getSourceFilePath() {
            return sourceFilePath;
        }

        /**
         * Gets the class name.
         *
         * @return the class name
         */
        public String getClassName() {
            return className;
        }

        /**
         * Gets the method name.
         *
         * @return the method name
         */
        public String getMethodName() {
            return methodName;
        }

        /**
         * Checks for exported class and method.
         *
         * @return true, if successful
         */
        public boolean hasExportedClassAndMethod() {
            return hasExportedClassAndMethod;
        }

        /**
         * Checks for exported method.
         *
         * @return true, if successful
         */
        public boolean hasExportedMethod() {
            return hasExportedMethod;
        }
    }

    /**
     * Getter for the handler attribute.
     *
     * @return the handler
     */
    public FixedValue getHandler() {
        return handler;
    }

    /**
     * Setter of the handler attribute.
     *
     * @param handler the handler
     */
    public void setHandler(FixedValue handler) {
        this.handler = handler;
    }

    /**
     * Getter for the engine attribute.
     *
     * @return the type
     */
    public FixedValue getType() {
        return type;
    }

    /**
     * Setter of the engine attribute.
     *
     * @param type the type
     */
    public void setType(FixedValue type) {
        this.type = type;
    }

    /**
     * Execute.
     *
     * @param execution the execution
     */
    @Override
    public void execute(DelegateExecution execution) {
        Tracer tracer = OpenTelemetryProvider.get()
                                             .getTracer("eclipse-dirigible");
        Span span = tracer.spanBuilder("flowable_task_execution")
                          .startSpan();
        try (Scope scope = span.makeCurrent()) {
            addSpanAttributes(execution, span);

            executeInternal(execution);
        } catch (RuntimeException e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Exception occurred during task execution");

            throw e;
        } finally {
            span.end();
        }
    }

    private void addSpanAttributes(DelegateExecution execution, Span span) {
        String executionId = execution.getId();
        span.setAttribute("execution.id", executionId);

        String processInstanceId = execution.getProcessInstanceId();
        span.setAttribute("process.instance.id", processInstanceId);

        String processInstanceBusinessKey = execution.getProcessInstanceBusinessKey();
        span.setAttribute("process.instance.business.key", processInstanceBusinessKey);

        String processDefinitionId = execution.getProcessDefinitionId();
        span.setAttribute("process.definition.id", processDefinitionId);
    }

    private void executeInternal(DelegateExecution execution) {
        String action = (String) execution.getVariable(DIRIGIBLE_BPM_INTERNAL_SKIP_STEP);
        if (SKIP.getActionName()
                .equals(action)) {
            execution.removeVariable(DIRIGIBLE_BPM_INTERNAL_SKIP_STEP);
            return;
        }

        Map<Object, Object> context = new HashMap<>();
        ExecutionData executionData = new ExecutionData();
        BeanUtils.copyProperties(execution, executionData);
        context.put("execution", GsonHelper.toJson(executionData));
        if (type == null) {
            type = new FixedValue("javascript");
        }
        if (handler == null) {
            throw new BpmnError("Handler cannot be null at the call delegate.");
        }
        executeJSHandler(context);
    }

    /**
     * Execute JS handler.
     *
     * @param context the context
     */
    private void executeJSHandler(Map<Object, Object> context) {
        RepositoryPath path = new RepositoryPath(handler.getExpressionText());
        JSTask task = JSTask.fromRepositoryPath(path);

        Span.current()
            .setAttribute("handler", path.toString());

        try (DirigibleJavascriptCodeRunner runner = new DirigibleJavascriptCodeRunner(context, false)) {
            Source source = runner.prepareSource(task.getSourceFilePath());
            Value value = runner.run(source);

            if (task.hasExportedClassAndMethod()) {
                value.getMember(task.getClassName())
                     .newInstance()
                     .getMember(task.getMethodName())
                     .executeVoid();
            } else if (task.hasExportedMethod()) {
                value.getMember(task.getMethodName())
                     .executeVoid();
            }

        }
    }

}
