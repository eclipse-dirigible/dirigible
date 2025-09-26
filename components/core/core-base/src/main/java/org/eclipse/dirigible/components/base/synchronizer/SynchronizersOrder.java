/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.synchronizer;

/**
 * The Interface SynchronizersOrder.
 */
public interface SynchronizersOrder {

    /** The extensionpoint. */
    int EXTENSIONPOINT = 10;

    /** The extension. */
    int EXTENSION = 20;

    /** The role. */
    int ROLE = 30;

    /** The access. */
    int ACCESS = 40;

    /** The job. */
    int JOB = 50;

    /** The listener. */
    int LISTENER = 60;

    /** The expose. */
    int EXPOSE = 70;

    /** The openapi. */
    int OPENAPI = 110;

    /** The websocket. */
    int WEBSOCKET = 120;

    /** The datasource. */
    int DATASOURCE = 200;

    /** The schema. */
    int SCHEMA = 210;

    /** The table. */
    int TABLE = 220;

    /** The view. */
    int VIEW = 230;

    /** The entity. */
    int ENTITY = 240;

    /** The bpmn. */
    int BPMN = 300;

    /** The odata. */
    int ODATA = 310;

    /** The csvim. */
    int CSVIM = 400;

    /** The confluence. */
    int CONFLUENCE = 410;

    /** The markdown. */
    int MARKDOWN = 420;

    /** The proxy. */
    int PROXY = 430;

}
