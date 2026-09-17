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

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.dirigible.sdk.component.Inject;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/** Collection injection: every @Component contributing the Rule extension point. */
public class CollectionDelegate implements JavaDelegate {

    @Inject
    private List<Rule> rules;

    @Override
    public void execute(DelegateExecution execution) {
        // Sorted, not joined as injected: the container injects in registration order, which is the
        // order the loader hands the classes over - not the order they are declared in.
        execution.setVariable("rules", rules.stream()
                                            .map(Rule::name)
                                            .sorted()
                                            .collect(Collectors.joining(",")));
    }
}
