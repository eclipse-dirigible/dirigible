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

import org.eclipse.dirigible.sdk.component.Inject;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/** Field injection on the {@code ${JavaTask}} + {@code handler} path. */
public class FieldInjectedHandler implements JavaDelegate {

    @Inject
    private RateProvider rates;

    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariable("fieldRate", rates.rate());
    }
}
