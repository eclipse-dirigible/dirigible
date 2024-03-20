/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible
 * contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs;

import java.util.Optional;

/**
 * The Class SystemJob.
 */
public abstract class SystemJob extends DirigibleJob {

    /**
     * Gets the trigger group.
     *
     * @return the trigger group
     */
    @Override
    protected final Optional<String> getTriggerGroup() {
        return Optional.of("system");
    }

    /**
     * Gets the job group.
     *
     * @return the job group
     */
    @Override
    protected final Optional<String> getJobGroup() {
        return Optional.of("system");
    }
}
