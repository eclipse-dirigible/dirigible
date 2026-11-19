/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.listener;

import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a listener's handler again, in process, when it failed only because the database chose it as
 * the victim of a lock conflict (#8745) - so a deadlock no longer spends one of the few deliveries
 * the broker's redelivery policy grants a message before it dead-letters it.
 *
 * <p>
 * <b>Why running it again is safe.</b> A listener dispatch is not a transaction: every repository
 * write a handler makes is a transaction of its own, and a {@code UnitOfWork} block groups several,
 * and both roll back on any failure before the exception leaves them. So when a handler's failure
 * reaches the dispatch, nothing it opened is still open, the write that lost the lock is gone, and
 * whatever it committed before that stays committed - exactly the state a broker redelivery finds.
 * Running the handler here asks of it nothing a redelivery does not already ask: to be safe to run
 * again, which the generated handlers are (a posting checks for the rows it would write, a roll-up
 * recomputes from the store).
 *
 * <p>
 * <b>What counts as a victim</b> is judged from the SQLSTATE anywhere in the failure's cause chain,
 * on the two databases a deployment runs on:
 * <ul>
 * <li>{@code 40001} - H2's deadlock, and the standard serialization failure (PostgreSQL's under
 * {@code SERIALIZABLE} / {@code REPEATABLE READ});</li>
 * <li>{@code 40P01} - PostgreSQL's deadlock;</li>
 * <li>{@code HYT00} - H2's lock-wait timeout;</li>
 * <li>{@code 55P03} - PostgreSQL's lock-wait timeout ({@code lock_timeout}, {@code NOWAIT}).</li>
 * </ul>
 * A lock-wait timeout is included here although {@code UnitOfWork.callRetryingDeadlock} leaves it
 * to its caller: there a user waits for the answer and a repeat would double the wait, while a
 * listener has no one waiting and the broker would run it again after a second anyway. Every other
 * failure - a validation, a constraint, a bug - is handed straight back, as before.
 *
 * <p>
 * Each repeat first waits for the transaction that won to get further: a randomised pause that
 * doubles, about 50-100, 100-200, 200-400 and 400-800 ms, under 1.5 s in all, so two victims of the
 * same deadlock do not come back in step.
 */
final class DeadlockVictimRetry {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeadlockVictimRetry.class);

    /** How many times a lock-conflict victim is run in all, the first run included. */
    static final int ATTEMPTS = 5;

    /** The longest pause before the first repeat; it doubles before each further one. */
    static final long PAUSE_MILLIS = 100;

    private static final Set<String> VICTIM_SQL_STATES = Set.of("40001", "40P01", "HYT00", "55P03");

    /** One run of the handler. */
    interface Attempt {
        void run() throws Exception;
    }

    /** Waits between repeats; replaced by tests that should not sleep. */
    interface Pauser {
        void pause(long millis) throws InterruptedException;
    }

    private final Pauser pauser;

    DeadlockVictimRetry(Pauser pauser) {
        this.pauser = pauser;
    }

    /**
     * Run the attempt, and run it again while it fails as a lock-conflict victim, up to
     * {@value #ATTEMPTS} runs in all.
     *
     * @param attempt the handler's run
     * @param label the listener, for the log
     * @throws Exception the failure of the last run, or the first failure that is not a victim's
     */
    void run(Attempt attempt, String label) throws Exception {
        for (int run = 1;; run++) {
            try {
                attempt.run();
                return;
            } catch (Exception failure) {
                if (run >= ATTEMPTS || !isLockConflictVictim(failure)) {
                    throw failure;
                }
                long pause = pause(run);
                LOGGER.debug("@Listener [{}] lost a lock conflict (run {} of {}) - running it again in {} ms", label, run, ATTEMPTS, pause,
                        failure);
                try {
                    pauser.pause(pause);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread()
                          .interrupt();
                    failure.addSuppressed(interrupted);
                    throw failure;
                }
            }
        }
    }

    /**
     * Whether the database rolled the failed statement's transaction back to resolve a lock conflict: a
     * deadlock, a serialization failure or a lock-wait timeout.
     *
     * @param failure what the handler threw
     * @return {@code true} if some cause carries one of the victim SQLSTATEs
     */
    static boolean isLockConflictVictim(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
            if (cause instanceof SQLException sql && VICTIM_SQL_STATES.contains(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The pause before the repeat that follows the given failed run: a random value in the upper half
     * of {@code 100 ms * 2^(run - 1)}.
     */
    static long pause(int run) {
        long ceiling = PAUSE_MILLIS << (run - 1);
        return ceiling / 2 + ThreadLocalRandom.current()
                                              .nextLong(ceiling / 2 + 1);
    }
}
