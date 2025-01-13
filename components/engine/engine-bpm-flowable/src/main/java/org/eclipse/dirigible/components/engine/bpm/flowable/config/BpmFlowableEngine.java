/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import org.eclipse.dirigible.components.base.artefact.Engine;
import org.springframework.stereotype.Component;

/**
 * The Class BPMFlowableEngine.
 */
@Component
public class BpmFlowableEngine implements Engine {

    /**
     * Gets the name.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return "Process (Flowable)";
    }

    /**
     * Gets the provider.
     *
     * @return the provider
     */
    @Override
    public String getProvider() {
        return "Eclipse Dirigible";
    }

}
