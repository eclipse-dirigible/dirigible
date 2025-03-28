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

import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public class TransactionExecutor {

    /**
     * Execute code in transaction for a data source
     *
     * @param dataSource data source
     * @param callable code to be executed
     * @param <R> result
     * @param <E>
     * @return
     * @throws TransactionExecutionException
     */
    public static <R, E extends Throwable> R executeInTransaction(DirigibleDataSource dataSource, CallableResultAndException<R, E> callable)
            throws TransactionExecutionException {
        PlatformTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setIsolationLevel(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        return transactionTemplate.execute(status -> {
            try {
                return callable.call();
            } catch (Throwable ex) {
                throw new TransactionExecutionException("Failed to execute code for data source [" + dataSource + "] using " + callable,
                        ex);
            }
        });
    }
    // public static <R, E extends Throwable> R executeInTransaction(DirigibleDataSource dataSource,
    // CallableResultAndException<R, E> callable)
    // throws Throwable {
    // PlatformTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
    //
    // DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
    // transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    //
    // TransactionStatus status = transactionManager.getTransaction(transactionDefinition);
    // try {
    // R result = callable.call();
    // transactionManager.commit(status);
    // return result;
    // } catch (Throwable ex) {
    // transactionManager.rollback(status);
    // throw ex;
    // }
    // }

}
