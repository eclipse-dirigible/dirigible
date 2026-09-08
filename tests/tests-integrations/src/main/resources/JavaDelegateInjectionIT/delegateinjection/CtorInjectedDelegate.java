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
import org.flowable.engine.delegate.JavaDelegate;

/**
 * Constructor injection on the {@code flowable:class} path. Deliberately NOT a @Component: a
 * delegate is created by the engine, so it is never a container-owned bean - the collaborator is
 * wired into the instance the engine builds.
 */
public class CtorInjectedDelegate implements JavaDelegate {

    private final RateProvider rates;

    public CtorInjectedDelegate(RateProvider rates) {
        this.rates = rates;
    }

    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariable("ctorRate", rates.rate());
    }
}
