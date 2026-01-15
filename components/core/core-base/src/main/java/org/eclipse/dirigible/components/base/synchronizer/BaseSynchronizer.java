/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.synchronizer;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantResult;
import org.eclipse.dirigible.components.open.telemetry.OpenTelemetryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.List;

/**
 * The Class BaseSynchronizer.
 */
public abstract class BaseSynchronizer<A extends Artefact, ID> implements Synchronizer<A, ID> {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(BaseSynchronizer.class);

    @Override
    public List<A> parse(String location, byte[] content) throws ParseException {
        Tracer tracer = OpenTelemetryProvider.get()
                                             .getTracer("eclipse-dirigible");

        Span span = tracer.spanBuilder(getSynchronizerSpanPrefix() + "parse_execution")
                          .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("location", location);

            return parseImpl(location, content);

        } catch (RuntimeException e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Exception occurred during synchronization");

            throw e;
        } finally {
            span.end();
        }
    }

    private String getSynchronizerSpanPrefix() {
        String synchronizerClassName = this.getClass()
                                           .getSimpleName();
        return "synchronizer_" + synchronizerClassName + "_";
    }

    protected abstract List<A> parseImpl(String location, byte[] content) throws ParseException;

    /**
     * Complete.
     *
     * @param wrapper the wrapper
     * @param flow the flow
     * @return true, if successful
     */
    @Override
    public boolean complete(TopologyWrapper<A> wrapper, ArtefactPhase flow) {
        Tracer tracer = OpenTelemetryProvider.get()
                                             .getTracer("eclipse-dirigible");

        Span span = tracer.spanBuilder(getSynchronizerSpanPrefix() + "complete_execution")
                          .startSpan();

        try (Scope scope = span.makeCurrent()) {
            addSpanAttributes(span, wrapper, flow);
            return completeInternal(wrapper, flow);

        } catch (RuntimeException e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Exception occurred during synchronization");

            throw e;
        } finally {
            span.end();
        }
    }

    private boolean completeInternal(TopologyWrapper<A> wrapper, ArtefactPhase flow) {
        A artefact = wrapper.getArtefact();
        ArtefactLifecycle lifecycle = artefact.getLifecycle();

        if (!multitenantExecution() || !isMultitenantArtefact(artefact)) {
            logger.debug("[{} will complete artefact with lifecycle [{}] in phase [{}]]...\nArtefact:[{}]", this, lifecycle, flow,
                    artefact);
            return completeImpl(wrapper, flow);
        }

        TenantContext tenantContext = BeanProvider.getTenantContext();
        List<TenantResult<Boolean>> results = tenantContext.executeForEachTenant(() -> {
            logger.debug("[{} will complete artefact with lifecycle [{}] in phase [{}]] for tenant [{}]...\\nArtefact:[{}]", this,
                    lifecycle, flow, tenantContext.getCurrentTenant(), artefact);
            artefact.setLifecycle(lifecycle);
            return completeImpl(wrapper, flow);
        });

        return results.stream()
                      .map(TenantResult::getResult)
                      .allMatch(Boolean.TRUE::equals);
    }

    /**
     * Multitenant execution.
     *
     * @return true, if successful
     */
    @Override
    public boolean multitenantExecution() {
        return false;
    }

    /**
     * Checks if is multitenant artefact.
     *
     * @param artefact the artefact
     * @return true, if is multitenant artefact
     */
    protected boolean isMultitenantArtefact(A artefact) {
        return false;
    }

    /**
     * Complete impl.
     *
     * @param wrapper the wrapper
     * @param flow the flow
     * @return true, if successful
     */
    protected abstract boolean completeImpl(TopologyWrapper<A> wrapper, ArtefactPhase flow);

    private void addSpanAttributes(Span span, TopologyWrapper<A> wrapper, ArtefactPhase phase) {
        addSpanAttributes(wrapper.getArtefact(), span);

        span.setAttribute("phase", phase.getValue());
    }

    private void addSpanAttributes(A artefact, Span span) {
        span.setAttribute("synchronizer", this.getClass()
                                              .getName());
        span.setAttribute("artefact.key", artefact.getKey());
    }

    /**
     * Cleanup.
     *
     * @param artefact the artefact
     */
    @Override
    public void cleanup(A artefact) {
        Tracer tracer = OpenTelemetryProvider.get()
                                             .getTracer("eclipse-dirigible");

        Span span = tracer.spanBuilder(getSynchronizerSpanPrefix() + "cleanup_execution")
                          .startSpan();

        try (Scope scope = span.makeCurrent()) {
            addSpanAttributes(artefact, span);

            cleanupInternal(artefact);

        } catch (RuntimeException e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Exception occurred during synchronization");

            throw e;
        } finally {
            span.end();
        }
    }

    private void cleanupInternal(A artefact) {
        if (!multitenantExecution() || !isMultitenantArtefact(artefact)) {
            logger.debug("[{} will cleanup artefact [{}]", this, artefact);
            cleanupImpl(artefact);
            return;
        }

        ArtefactLifecycle lifecycle = artefact.getLifecycle();

        TenantContext tenantContext = BeanProvider.getTenantContext();
        tenantContext.executeForEachTenant(() -> {
            logger.debug("[{} will cleanup artefact [{}] for tenant [{}]", this, artefact, tenantContext.getCurrentTenant());
            artefact.setLifecycle(lifecycle);
            cleanupImpl(artefact);
            return null;
        });
    }

    /**
     * Cleanup impl.
     *
     * @param artefact the artefact
     */
    protected abstract void cleanupImpl(A artefact);

    /**
     * Checks if is accepted.
     *
     * @param file the file
     * @param attrs the attrs
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(Path file, BasicFileAttributes attrs) {
        return file.toString().toLowerCase()
                   .endsWith(getFileExtension().toLowerCase());
    }

}
