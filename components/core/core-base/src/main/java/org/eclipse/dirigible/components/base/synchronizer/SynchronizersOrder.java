/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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

    /** The scope. */
    int SCOPE = 35;

    /** The access. */
    int ACCESS = 40;

    /**
     * The number-series declaration ({@code .numbers}). Deliberately before every artefact type that
     * could allocate a document number during synchronization (client Java components, BPMN, CSVIM), so
     * a declared series is provisioned before the first allocation can ask for it.
     */
    int NUMBER_SERIES = 45;

    /** The job. */
    int JOB = 50;

    /** The listener. */
    int LISTENER = 60;

    /** The expose. */
    int EXPOSE = 70;

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

    /** The component. */
    int COMPONENT = 250;

    /** The bpmn. */
    int BPMN = 300;

    /**
     * The odata. Reserved for the externalized OData engine (moved to a separate project); its
     * synchronizer still references this constant via {@code @Order(SynchronizersOrder.ODATA)}, so the
     * value is kept here to preserve the global synchronizer ordering.
     */
    int ODATA = 310;

    /** The csvim. */
    int CSVIM = 400;

    /** The confluence. */
    int CONFLUENCE = 410;

    /** The markdown. */
    int MARKDOWN = 420;

    /** The proxy. */
    int PROXY = 430;

    /** The native app. */
    int NATIVE_APP = 440;

    /** The openapi. */
    int OPENAPI = 510;

    /** The CMS seed (files under a project's {@code doc/} folder, seeded into the CMS). */
    int CMS_SEED = 520;

}
