/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.telemetry;

import org.quartz.listeners.JobListenerSupport;

/**
 * The listener interface for receiving openTelemetry events. The class that is interested in
 * processing a openTelemetry event implements this interface, and the object created with that
 * class is registered with a component using the component's addOpenTelemetryListener method. When
 * the openTelemetry event occurs, that object's appropriate method is invoked.
 *
 * @see OpenTelemetryEvent
 */
abstract class OpenTelemetryListener extends JobListenerSupport {

    /**
     * Gets the name.
     *
     * @return the name
     */
    @Override
    public final String getName() {
        return this.getClass()
                   .getSimpleName();
    }

}
