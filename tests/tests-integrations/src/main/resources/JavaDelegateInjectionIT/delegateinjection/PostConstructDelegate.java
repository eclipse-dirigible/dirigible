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

import jakarta.annotation.PostConstruct;

/**
 * Declares only a lifecycle callback. It counts as an injection point on purpose: a @PostConstruct
 * that silently never ran would be worse than not supporting it.
 */
public class PostConstructDelegate implements JavaDelegate {

    private String ready = "no";

    @PostConstruct
    void init() {
        ready = "yes";
    }

    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariable("postConstructRan", ready);
    }
}
