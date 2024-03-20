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
package org.eclipse.dirigible.components.base;

import org.springframework.core.Ordered;

/**
 * Lower value means higher precedence - lower values will be executed before the higher values.
 */
public interface ApplicationListenersOrder {

    /** The app lyfecycle logging listener. */
    int APP_LYFECYCLE_LOGGING_LISTENER = Ordered.LOWEST_PRECEDENCE;


    /**
     * The Interface ApplicationReadyEventListeners.
     */
    interface ApplicationReadyEventListeners {

        /** The system roles initializer. */
        int SYSTEM_ROLES_INITIALIZER = 10;

        /** The default tenant initializer. */
        int DEFAULT_TENANT_INITIALIZER = 20;

        /** The admin user initializer. */
        int ADMIN_USER_INITIALIZER = 30;

        /** The synchronization intializer. */
        int SYNCHRONIZATION_INTIALIZER = 40;

        /** The jobs initializer. */
        int JOBS_INITIALIZER = 50;


    }


    /**
     * The Interface ApplicationStoppedEventListeners.
     */
    interface ApplicationStoppedEventListeners {

        /** The active mq cleanup. */
        int ACTIVE_MQ_CLEANUP = APP_LYFECYCLE_LOGGING_LISTENER - 10;

    }

}
