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

/** Constructor-injected, and cached by Flowable on the parsed activity until the next rebuild. */
public class VersionDelegate implements JavaDelegate {

    private final VersionProvider versions;

    public VersionDelegate(VersionProvider versions) {
        this.versions = versions;
    }

    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariable("version", versions.version());
    }
}
