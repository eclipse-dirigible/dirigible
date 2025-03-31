/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.config;

import org.eclipse.dirigible.components.base.callable.CallableNoResultAndException;
import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public class TransactionExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionExecutor.class);

    private static final ThreadLocal<Boolean> executedByTheExecutor = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static boolean isExecutedInTransaction() {
        return executedByTheExecutor.get();
    }

    /**
     * Execute code in transaction for a data source
     *
     * @param dataSource data source
     * @param callable code to be executed
     * @param <R> result
     * @param <E>
     * @return
     * @throws TransactionExecutionException if fail to execute the code
     */
    public static <R, E extends Throwable> R executeInTransaction(DirigibleDataSource dataSource, CallableResultAndException<R, E> callable)
            throws TransactionExecutionException {

        PlatformTransactionManager transactionManager = getTransactionManager(dataSource);
        TransactionTemplate transactionTemplate = createTemplate(transactionManager);

        return transactionTemplate.execute(status -> {
            try {
                executedByTheExecutor.set(true);
                return callable.call();
            } catch (Throwable ex) {
                throw new TransactionExecutionException("Failed to execute code for data source [" + dataSource + "] using " + callable,
                        ex);
            } finally {
                executedByTheExecutor.set(false);
            }
        });
    }

    private static PlatformTransactionManager getTransactionManager(DirigibleDataSource dataSource) {
        return dataSource.getTransactionManager()
                         .orElseThrow(() -> new TransactionExecutionException("Missing transaction manager for data source " + dataSource));
    }

    private static TransactionTemplate createTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        return transactionTemplate;
    }

    /**
     * Execute code in transaction for a data source
     *
     * @param dataSource data source
     * @param callable code to be executed
     * @param <E>
     * @throws TransactionExecutionException if fail to execute the code
     */
    public static <E extends Throwable> void executeInTransaction(DirigibleDataSource dataSource, CallableNoResultAndException<E> callable)
            throws TransactionExecutionException {
        PlatformTransactionManager transactionManager = getTransactionManager(dataSource);
        TransactionTemplate transactionTemplate = createTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            try {
                executedByTheExecutor.set(true);
                callable.call();
            } catch (Throwable ex) {
                throw new TransactionExecutionException("Failed to execute code for data source [" + dataSource + "] using " + callable,
                        ex);
            } finally {
                executedByTheExecutor.set(false);
            }
        });
    }

}
