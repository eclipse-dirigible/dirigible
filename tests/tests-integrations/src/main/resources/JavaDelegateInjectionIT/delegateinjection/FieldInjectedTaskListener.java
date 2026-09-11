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
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;

/**
 * Field injection in a {@code flowable:taskListener class="..."} - the failure mode #7222 is about:
 * the class instantiates either way, and before the fix the injected field simply read
 * {@code null}.
 */
public class FieldInjectedTaskListener implements TaskListener {

    @Inject
    private RateProvider rates;

    @Override
    public void notify(DelegateTask delegateTask) {
        delegateTask.setVariable("taskListenerRate", rates.rate());
    }
}
