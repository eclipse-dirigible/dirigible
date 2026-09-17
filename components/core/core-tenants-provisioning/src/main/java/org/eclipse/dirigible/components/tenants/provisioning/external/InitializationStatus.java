/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

/**
 * How far the initialization of a tenant has got - what a caller polls after activating it.
 */
public enum InitializationStatus {

    /** The tenant is registered but its activation was never requested. */
    NOT_STARTED,

    /** The tenant is active and its artefacts are still being materialized. */
    IN_PROGRESS,

    /** Every artefact of the tenant has been materialized. */
    COMPLETED,

    /** Some artefacts could not be materialized; the detail says which and why. */
    FAILED
}
