/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.readiness;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The platform readiness state, keyed to artefact depletion (#6448): a synchronization pass that
 * ends with no undepleted artefacts makes the instance READY - or READY_DEGRADED when artefacts
 * parked terminally FAILED after exhausting their retries (a broken artefact must not pin the
 * instance at initializing; it is a quality signal, not a traffic gate). The state re-arms to
 * SYNCHRONIZING on every later pass (a publish), while {@link #isBootCompleted()} is a ONE-WAY
 * latch: once the first pass has depleted, the instance accepts traffic forever - a publish never
 * takes a running application offline.
 *
 * <p>
 * A static singleton (like {@code HealthCheckStatus}) so the synchronizer, the endpoint and the
 * shells reach it without wiring.
 */
public final class PlatformReadiness {

    /** The lifecycle: INITIALIZING (boot) &rarr; READY | READY_DEGRADED &harr; SYNCHRONIZING. */
    public enum State {
        /** Booting - the first synchronization pass has not depleted yet. */
        INITIALIZING,
        /** A later pass is processing (a publish); the instance keeps serving. */
        SYNCHRONIZING,
        /** The last pass depleted with every artefact successful. */
        READY,
        /** The last pass depleted, but some artefacts parked FAILED after their retries. */
        READY_DEGRADED
    }

    private static final PlatformReadiness INSTANCE = new PlatformReadiness();

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformReadiness.class);

    private final AtomicReference<State> state = new AtomicReference<>(State.INITIALIZING);
    private final AtomicBoolean bootCompleted = new AtomicBoolean(false);
    private final List<Consumer<State>> listeners = new CopyOnWriteArrayList<>();
    private volatile int failedArtefacts = 0;
    private volatile int pendingArtefacts = 0;
    private volatile Instant since = Instant.now();
    private volatile ArtefactCensus artefacts = ArtefactCensus.NONE;
    private volatile CompiledModulesCensus compiledModules = null;
    private final Map<String, Set<String>> orphanColumns = new ConcurrentHashMap<>();

    private PlatformReadiness() {}

    public static PlatformReadiness getInstance() {
        return INSTANCE;
    }

    /** A synchronization pass started: INITIALIZING stays; a post-boot pass is SYNCHRONIZING. */
    public void passStarted() {
        if (bootCompleted.get()) {
            transition(State.SYNCHRONIZING);
        }
    }

    /**
     * A synchronization pass depleted its artefact queue.
     *
     * @param failed the artefacts that parked terminally FAILED in this pass (0 = clean READY)
     */
    public void passCompleted(int failed) {
        failedArtefacts = Math.max(failed, 0);
        pendingArtefacts = 0;
        bootCompleted.set(true);
        transition(failedArtefacts == 0 ? State.READY : State.READY_DEGRADED);
    }

    /**
     * Reports how many artefacts the running pass has still to deplete, so the readiness endpoint and
     * the IDE indicator can show progress rather than an opaque spinner.
     *
     * @param pending the undepleted artefact count
     */
    public void passProgress(int pending) {
        pendingArtefacts = Math.max(pending, 0);
    }

    /** The artefacts the running pass has still to deplete (0 when no pass is running). */
    public int getPendingArtefacts() {
        return pendingArtefacts;
    }

    public State getState() {
        return state.get();
    }

    /** The artefacts terminally FAILED in the last depleted pass. */
    public int getFailedArtefacts() {
        return failedArtefacts;
    }

    /** When the current state was entered. */
    public Instant getSince() {
        return since;
    }

    /**
     * Records the lifecycle census of every registered artefact, taken at the end of a pass (#7533).
     *
     * @param census the census
     */
    public void recordArtefacts(ArtefactCensus census) {
        artefacts = census;
    }

    /** The artefact census of the last pass that took one ({@link ArtefactCensus#NONE} before). */
    public ArtefactCensus getArtefacts() {
        return artefacts;
    }

    /**
     * Records the columns a table keeps in the database although its published definition no longer
     * declares them (#7635) - kept with their data instead of dropped. Each reconciliation of the table
     * replaces its previous record, so a column that is declared again, or dropped, stops counting.
     *
     * @param table the table, qualified by its catalog and schema - one tenant's copy is not another's
     * @param columns the undeclared columns, empty when the table has none
     */
    public void recordOrphanColumns(String table, Set<String> columns) {
        if (columns.isEmpty()) {
            orphanColumns.remove(table);
        } else {
            orphanColumns.put(table, Set.copyOf(columns));
        }
    }

    /**
     * The undeclared columns kept across every table, as the last reconciliation of each left them.
     *
     * @return the orphan column count
     */
    public int getOrphanColumns() {
        return orphanColumns.values()
                            .stream()
                            .mapToInt(Set::size)
                            .sum();
    }

    /**
     * Records what the last AOT compiled-module discovery found (#7533). It runs on the application
     * ready event, independently of the synchronization passes, so the listeners are notified with the
     * unchanged state - a consumer waiting for a {@link #isCleanBoot() clean boot} re-evaluates.
     *
     * @param census the census
     */
    public void recordCompiledModules(CompiledModulesCensus census) {
        compiledModules = census;
        notifyListeners(state.get());
    }

    /**
     * The census of the last AOT compiled-module discovery.
     *
     * @return the census, empty until the discovery has run
     */
    public Optional<CompiledModulesCensus> getCompiledModules() {
        return Optional.ofNullable(compiledModules);
    }

    /**
     * Whether the boot is complete AND clean: no registered artefact is failed and the AOT discovery
     * has run and registered every class its markers list (#7533). The stricter, opt-in readiness
     * condition - {@link #isBootCompleted()} alone lets an instance serve with a failed seed.
     *
     * @return true when the instance booted with nothing missing
     */
    public boolean isCleanBoot() {
        CompiledModulesCensus modules = compiledModules;
        return isBootCompleted() && artefacts.failed() == 0 && modules != null && modules.complete();
    }

    /** The ONE-WAY boot latch: true once the first pass has depleted - traffic is accepted. */
    public boolean isBootCompleted() {
        return bootCompleted.get();
    }

    /**
     * Registers a listener notified after every state change and after every compiled-module census.
     * Listeners run on the synchronizer's thread, so a slow or throwing one must not disturb the pass -
     * a failure is logged and swallowed.
     *
     * @param listener the listener
     */
    public void addStateListener(Consumer<State> listener) {
        listeners.add(listener);
    }

    /** Test seam: reset to the boot state (the singleton outlives a test's application context). */
    public void reset() {
        state.set(State.INITIALIZING);
        bootCompleted.set(false);
        failedArtefacts = 0;
        pendingArtefacts = 0;
        since = Instant.now();
        artefacts = ArtefactCensus.NONE;
        compiledModules = null;
        orphanColumns.clear();
        listeners.clear();
    }

    private void transition(State next) {
        if (state.getAndSet(next) != next) {
            since = Instant.now();
            notifyListeners(next);
        }
    }

    private void notifyListeners(State next) {
        for (Consumer<State> listener : listeners) {
            try {
                listener.accept(next);
            } catch (RuntimeException ex) {
                LOGGER.error("Platform readiness listener [{}] failed for state [{}]", listener, next, ex);
            }
        }
    }
}
