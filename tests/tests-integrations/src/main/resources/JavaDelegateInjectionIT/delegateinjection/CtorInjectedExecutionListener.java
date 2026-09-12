/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package delegateinjection;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

/**
 * Constructor injection in a {@code flowable:executionListener class="..."}. Like a delegate, a
 * listener is created by the engine and is therefore never a container-owned bean - and until
 * #7222 the listener path did not reach the container at all, so this class could not even be
 * instantiated (no no-arg constructor).
 */
public class CtorInjectedExecutionListener implements ExecutionListener {

    private final RateProvider rates;

    public CtorInjectedExecutionListener(RateProvider rates) {
        this.rates = rates;
    }

    @Override
    public void notify(DelegateExecution execution) {
        execution.setVariable("executionListenerRate", rates.rate());
    }
}
