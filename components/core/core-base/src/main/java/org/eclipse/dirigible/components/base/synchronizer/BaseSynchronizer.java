/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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
import org.eclipse.dirigible.components.base.tenant.TenantArtefactLedger;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantResult;
import org.eclipse.dirigible.components.open.telemetry.OpenTelemetryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        } catch (StackOverflowError e) {
            // Analysing ONE definition must never end the synchronization pass. A stack overflow in a
            // parser is a property of that definition - a pathological source, a deeply nested document
            // - not of the JVM: the stack has already unwound here and every other definition parses
            // fine. Left to propagate, it escapes the whole run, and since the same file is parsed
            // again on the next pass the job never completes again: no schema, no seeds, no compile, no
            // routing, while HTTP keeps answering 200. Reported as a broken definition instead: the
            // pass completes, the failure is logged and listed with the run's errors, and the
            // definition is re-parsed on every later pass so a corrected source heals itself.
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Stack overflow occurred during synchronization");

            logger.error("Stack overflow while parsing [{}] by [{}]", location, this, e);
            throw new ParseException("Stack overflow while parsing [" + location + "] - the definition is too large or too deeply "
                    + "nested for the parser", 0);
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

        String error = artefact.getError();
        TenantContext tenantContext = BeanProvider.getTenantContext();
        List<TenantResult<TenantOutcome>> results = tenantContext.executeForEachTenant(() -> {
            logger.debug("[{} will complete artefact with lifecycle [{}] in phase [{}]] for tenant [{}]...\\nArtefact:[{}]", this,
                    lifecycle, flow, tenantContext.getCurrentTenant(), artefact);
            artefact.setLifecycle(lifecycle);
            artefact.setError(error);
            boolean completed = completeImpl(wrapper, flow);
            return TenantOutcome.of(completed, lifecycle, artefact, flow);
        });

        recordOutcomes(artefact, results);
        return results.stream()
                      .map(TenantResult::getResult)
                      .allMatch(TenantOutcome::completed);
    }

    /**
     * Records each tenant's outcome in the {@link TenantArtefactLedger}, and keeps the shared row from
     * reading as a success while a tenant failed: every tenant completes the artefact on the same
     * object, so the state the row carries is the one the LAST tenant registered, and a tenant that
     * succeeded after another one failed would otherwise hide the failure (#7776).
     *
     * @param artefact the artefact
     * @param results the outcome per tenant
     */
    private void recordOutcomes(A artefact, List<TenantResult<TenantOutcome>> results) {
        // the ledger lives beside the synchronization processor; a context without one has none
        Optional<TenantArtefactLedger> ledger = BeanProvider.getOptionalBean(TenantArtefactLedger.class);
        List<String> failures = new ArrayList<>();
        boolean fatal = false;
        for (TenantResult<TenantOutcome> result : results) {
            TenantOutcome outcome = result.getResult();
            if (outcome.lifecycle() == null) {
                continue;
            }
            String tenantId = result.getTenant()
                                    .getId();
            ledger.ifPresent(l -> record(l, artefact, tenantId, outcome));
            if (isFailure(outcome.lifecycle())) {
                failures.add(tenantId + ": " + outcome.error());
                fatal |= ArtefactLifecycle.FATAL == outcome.lifecycle();
            }
        }
        if (!failures.isEmpty() && !isFailure(artefact.getLifecycle())) {
            String message = "Failed for tenant(s) " + String.join("; ", failures);
            logger.debug("Artefact [{}] completed for its last tenant but not for every tenant: [{}]", artefact.getKey(), message);
            setStatus(artefact, fatal ? ArtefactLifecycle.FATAL : ArtefactLifecycle.FAILED, message);
        }
    }

    /**
     * Records one outcome. The ledger is bookkeeping about the work, not the work: a write that fails
     * must not fail the artefact it describes.
     */
    private void record(TenantArtefactLedger ledger, A artefact, String tenantId, TenantOutcome outcome) {
        try {
            ledger.record(artefact, tenantId, outcome.lifecycle(), outcome.error());
        } catch (RuntimeException ex) {
            logger.warn("Failed to record the outcome [{}] of artefact [{}] for tenant [{}]", outcome.lifecycle(), artefact.getKey(),
                    tenantId, ex);
        }
    }

    private static boolean isFailure(ArtefactLifecycle lifecycle) {
        return ArtefactLifecycle.FAILED == lifecycle || ArtefactLifecycle.FATAL == lifecycle;
    }

    /**
     * What completing an artefact in one phase came to for one tenant.
     *
     * @param completed whether the synchronizer completed the phase
     * @param lifecycle the lifecycle to record for the tenant, or null when the phase changed nothing
     *        for it - a phase that does not apply to the artefact's lifecycle, or the retry of a
     *        failure that is still failed, must not overwrite the outcome recorded by the phase that
     *        did the work
     * @param error the error to record with a failure
     */
    private record TenantOutcome(boolean completed, ArtefactLifecycle lifecycle, String error) {

        static TenantOutcome of(boolean completed, ArtefactLifecycle before, Artefact artefact, ArtefactPhase flow) {
            ArtefactLifecycle after = artefact.getLifecycle();
            if (after != before) {
                return new TenantOutcome(completed, after, isFailure(after) ? artefact.getError() : null);
            }
            if (!completed && !isFailure(before)) {
                String error = artefact.getError();
                return new TenantOutcome(false, ArtefactLifecycle.FAILED,
                        error == null || error.isBlank() ? "Not completed in phase [" + flow + "]" : error);
            }
            return new TenantOutcome(completed, null, null);
        }
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
        return file.toString()
                   .toLowerCase()
                   .endsWith(getFileExtension().toLowerCase());
    }

}
